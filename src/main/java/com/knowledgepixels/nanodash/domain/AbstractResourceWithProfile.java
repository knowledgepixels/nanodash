package com.knowledgepixels.nanodash.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashThreadPool;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Abstract class representing a resource with a profile in the Nanodash application.
 * This class provides common functionality for resources that have associated profiles, such as spaces and users.
 */
public abstract class AbstractResourceWithProfile implements Serializable, ResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceWithProfile.class);

    private static final Map<Class<?>, Map<String, AbstractResourceWithProfile>> instances = new ConcurrentHashMap<>();

    // Backoff after a failed data update, so a persistently failing resource
    // doesn't respawn an update task on every page poll (~1/s per open page).
    private static final long FAILED_UPDATE_BACKOFF_MS = 10 * 1000;

    private final String id;
    private Space space;
    private ResourceWithProfile data = new ResourceWithProfile();
    private volatile boolean dataInitialized = false;
    private volatile boolean dataNeedsUpdate = true;
    private volatile Long runUpdateAfter = null;

    /**
     * Inner class to hold the data associated with a resource, including its view displays.
     */
    protected static class ResourceWithProfile implements Serializable {
        List<ViewDisplay> viewDisplays = new ArrayList<>();
    }

    /**
     * Checks if a resource with the given unique identifier exists in the system.
     *
     * @param id the unique identifier of the resource
     * @return true if a resource with the given id exists, false otherwise
     */
    public static boolean isResourceWithProfile(String id) {
        return get(id) != null;
    }

    /**
     * Retrieves an instance of AbstractResourceWithProfile by its unique identifier.
     *
     * @param id the unique identifier of the resource
     * @return the AbstractResourceWithProfile instance associated with the given id, or null if no such instance exists
     */
    public static AbstractResourceWithProfile get(String id) {
        for (Map<String, AbstractResourceWithProfile> map : instances.values()) {
            if (map.containsKey(id)) {
                return map.get(id);
            }
        }
        return null;
    }

    /**
     * Constructor for AbstractResourceWithProfile.
     *
     * @param id the unique identifier for this resource
     */
    protected AbstractResourceWithProfile(String id) {
        this.id = id;
        instances.computeIfAbsent(getClass(), k -> new ConcurrentHashMap<>()).put(id, this);
    }

    /**
     * Removes an instance of AbstractResourceWithProfile from the instances map based on its type and unique identifier.
     *
     * @param type the class type of the resource to remove
     * @param id   the unique identifier of the resource to remove
     */
    protected static void removeInstance(Class<?> type, String id) {
        Map<String, AbstractResourceWithProfile> map = instances.get(type);
        if (map != null) {
            map.remove(id);
        }
    }

    /**
     * Retrieves all instances of AbstractResourceWithProfile of a specific type.
     *
     * @param type the class type of the resources to retrieve
     * @return a map of resource IDs to AbstractResourceWithProfile instances of the specified type, or an empty map if no instances exist for that type
     */
    protected static Map<String, AbstractResourceWithProfile> getInstances(Class<?> type) {
        return instances.getOrDefault(type, Collections.emptyMap());
    }

    /**
     * Initializes the space for this resource.
     *
     * @param space the space to associate with this resource
     */
    protected void initSpace(Space space) {
        this.space = space;
        logger.info("Initialized space {} for resource {}", space.getId(), id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized Future<?> triggerDataUpdate() {
        if (dataNeedsUpdate) {
            // Not due yet (delayed forceRefresh or backoff after a failure): don't
            // occupy a pool thread with sleeping; a later access re-triggers.
            Long after = runUpdateAfter;
            if (after != null && System.currentTimeMillis() < after) {
                return null;
            }
            runUpdateAfter = null;
            logger.info("Data needs update for resource {}, starting update thread", id);
            dataNeedsUpdate = false;
            return NanodashThreadPool.submit(() -> {
                try {
                    ResourceWithProfile newData = new ResourceWithProfile();

                    // The query returns both standalone view displays (bound ?display) and
                    // preset-supplied views (issue #302: unbound ?display, the assignment's
                    // preset expanded into its views server-side), already ordered by date
                    // (latest first) so the per-view-kind latest-wins / deactivation
                    // aggregation in getViewDisplays() resolves overrides between presets and
                    // standalone displays correctly, in either direction.
                    seedFromCacheIfPossible();
                    newData.viewDisplays.addAll(buildViewDisplays(viewDisplaysQueryRef()));
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update data for resource {}", id, ex);
                    runUpdateAfter = System.currentTimeMillis() + FAILED_UPDATE_BACKOFF_MS;
                    dataNeedsUpdate = true;
                }
            });
        }
        return null;
    }

    /**
     * The query for this resource's view displays. For a space, scoped to its
     * representative ref (root nanopub) so a multi-ref identifier doesn't merge displays
     * across rival definitions; other resource kinds (and spaces with no known ref root)
     * stay IRI-keyed.
     */
    private QueryRef viewDisplaysQueryRef() {
        String vdRefRoot = getViewDisplayRefRoot();
        return (vdRefRoot != null && !vdRefRoot.isEmpty())
                ? viewDisplaysRefQueryRef(vdRefRoot)
                : new QueryRef(QueryApiAccess.GET_VIEW_DISPLAYS, "resource", id);
    }

    /**
     * Initializes the resource data from whatever view-displays response the cache still
     * holds — typically the persisted snapshot right after a restart (issue #570) — so
     * pages gated on {@link #isDataInitialized()} render their content on the first
     * request instead of a loading spinner. Purely cache-fed, so it is quick exactly when
     * it can succeed and does nothing on a genuinely cold cache, where the asynchronous
     * update (whose fetch also brings seeded data current) remains the only path. The
     * seeded data doubles as the outage fallback should that fetch fail.
     */
    private synchronized void seedFromCacheIfPossible() {
        if (dataInitialized) return;
        // A pending delayed refresh (forceRefresh, e.g. just after publishing) must not be
        // masked by re-seeding the old state as initialized: there the page is meant to
        // wait for the fresh data. Seeding is for the never-initialized case, where
        // runUpdateAfter has not been set.
        if (runUpdateAfter != null) return;
        QueryRef vdQuery = viewDisplaysQueryRef();
        ApiResponse cachedResponse = ApiCache.retrieveStaleResponse(vdQuery);
        if (cachedResponse == null) return;
        ResourceWithProfile seeded = new ResourceWithProfile();
        seeded.viewDisplays.addAll(buildViewDisplays(cachedResponse, vdQuery));
        data = seeded;
        dataInitialized = true;
    }

    /**
     * Forces a refresh of the resource data after a specified delay.
     *
     * @param waitMillis the delay in milliseconds before the data refresh is triggered
     */
    public void forceRefresh(long waitMillis) {
        logger.info("Forcing refresh of resource {} after {} ms", id, waitMillis);
        dataNeedsUpdate = true;
        dataInitialized = false;
        runUpdateAfter = System.currentTimeMillis() + waitMillis;
    }

    /**
     * Static method to force a refresh of the resource data for all instances of AbstractResourceWithProfile.
     */
    public static void refresh() {
        instances.values().forEach(map -> map.values().forEach(AbstractResourceWithProfile::setDataNeedsUpdate));
    }

    @Override
    public Long getRunUpdateAfter() {
        return runUpdateAfter;
    }

    @Override
    public Space getSpace() {
        return space;
    }

    @Override
    public abstract String getNanopubId();

    @Override
    public abstract Nanopub getNanopub();

    public abstract String getNamespace();

    @Override
    public void setDataNeedsUpdate() {
        dataNeedsUpdate = true;
    }

    @Override
    public boolean isDataInitialized() {
        // Seeding synchronously (cache-only, no network) is what lets the FIRST render of
        // a page reach this resource's content: the update below runs asynchronously, so
        // without the seed that render would fall back to a loading spinner even though
        // everything it needs is in the (restored) cache.
        if (!dataInitialized) {
            seedFromCacheIfPossible();
        }
        triggerDataUpdate();
        return dataInitialized;
    }

    @Override
    public List<ViewDisplay> getViewDisplays() {
        logger.info("Getting view displays for resource {}", id);
        return data.viewDisplays;
    }

    @Override
    public List<ViewDisplay> getTopLevelViewDisplays() {
        // Pass the resource's own type(s) so that views targeting that type (e.g. a
        // messages view for gen:MaintainedResource) are shown at the top level, while
        // views targeting part types (e.g. gen:hasViewTargetClass owl:Class) are not.
        return getViewDisplays(true, getId(), getOwnClasses());
    }

    /**
     * The resource's own type IRI(s), used to match top-level views by
     * {@code gen:appliesToInstancesOf}. Empty by default; overridden per resource type.
     *
     * @return the resource's own classes (never null)
     */
    protected Set<IRI> getOwnClasses() {
        return Collections.emptySet();
    }

    @Override
    public List<ViewDisplay> getPartLevelViewDisplays(String resourceId, Set<IRI> classes) {
        return getViewDisplays(false, resourceId, classes);
    }

    private List<ViewDisplay> getViewDisplays(boolean toplevel, String resourceId, Set<IRI> classes) {
        triggerDataUpdate();
        return filterViewDisplays(getViewDisplays(), toplevel, resourceId, classes);
    }

    /**
     * Top-level view displays scoped to a specific space ref (root nanopub), fetched on demand
     * rather than from the IRI-keyed singleton data — used to render the Content tab of a
     * {@code ?root=}-pinned space page so it shows only that ref's displays. Falls back to the
     * default (singleton) displays when {@code refRoot} is null/empty. See docs/space-ref-identity.md.
     *
     * @param refRoot the ref's root nanopub, or null/empty for the default
     * @return the ref-scoped top-level view displays
     */
    public List<ViewDisplay> getTopLevelViewDisplays(String refRoot) {
        if (refRoot == null || refRoot.isEmpty()) return getTopLevelViewDisplays();
        return filterViewDisplays(buildViewDisplays(viewDisplaysRefQueryRef(refRoot)), true, getId(), getOwnClasses());
    }

    private List<ViewDisplay> filterViewDisplays(List<ViewDisplay> source, boolean toplevel, String resourceId, Set<IRI> classes) {
        List<ViewDisplay> viewDisplays = new ArrayList<>();
        Set<IRI> viewKinds = new HashSet<>();

        // Results are sorted by date (most recent first); only the most recent per view-kind is considered
        for (ViewDisplay vd : source) {
            IRI kind = vd.getViewKindIri();
            if (kind != null) {
                if (viewKinds.contains(kind)) {
                    continue;
                }
                viewKinds.add(kind);
            }

            if (vd.hasType(KPXL_TERMS.DEACTIVATED_VIEW_DISPLAY)) {
                continue;
            }

            if (!toplevel && vd.hasType(KPXL_TERMS.TOP_LEVEL_VIEW_DISPLAY)) {
                // Deprecated
                // do nothing
            } else if (vd.appliesTo(resourceId, classes)) {
                viewDisplays.add(vd);
            } else if (toplevel && vd.hasType(KPXL_TERMS.TOP_LEVEL_VIEW_DISPLAY)) {
                // Deprecated
                viewDisplays.add(vd);
            }
        }

        Collections.sort(viewDisplays);
        return viewDisplays;
    }

    /**
     * Fetches the applicable view displays synchronously from the API, bypassing the async
     * singleton data. Used by the download pages, which need the displays within the current
     * request; the resolution mirrors the page rendering (ref-scoped query for spaces,
     * preset-supplied views, per-view-kind latest-wins).
     *
     * @param partId      the part IRI, or null for the resource's top-level displays
     * @param partClasses the part's classes (ignored for top-level, where the resource's
     *                    own classes are used)
     * @return the applicable view displays, sorted by structural position
     */
    public List<ViewDisplay> fetchViewDisplaysSync(String partId, Set<IRI> partClasses) {
        String vdRefRoot = getViewDisplayRefRoot();
        QueryRef vdQuery = (vdRefRoot != null && !vdRefRoot.isEmpty())
                ? viewDisplaysRefQueryRef(vdRefRoot)
                : new QueryRef(QueryApiAccess.GET_VIEW_DISPLAYS, "resource", id);
        boolean toplevel = (partId == null);
        return filterViewDisplays(buildViewDisplays(vdQuery), toplevel,
                toplevel ? getId() : partId, toplevel ? getOwnClasses() : partClasses);
    }

    /**
     * Builds {@link ViewDisplay} objects from a get-view-displays(-ref) query result (standalone
     * displays with a bound {@code ?display}, and preset-supplied views with an unbound one).
     */
    private List<ViewDisplay> buildViewDisplays(QueryRef ref) {
        // Null on a cold cache or a failed (flaky federated) fetch — yields nothing for now;
        // the cache refreshes asynchronously and the page's auto-refresh repopulates it.
        return buildViewDisplays(ApiCache.retrieveResponseSync(ref, true), ref);
    }

    private List<ViewDisplay> buildViewDisplays(ApiResponse response, QueryRef ref) {
        List<ViewDisplay> list = new ArrayList<>();
        if (response == null) return list;
        // The unresolved query variant returns ?view as the referenced version, leaving
        // latest-version resolution to us (View.get with resolveLatest=true, memoized;
        // it also covers space-governed pins); the older resolved heads return ?view
        // already latest-resolved server-side, so it is passed through as-is.
        boolean viewsPreResolved = !QueryApiAccess.GET_VIEW_DISPLAYS_UNRESOLVED.equals(ref.getQueryId());
        for (ApiResponseEntry r : response.getData()) {
            try {
                String display = r.get("display");
                if (display != null && !display.isEmpty()) {
                    list.add(ViewDisplay.get(display, viewsPreResolved ? r.get("view") : null));
                } else {
                    String view = r.get("view");
                    if (view == null || view.isEmpty()) continue;
                    boolean topLevel = KPXL_TERMS.TOP_LEVEL_VIEW_DISPLAY.stringValue().equals(r.get("displayType"));
                    boolean deactivated = KPXL_TERMS.DEACTIVATED_PRESET_ASSIGNMENT.stringValue().equals(r.get("displayMode"));
                    ViewDisplay vd = ViewDisplay.forPresetView(id, view, topLevel, deactivated, !viewsPreResolved);
                    if (vd != null) list.add(vd);
                }
            } catch (IllegalArgumentException ex) {
                logger.error("Couldn't generate view display object", ex);
            }
        }
        return list;
    }

    private QueryRef viewDisplaysRefQueryRef(String refRoot) {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("resource", id);
        params.put("root_np", refRoot);
        return new QueryRef(QueryApiAccess.GET_VIEW_DISPLAYS_UNRESOLVED, params);
    }

    /**
     * The root nanopub of the space ref the resource's view displays should be scoped to, or null
     * to use the IRI-keyed query (merged across refs). Null by default; {@link Space} overrides it
     * to its representative ref so a multi-ref identifier doesn't merge Content-tab displays.
     *
     * @return the ref's root nanopub, or null
     */
    protected String getViewDisplayRefRoot() {
        return null;
    }

    @Override
    public abstract String getLabel();

    @Override
    public String toString() {
        return id;
    }

    /**
     * Gets the chain of superspaces from the current space up to the root space.
     *
     * @return the list of superspaces from the given space to the root space
     */
    @Override
    public List<AbstractResourceWithProfile> getAllSuperSpacesUntilRoot() {
        List<AbstractResourceWithProfile> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        collectAncestors(space, chain, visited);
        Collections.reverse(chain);
        return chain;
    }

    private void collectAncestors(Space current, List<AbstractResourceWithProfile> chain, Set<String> visited) {
        if (current == null) {
            return;
        }
        List<Space> parents = SpaceRepository.get().findSuperspaces(current);
        if (parents == null || parents.isEmpty()) {
            return;
        }
        Space parent = parents.getFirst();
        if (parent == null) {
            return;
        }
        String pid = parent.getId();
        if (pid == null || !visited.add(pid)) {
            return;
        }
        chain.add(parent);
        collectAncestors(parent, chain, visited);
    }

    /**
     * Checks if any view display of this resource applies to the given element ID and set of classes by triggering a data update and checking each view display for applicability.
     *
     * @param elementId the ID of the element to check for applicability
     * @param classes   the set of classes to check for applicability
     * @return true if any view display of this resource applies to the given element ID and set of classes, false otherwise
     */
    public boolean appliesTo(String elementId, Set<IRI> classes) {
        triggerDataUpdate();
        for (ViewDisplay v : getViewDisplays()) {
            if (v.appliesTo(elementId, classes)) {
                return true;
            }
        }
        return false;
    }

}
