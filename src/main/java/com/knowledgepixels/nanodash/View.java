package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a Resource View.
 */
public class View implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(View.class);
    private static final Set<IRI> supportedViewTypes = Set.of(
            KPXL_TERMS.TABULAR_VIEW,
            KPXL_TERMS.LIST_VIEW,
            KPXL_TERMS.PLAIN_PARAGRAPH_VIEW,
            KPXL_TERMS.NANOPUB_SET_VIEW,
            KPXL_TERMS.ITEM_LIST_VIEW,
            KPXL_TERMS.SVG_VIEW,
            KPXL_TERMS.HEADER_VIEW
    );

    static Map<IRI, Integer> columnWidths = new HashMap<>();

    static {
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_1_OF_12, 1);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_2_OF_12, 2);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_3_OF_12, 3);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_4_OF_12, 4);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_5_OF_12, 5);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_6_OF_12, 6);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_7_OF_12, 7);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_8_OF_12, 8);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_9_OF_12, 9);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_10_OF_12, 10);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_11_OF_12, 11);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_12_OF_12, 12);
    }

    private static final Cache<String, View> views = CacheBuilder.newBuilder()
        .maximumSize(5_000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    /**
     * Memo of latest-version resolutions: view id (as passed to {@link #get(String)})
     * to the resolution time and the View it resolved to. Entries are served as
     * long as they live; one older than {@link #REFRESH_RESOLUTION_AFTER_MS} is
     * served stale while a background re-resolution runs (stale-while-revalidate,
     * like {@link ApiCache}), so a superseding view nanopub is picked up on a
     * later render without {@link #get(String)} ever blocking on the network
     * once an entry exists.
     */
    private static final Cache<String, Pair<Long, View>> latestResolvedViews = CacheBuilder.newBuilder()
        .maximumSize(5_000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    /**
     * Age after which a memoized latest-version resolution is re-resolved in the
     * background; mirrors the freshness window of
     * {@link QueryApiAccess#getLatestVersionId(String)}.
     */
    private static final long REFRESH_RESOLUTION_AFTER_MS = 1000 * 60;

    /**
     * Ids whose latest-version resolution is currently being refreshed in the
     * background, so concurrent renders don't pile up duplicate refreshes.
     */
    private static final Set<String> refreshingViews = ConcurrentHashMap.newKeySet();

    /**
     * Indicates whether {@link #get(String)} would currently return without
     * network access, i.e. a latest-version resolution is memoized for this id
     * (possibly stale, in which case get() serves it while refreshing in the
     * background). Used to decide between constructing a view-based panel
     * directly and deferring it to a lazy-loading AJAX request.
     *
     * @param id the ID of the View
     * @return true if {@link #get(String)} currently returns without blocking
     */
    public static boolean isCached(String id) {
        return latestResolvedViews.getIfPresent(id) != null;
    }

    /**
     * The current latest-version resolution memo, for persisting across restarts (issue
     * #570; see {@link ApiCachePersistence}). Restoring it is what lets pages build their
     * view panels synchronously right after a restart — {@link #isCached(String)} decides
     * that — and the stale-while-revalidate handling re-resolves the restored entries in
     * the background as they are used.
     *
     * @return a copy of the memoized resolutions
     */
    static Map<String, Pair<Long, View>> exportResolvedViews() {
        return new HashMap<>(latestResolvedViews.asMap());
    }

    /**
     * The current exact-version view cache, for persisting across restarts (issue #570; see
     * {@link ApiCachePersistence}). These are the constructed View objects the view displays
     * hand out; rebuilding one involves governed-version lookups and query construction, so
     * restoring them is what makes a page's views renderable right after a restart. Keyed by
     * the exact (immutable) version id, so a restored entry can never be out of date.
     *
     * @return a copy of the cached views
     */
    static Map<String, View> exportViews() {
        return new HashMap<>(views.asMap());
    }

    /**
     * Restores previously exported views into the exact-version cache, skipping any that are
     * already cached. Meant to run once at startup.
     *
     * @param map the views to restore
     * @return the number of restored views
     */
    static int importViews(Map<String, View> map) {
        int count = 0;
        for (Map.Entry<String, View> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            if (views.getIfPresent(e.getKey()) != null) continue;
            views.put(e.getKey(), e.getValue());
            count++;
        }
        return count;
    }

    /**
     * Restores previously exported latest-version resolutions, keeping their original
     * resolution times so the normal re-resolution age logic takes over. Entries already
     * memoized are left alone, as are entries older than the given maximum age or carrying
     * a timestamp from the future. Meant to run once at startup.
     *
     * @param map      the resolutions to restore
     * @param maxAgeMs entries resolved further back than this are dropped
     * @return the number of restored entries
     */
    static int importResolvedViews(Map<String, Pair<Long, View>> map, long maxAgeMs) {
        long timeNow = System.currentTimeMillis();
        int count = 0;
        for (Map.Entry<String, Pair<Long, View>> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue().getLeft() == null || e.getValue().getRight() == null) continue;
            long t = e.getValue().getLeft();
            if (t > timeNow || timeNow - t > maxAgeMs) continue;
            if (latestResolvedViews.getIfPresent(e.getKey()) != null) continue;
            latestResolvedViews.put(e.getKey(), e.getValue());
            count++;
        }
        return count;
    }

    /**
     * Get a View by its ID, resolving to the latest version (following the
     * supersedes chain).
     *
     * @param id the ID of the View
     * @return the View object
     */
    public static View get(String id) {
        return get(id, true);
    }

    /**
     * Get a View by its ID.
     *
     * @param id            the ID of the View
     * @param resolveLatest if true, follow the supersedes chain to load the latest
     *                      version of the view; if false, load exactly the given
     *                      version without a latest-version lookup. Pass false when
     *                      the caller already holds a latest-resolved IRI (e.g. from
     *                      the get-view-displays query, which now resolves it
     *                      server-side) to avoid a redundant network round-trip.
     *                      A version declaring {@code gen:governedBy} still gets the
     *                      space-based resolution here even with false: its float is
     *                      not supersedes-based, so the caller's server-side
     *                      resolution doesn't cover it.
     * @return the View object
     */
    public static View get(String id, boolean resolveLatest) {
        String npId = toNanopubId(id);
        if (!resolveLatest) {
            View exact = getExactVersion(id, npId);
            if (exact == null || exact.getGoverningSpace() == null || exact.getViewKindIri() == null) {
                return exact;
            }
            // fall through to the memoized latest path, which resolves a governed
            // version space-based (never supersedes-based) for this pin
        }
        Pair<Long, View> memo = latestResolvedViews.getIfPresent(id);
        if (memo != null) {
            if (System.currentTimeMillis() - memo.getLeft() > REFRESH_RESOLUTION_AFTER_MS) {
                triggerResolutionRefresh(id, npId);
            }
            return memo.getRight();
        }
        View resolved = resolveLatestVersion(id, npId);
        if (resolved != null) {
            latestResolvedViews.put(id, Pair.of(System.currentTimeMillis(), resolved));
        }
        return resolved;
    }

    /**
     * Re-resolves the latest version of a view, going back to the query API instead of
     * trusting what is memoized. This is what lets a view display's "refresh now" bring the
     * <em>view</em> up to date and not just its results (issue #654): a memoized resolution
     * is only re-checked once a minute in the background, and a display whose view was
     * resolved server-side by the {@code get-view-displays} query carries an exact version
     * that is never re-checked at all, so a newly published version of the view would
     * otherwise not show up until the page's structure happened to be refreshed.
     * <p>
     * Every memoized resolution leading to the given version is dropped along with the
     * lookups behind it, so that pages reaching this view by another id — a built-in view is
     * looked up by the id hard-coded for it, not by the version that id resolves to —
     * re-resolve it on their next render too.
     *
     * @param id the id of the view version currently shown
     * @return the view's current latest version, which is the given one when there is no
     * newer version or the lookup fails, or null if the view cannot be loaded at all
     */
    public static View refreshLatestVersion(String id) {
        // The ids whose lookups are to be forgotten: the given one, plus every memo key
        // that leads to it.
        Set<String> staleIds = new HashSet<>();
        staleIds.add(id);
        for (Map.Entry<String, Pair<Long, View>> memo : latestResolvedViews.asMap().entrySet()) {
            View memoized = memo.getValue().getRight();
            if (memo.getKey().equals(id) || (memoized != null && id.equals(memoized.getId()))) {
                latestResolvedViews.invalidate(memo.getKey());
                staleIds.add(memo.getKey());
            }
        }
        for (String staleId : staleIds) forgetLatestVersionLookup(staleId);
        View resolved = resolveLatestVersion(id, toNanopubId(id));
        if (resolved != null) {
            latestResolvedViews.put(id, Pair.of(System.currentTimeMillis(), resolved));
        }
        return resolved;
    }

    /**
     * Marks the version lookup behind a view id as outdated, so that the next resolution
     * asks the API instead of answering from what it holds: the governed-version query for
     * a view that floats within its space, the supersedes-chain lookup (its memo and its
     * cached response both) for one that does not.
     */
    private static void forgetLatestVersionLookup(String viewId) {
        String npId = toNanopubId(viewId);
        View pinned = getExactVersion(viewId, npId);
        if (pinned != null && pinned.getGoverningSpace() != null && pinned.getViewKindIri() != null) {
            ApiCache.clearCache(GovernedVersions.getQueryRef(
                    pinned.getViewKindIri().stringValue(), pinned.getGoverningSpace().stringValue()), 0);
        } else {
            QueryApiAccess.forgetLatestVersion(npId);
            ApiCache.clearCache(new QueryRef(QueryApiAccess.GET_LATEST_VERSION_OF_NP, "np", npId), 0);
        }
    }

    /**
     * The id of the nanopub a view id belongs to: the view id up to and including its
     * artifact code. An id that is already a nanopub id is returned unchanged.
     */
    private static String toNanopubId(String viewId) {
        return viewId.replaceFirst("^(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
    }

    /**
     * Resolves a view id to the latest version of its view definition, falling
     * back to the exact given version if the lookup fails or doesn't yield a
     * single embedded view IRI. This is the network-touching part of
     * {@link #get(String)}. A version that declares {@code gen:governedBy}
     * resolves space-based (authority-scoped latest-wins within its
     * {@code (kind, space)} pair); one that doesn't follows the supersedes
     * chain as before. See docs/views-and-presets-as-maintained-resources.md.
     */
    private static View resolveLatestVersion(String id, String npId) {
        View pinned = getExactVersion(id, npId);
        if (pinned != null && pinned.getGoverningSpace() != null && pinned.getViewKindIri() != null) {
            return resolveGovernedVersion(pinned);
        }
        // Automatically selecting latest version of view definition:
        // TODO This should be made configurable at some point, so one can make it a fixed version.
        try {
            String latestNpId = QueryApiAccess.getLatestVersionId(npId);
            if (!latestNpId.equals(npId)) {
                Nanopub np = Utils.getAsNanopub(latestNpId);
                if (np != null) {
                    Set<String> embeddedIris = NanopubUtils.getEmbeddedIriIds(np);
                    if (embeddedIris.size() == 1) {
                        String latestId = embeddedIris.iterator().next();
                        View cached = views.getIfPresent(latestId);
                        if (cached == null) {
                            cached = new View(latestId, np);
                            views.put(latestId, cached);
                        }
                        return cached;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error resolving latest version for view: {}", id, ex);
        }
        return pinned;
    }

    /**
     * Resolves the latest space-governed version of the pinned view's
     * {@code (kind, space)} pair: the newest version declaring the same kind and
     * governing space, signed by a current member+ of that space, with the kind
     * validated as maintained by the space — all checked server-side by the
     * {@link QueryApiAccess#GET_LATEST_GOVERNED_VERSION} query. The pin is the
     * floor: on an empty result (or any failure) the pinned version stands,
     * un-revalidated.
     */
    private static View resolveGovernedVersion(View pinned) {
        try {
            String latestId = GovernedVersions.getLatestVersionIriSync(
                    pinned.getViewKindIri().stringValue(), pinned.getGoverningSpace().stringValue());
            if (latestId != null && !latestId.equals(pinned.getId())) {
                String latestNpId = toNanopubId(latestId);
                View resolved = getExactVersion(latestId, latestNpId);
                if (resolved != null) return resolved;
            }
        } catch (Exception ex) {
            logger.error("Error resolving governed version for view: {}", pinned.getId(), ex);
        }
        return pinned;
    }

    /**
     * Loads the view exactly as given, without a latest-version lookup.
     */
    private static View getExactVersion(String id, String npId) {
        Nanopub np = Utils.getAsNanopub(npId);
        View cached = views.getIfPresent(id);
        if (cached == null) {
            try {
                cached = new View(id, np);
                views.put(id, cached);
            } catch (Exception ex) {
                logger.error("Couldn't load nanopub for resource: {}", id, ex);
            }
        }
        return cached;
    }

    /**
     * Re-resolves a stale memoized latest-version resolution in the background.
     * The stale value keeps being served meanwhile; on failure it is re-stamped
     * with the current time, so failing lookups are retried at most once per
     * {@link #REFRESH_RESOLUTION_AFTER_MS} rather than on every render.
     */
    private static void triggerResolutionRefresh(String id, String npId) {
        if (!refreshingViews.add(id)) return;
        NanodashThreadPool.submit(() -> {
            try {
                View resolved = resolveLatestVersion(id, npId);
                if (resolved == null) {
                    Pair<Long, View> previous = latestResolvedViews.getIfPresent(id);
                    resolved = (previous == null ? null : previous.getRight());
                }
                if (resolved != null) {
                    latestResolvedViews.put(id, Pair.of(System.currentTimeMillis(), resolved));
                }
            } finally {
                refreshingViews.remove(id);
            }
        });
    }

    private String id;
    private Nanopub nanopub;
    private IRI viewKind;
    private IRI governingSpace;
    private String label;
    private String title = "View";
    private String description;
    private GrlcQuery query;
    private String queryField = "resource";
    private Integer pageSize;
    private Integer displayWidth;
    private String structuralPosition;
    private List<IRI> viewResultActionList = new ArrayList<>();
    private List<IRI> viewEntryActionList = new ArrayList<>();
    private Set<IRI> appliesToClasses = new HashSet<>();
    private Set<IRI> appliesToNamespaces = new HashSet<>();
    private Map<IRI, Template> actionTemplateMap = new HashMap<>();
    private Map<IRI, String> actionTemplateTargetFieldMap = new HashMap<>();
    private Map<IRI, IRI> actionTemplateTypeMap = new HashMap<>();
    private Map<IRI, String> actionTemplatePartFieldMap = new HashMap<>();
    private Map<IRI, List<String>> actionTemplateQueryMappingsMap = new HashMap<>();
    private Map<IRI, String> labelMap = new HashMap<>();
    private IRI viewType;
    private boolean queryForm = false;
    private Map<IRI, Set<IRI>> actionVisibleToMap = new HashMap<>();

    private View(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;
        List<IRI> actionList = new ArrayList<>();
        boolean viewTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(id)) {
                if (st.getPredicate().equals(RDF.TYPE)) {
                    if (st.getObject().equals(KPXL_TERMS.RESOURCE_VIEW)) {
                        viewTypeFound = true;
                    }
                    if (st.getObject() instanceof IRI objIri && supportedViewTypes.contains(objIri)) {
                        viewType = objIri;
                    }
                    if (st.getObject().equals(KPXL_TERMS.QUERY_FORM_VIEW)) {
                        queryForm = true;
                    }
                } else if (st.getPredicate().equals(DCTERMS.IS_VERSION_OF) && st.getObject() instanceof IRI objIri) {
                    viewKind = objIri;
                } else if (st.getPredicate().equals(KPXL_TERMS.GOVERNED_BY) && st.getObject() instanceof IRI objIri) {
                    governingSpace = objIri;
                } else if (st.getPredicate().equals(RDFS.LABEL)) {
                    label = st.getObject().stringValue();
                } else if (st.getPredicate().equals(DCTERMS.TITLE)) {
                    title = st.getObject().stringValue();
                } else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    description = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_QUERY)) {
                    query = GrlcQuery.get(st.getObject().stringValue());
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_QUERY_TARGET_FIELD)) {
                    queryField = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_ACTION) && st.getObject() instanceof IRI objIri) {
                    actionList.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_NAMESPACE) && st.getObject() instanceof IRI objIri) {
                    appliesToNamespaces.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_INSTANCES_OF) && st.getObject() instanceof IRI objIri) {
                    appliesToClasses.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_TARGET_CLASS) && st.getObject() instanceof IRI objIri) {
                    // Deprecated
                    appliesToClasses.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PAGE_SIZE) && st.getObject() instanceof Literal objL) {
                    try {
                        pageSize = Integer.parseInt(objL.stringValue());
                    } catch (NumberFormatException ex) {
                        logger.error("Invalid page size value: {}", objL.stringValue(), ex);
                    }
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_DISPLAY_WIDTH) && st.getObject() instanceof IRI objIri) {
                    displayWidth = columnWidths.get(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_STRUCTURAL_POSITION) && st.getObject() instanceof Literal objL) {
                    structuralPosition = objL.stringValue();
                }
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE)) {
                Template template = TemplateData.get().getTemplate(st.getObject().stringValue());
                actionTemplateMap.put((IRI) st.getSubject(), template);
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_TARGET_FIELD)) {
                putUnlessVoid(actionTemplateTargetFieldMap, (IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_PART_FIELD)) {
                putUnlessVoid(actionTemplatePartFieldMap, (IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_QUERY_MAPPING)) {
                // Repeatable: an action may declare several query mappings (e.g. derive
                // maps both the row np and the local pubkey). "void" means "none".
                String mapping = st.getObject().stringValue();
                if (!"void".equals(mapping)) {
                    actionTemplateQueryMappingsMap.computeIfAbsent((IRI) st.getSubject(), k -> new ArrayList<>()).add(mapping);
                }
            } else if (st.getPredicate().equals(KPXL_TERMS.IS_VISIBLE_TO) && st.getObject() instanceof IRI objIri) {
                // Per-action visibility: gen:isVisibleTo on an action node restricts
                // that action button to viewers holding the given role tier or
                // specific role. See docs/role-specific-views.md.
                actionVisibleToMap.computeIfAbsent((IRI) st.getSubject(), k -> new HashSet<>()).add(objIri);
            } else if (st.getPredicate().equals(RDFS.LABEL)) {
                labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(RDF.TYPE)) {
                if (st.getObject().equals(KPXL_TERMS.VIEW_ACTION) || st.getObject().equals(KPXL_TERMS.VIEW_ENTRY_ACTION)) {
                    actionTemplateTypeMap.put((IRI) st.getSubject(), (IRI) st.getObject());
                }
            }
        }
        for (IRI actionIri : actionList) {
            if (actionTemplateTypeMap.containsKey(actionIri) && actionTemplateTypeMap.get(actionIri).equals(KPXL_TERMS.VIEW_ENTRY_ACTION)) {
                viewEntryActionList.add(actionIri);
            } else {
                viewResultActionList.add(actionIri);
            }
        }
        if (!viewTypeFound) throw new IllegalArgumentException("Not a proper resource view nanopub: " + id);
        // Header views are the one display type without a query (issue #572).
        if (query == null && !KPXL_TERMS.HEADER_VIEW.equals(viewType)) throw new IllegalArgumentException("Query not found: " + id);
    }

    /**
     * Stores an action-field value unless it is the {@code "void"} sentinel.
     * View-creation templates can't leave a statement optional inside a repeated
     * action group, so views carry every action field, with {@code "void"} for the
     * not-applicable ones (its presence is what lets Nanodash repopulate the action
     * group when superseding a view). It is treated here as absent — so e.g. a
     * "void" part field never becomes a bogus {@code param_void}.
     */
    private static void putUnlessVoid(Map<IRI, String> map, IRI key, String value) {
        if (value != null && !value.equals("void")) {
            map.put(key, value);
        }
    }

    /**
     * Gets the ID of the View.
     *
     * @return the ID of the View
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the Nanopub defining this View.
     *
     * @return the Nanopub defining this View
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    public IRI getViewKindIri() {
        return viewKind;
    }

    /**
     * Gets the space governing this view version's {@code (kind, space)} pair via
     * {@code gen:governedBy}, or null if the version doesn't opt into space
     * governance (in which case latest-version resolution stays supersedes-based).
     *
     * @return the governing space IRI, or null
     */
    public IRI getGoverningSpace() {
        return governingSpace;
    }

    /**
     * Gets the label of the View.
     *
     * @return the label of the View
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the title of the View.
     *
     * @return the title of the View
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the description of the View ({@code dct:description}), shown below the
     * title for header views.
     *
     * @return the description, or null if none is declared
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the GrlcQuery associated with the View.
     *
     * @return the GrlcQuery associated with the View, or null for a header view
     */
    public GrlcQuery getQuery() {
        return query;
    }

    /**
     * Gets the query field of the View.
     *
     * @return the query field
     */
    public String getQueryField() {
        return queryField;
    }

    /**
     * Returns the preferred page size.
     *
     * @return page size (0 = everything on first page)
     */
    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public String getStructuralPosition() {
        return structuralPosition;
    }

    /**
     * Gets the visibility restriction declared on a given action node via
     * {@code gen:isVisibleTo}: the set of role-tier or specific-role IRIs a viewer
     * must hold for that action button to be shown. An empty set means the action
     * is visible to everyone (subject to the existing button-list routing).
     *
     * @param actionIri the action IRI (a result or entry action of this view)
     * @return the set of {@code gen:isVisibleTo} IRIs for that action (never null)
     */
    public Set<IRI> getActionVisibleTo(IRI actionIri) {
        return actionVisibleToMap.getOrDefault(actionIri, Collections.emptySet());
    }

    /**
     * Gets the list of action IRIs associated with the View.
     *
     * @return the list of action IRIs
     */
    public List<IRI> getViewResultActionList() {
        return viewResultActionList;
    }

    public List<IRI> getViewEntryActionList() {
        return viewEntryActionList;
    }

    /**
     * Gets the Template for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the Template for the action IRI
     */
    public Template getTemplateForAction(IRI actionIri) {
        return actionTemplateMap.get(actionIri);
    }

    /**
     * Gets the template field for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the template field for the action IRI
     */
    public String getTemplateTargetFieldForAction(IRI actionIri) {
        return actionTemplateTargetFieldMap.get(actionIri);
    }

    public String getTemplatePartFieldForAction(IRI actionIri) {
        return actionTemplatePartFieldMap.get(actionIri);
    }

    /**
     * Gets the query mappings declared for an action: each is {@code "col:target"},
     * mapping result column {@code col} to template parameter {@code param_target}
     * — or, when {@code target} begins with {@code @}, to the raw URL parameter
     * {@code target} (without the {@code param_} prefix), used for fill-mode keys
     * such as {@code @derive-a} / {@code @supersede}. An entry action applies all
     * of these per row; see docs/magic-query-params.md.
     *
     * @param actionIri the action IRI
     * @return the list of mappings (never null; empty if none)
     */
    public List<String> getTemplateQueryMappings(IRI actionIri) {
        List<String> result = new ArrayList<>();
        for (String literal : actionTemplateQueryMappingsMap.getOrDefault(actionIri, Collections.emptyList())) {
            result.addAll(parseMappingLiteral(literal));
        }
        return result;
    }

    /**
     * Splits a query-mapping literal into individual {@code "col:target"} mappings
     * on whitespace. Multiple mappings share a single literal because a
     * view-creation template cannot repeat a statement inside its repeated action
     * group — e.g. {@code "np:nanopubToBeRetracted"} or
     * {@code "derive_target:@derive-a local_pubkey:public-key__.1"}.
     *
     * @param literal the mapping literal (may be null/blank/"void")
     * @return the individual mappings (never null; empty if none)
     */
    public static List<String> parseMappingLiteral(String literal) {
        List<String> mappings = new ArrayList<>();
        if (literal == null || literal.isBlank()) return mappings;
        for (String m : literal.trim().split("\\s+")) {
            if (!m.isEmpty() && !"void".equals(m)) mappings.add(m);
        }
        return mappings;
    }

    /**
     * Gets the set of query result columns that serve only as <em>sources</em> for
     * this view's action query mappings (the {@code col} part of each
     * {@code "col:target"} mapping, across all actions). These columns carry
     * action data — conditional targets, the local-key bundle — not row content, so
     * the result builders skip them when rendering visible columns. A column that
     * happens to be both a display column and a mapping source would also be hidden;
     * map a duplicated/aliased column instead if you need to show one.
     *
     * @return the set of mapping-source column names (never null)
     */
    public Set<String> getActionMappingSourceColumns() {
        Set<String> columns = new HashSet<>();
        for (IRI actionIri : actionTemplateQueryMappingsMap.keySet()) {
            for (String mapping : getTemplateQueryMappings(actionIri)) {
                int idx = mapping.indexOf(':');
                if (idx > 0) columns.add(mapping.substring(0, idx));
            }
        }
        return columns;
    }

    /**
     * Gets the first query mapping for an action, or null. Kept for result-action
     * callers that pass a single {@code values-from-query-mapping}.
     *
     * @param actionIri the action IRI
     * @return the first mapping, or null
     */
    public String getTemplateQueryMapping(IRI actionIri) {
        List<String> mappings = actionTemplateQueryMappingsMap.get(actionIri);
        return (mappings == null || mappings.isEmpty()) ? null : mappings.get(0);
    }

    /**
     * Gets the label for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the label for the action IRI
     */
    public String getLabelForAction(IRI actionIri) {
        return labelMap.get(actionIri);
    }

    public boolean appliesTo(String resourceId, Set<IRI> classes) {
        for (IRI namespace : appliesToNamespaces) {
            if (resourceId.startsWith(namespace.stringValue())) return true;
        }
        if (classes != null) {
            for (IRI c : classes) {
                if (appliesToClasses.contains(c)) return true;
            }
        }
        return false;
    }

    /**
     * Checks if the View has target classes.
     *
     * @return true if the View has target classes, false otherwise
     */
    public boolean appliesToClasses() {
        return !appliesToClasses.isEmpty();
    }

    /**
     * Checks if the View has a specific target class.
     *
     * @param targetClass the target class IRI
     * @return true if the View has the target class, false otherwise
     */
    public boolean appliesToClass(IRI targetClass) {
        return appliesToClasses.contains(targetClass);
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Gets the view type of the View.
     *
     * @return the view type mode IRI
     */
    public IRI getViewType() {
        return viewType;
    }

    /**
     * Whether this view is additionally typed {@code gen:QueryFormView}: on a resource
     * page it renders as a form for the query placeholders not auto-filled from the
     * page context, whose submission leads to the full results page. Orthogonal to
     * {@link #getViewType()}, which then determines how those results render.
     *
     * @return true if this is a query-form view
     */
    public boolean hasQueryForm() {
        return queryForm;
    }

    /**
     * Get the supported view types.
     *
     * @return a set of supported view type IRIs
     */
    public static Set<IRI> getSupportedViewTypes() {
        return supportedViewTypes;
    }

}
