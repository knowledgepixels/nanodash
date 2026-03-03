package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class representing a resource with a profile in the Nanodash application.
 * This class provides common functionality for resources that have associated profiles, such as spaces and users.
 */
public abstract class AbstractResourceWithProfile implements Serializable, ResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceWithProfile.class);

    private static final Map<Class<?>, Map<String, AbstractResourceWithProfile>> instances = new ConcurrentHashMap<>();

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
    public synchronized Thread triggerDataUpdate() {
        if (dataNeedsUpdate) {
            logger.info("Data needs update for resource {}, starting update thread", id);
            dataNeedsUpdate = false;
            Thread thread = new Thread(() -> {
                try {
                    if (runUpdateAfter != null) {
                        while (System.currentTimeMillis() < runUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runUpdateAfter = null;
                    }

                    ResourceWithProfile newData = new ResourceWithProfile();

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_VIEW_DISPLAYS, "resource", id), true).getData()) {
                        if (space != null && !space.isAdminPubkey(r.get("pubkey"))) {
                            continue;
                        }
                        try {
                            newData.viewDisplays.add(ViewDisplay.get(r.get("display")));
                        } catch (IllegalArgumentException ex) {
                            logger.error("Couldn't generate view display object", ex);
                        }
                    }
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update data for resource {}", id, ex);
                    dataNeedsUpdate = true;
                }
            });
            thread.start();
            return thread;
        }
        return null;
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
        return getViewDisplays(true, getId(), null);
    }

    @Override
    public List<ViewDisplay> getPartLevelViewDisplays(String resourceId, Set<IRI> classes) {
        return getViewDisplays(false, resourceId, classes);
    }

    private List<ViewDisplay> getViewDisplays(boolean toplevel, String resourceId, Set<IRI> classes) {
        triggerDataUpdate();
        List<ViewDisplay> viewDisplays = new ArrayList<>();
        Set<IRI> viewKinds = new HashSet<>();

        for (ViewDisplay vd : getViewDisplays()) {
            IRI kind = vd.getViewKindIri();
            if (kind != null) {
                if (viewKinds.contains(kind)) {
                    continue;
                }
                viewKinds.add(vd.getViewKindIri());
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

}
