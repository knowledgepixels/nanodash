package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.MaintainedResourceFactory;
import com.knowledgepixels.nanodash.domain.Space;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintainedResourceRepository {

    private static final Logger logger = LoggerFactory.getLogger(MaintainedResourceRepository.class);

    private static final MaintainedResourceRepository INSTANCE = new MaintainedResourceRepository();

    private volatile List<MaintainedResource> resourceList;
    private Map<String, MaintainedResource> resourcesById;
    private Map<String, MaintainedResource> resourcesByNamespace;
    private Map<Space, List<MaintainedResource>> resourcesBySpace;
    private boolean loaded = false;
    private volatile Long runRootUpdateAfter = null;
    private volatile long lastRefreshTime = 0;
    private final Object loadLock = new Object();

    /**
     * Get the singleton instance of MaintainedResourceRepository.
     *
     * @return The singleton instance of MaintainedResourceRepository.
     */
    public static MaintainedResourceRepository get() {
        return INSTANCE;
    }

    private MaintainedResourceRepository() {
    }

    /**
     * Refresh the list of maintained resources from the API response, updating the internal state accordingly.
     *
     * @param resp The API response containing maintained resource data.
     */
    public synchronized void refresh(ApiResponse resp) {
        List<MaintainedResource> newResourceList = new ArrayList<>();
        Map<String, MaintainedResource> newResourcesById = new HashMap<>();
        Map<Space, List<MaintainedResource>> newResourcesBySpace = new HashMap<>();
        Map<String, MaintainedResource> newResourcesByNamespace = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            Space space = SpaceRepository.get().findById(entry.get("space"));
            if (space == null) {
                continue;
            }
            MaintainedResource resource = MaintainedResourceFactory.getOrCreate(entry, space);
            if (newResourcesById.containsKey(resource.getId())) {
                continue;
            }
            newResourceList.add(resource);
            newResourcesById.put(resource.getId(), resource);
            newResourcesBySpace.computeIfAbsent(space, k -> new ArrayList<>()).add(resource);
            if (resource.getNamespace() != null) {
                // TODO Handle conflicts when two resources claim the same namespace:
                newResourcesByNamespace.put(resource.getNamespace(), resource);
            }
        }
        MaintainedResourceFactory.removeStale(newResourcesById.keySet());
        resourcesById = newResourcesById;
        resourcesBySpace = newResourcesBySpace;
        resourcesByNamespace = newResourcesByNamespace;
        loaded = true;
        lastRefreshTime = System.currentTimeMillis();
        resourceList = newResourceList; // volatile write last — establishes happens-before for all above
    }

    /**
     * Find a maintained resource by its namespace.
     *
     * @param namespace The namespace to search for.
     * @return The MaintainedResource with the given namespace, or null if not found.
     */
    public MaintainedResource findByNamespace(String namespace) {
        ensureLoaded();
        return resourcesByNamespace.get(namespace);
    }

    /**
     * Find a maintained resource by its ID.
     *
     * @param space The space to which the resource belongs.
     * @return The MaintainedResource with the given ID, or null if not found.
     */
    public List<MaintainedResource> findResourcesBySpace(Space space) {
        return resourcesBySpace.computeIfAbsent(space, k -> new ArrayList<>());
    }

    /**
     * Get a maintained resource by its id.
     *
     * @param id The id of the resource.
     * @return The corresponding MaintainedResource object, or null if not found.
     */
    public MaintainedResource findById(String id) {
        ensureLoaded();
        return resourcesById.get(id);
    }

    /**
     * Ensure that the resources are loaded, fetching them from the API if necessary.
     */
    public void ensureLoaded() {
        if (resourceList == null) {
            try {
                synchronized (loadLock) {
                    if (runRootUpdateAfter != null) {
                        while (runRootUpdateAfter != null && System.currentTimeMillis() < runRootUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runRootUpdateAfter = null;
                    }
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
            if (resourceList == null) { // double-check after potential wait
                refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES), true));
            }
        } else if (System.currentTimeMillis() - lastRefreshTime > 60_000) {
            refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES), true));
        }
    }

    /**
     * Force a refresh of the maintained resources on the next access, with an optional delay to allow for updates to propagate.
     *
     * @param waitMillis Number of milliseconds to wait before allowing the next access to trigger a refresh. If 0, the refresh will happen immediately on the next access.
     */
    public void forceRootRefresh(long waitMillis) {
        resourceList = null;
        runRootUpdateAfter = System.currentTimeMillis() + waitMillis;
    }

    /**
     * Refresh the maintained resources by fetching the latest data from the API and updating the internal state accordingly.
     */
    public void refresh() {
        refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES), true));
        for (MaintainedResource resource : resourceList) {
            resource.setDataNeedsUpdate();
        }
    }

}
