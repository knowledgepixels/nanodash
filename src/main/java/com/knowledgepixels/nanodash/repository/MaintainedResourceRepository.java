package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.SpacesRepoAccess;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.MaintainedResourceFactory;
import com.knowledgepixels.nanodash.domain.Space;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String MAINTAINED_RESOURCES_QUERY = SpacesRepoAccess.PREFIXES
            + "SELECT ?resource ?space ?np ?label ?namespace ?date WHERE {\n"
            + SpacesRepoAccess.CURRENT_STATE_POINTER
            + "  GRAPH ?g { ?resource npa:isMaintainedBy ?space . }\n"
            + "  GRAPH npa:spacesGraph {\n"
            + "    ?d a npa:MaintainedResourceDeclaration ;\n"
            + "       npa:resourceIri ?resource ;\n"
            + "       npa:maintainerSpace ?space ;\n"
            + "       npa:viaNanopub ?np ;\n"
            + "       dct:created ?date .\n"
            + "  }\n"
            + "  GRAPH npa:graph {\n"
            + "    ?np np:hasAssertion ?a .\n"
            + "    OPTIONAL { ?np rdfs:label ?label }\n"
            + "  }\n"
            + "  GRAPH ?a { OPTIONAL { ?resource gen:hasNamespace ?namespace } }\n"
            + "} ORDER BY DESC(?date)";

    /**
     * Refresh the list of maintained resources from the spaces repo. Pulls
     * server-validated {@code npa:isMaintainedBy} links from the current
     * space-state graph; only the most recent declaration per resource is kept.
     */
    public synchronized void refresh() {
        List<MaintainedResource> newResourceList = new ArrayList<>();
        Map<String, MaintainedResource> newResourcesById = new HashMap<>();
        Map<Space, List<MaintainedResource>> newResourcesBySpace = new HashMap<>();
        Map<String, MaintainedResource> newResourcesByNamespace = new HashMap<>();
        Set<String> seenResources = new HashSet<>();
        int[] rowCount = {0};
        Set<String> missingSpaces = new HashSet<>();
        SpacesRepoAccess.get().select(MAINTAINED_RESOURCES_QUERY, null, b -> {
            rowCount[0]++;
            String resourceId = b.getValue("resource").stringValue();
            if (!seenResources.add(resourceId)) return null; // first row (newest date) wins
            String spaceId = b.getValue("space").stringValue();
            Space space = SpaceRepository.get().findById(spaceId);
            if (space == null) { missingSpaces.add(spaceId); return null; }
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("resource", resourceId);
            entry.add("np", b.getValue("np") == null ? null : b.getValue("np").stringValue());
            if (b.getValue("label") != null) entry.add("label", b.getValue("label").stringValue());
            if (b.getValue("namespace") != null) entry.add("namespace", b.getValue("namespace").stringValue());
            MaintainedResource resource = MaintainedResourceFactory.getOrCreate(entry, space);
            newResourceList.add(resource);
            newResourcesById.put(resourceId, resource);
            newResourcesBySpace.computeIfAbsent(space, k -> new ArrayList<>()).add(resource);
            if (resource.getNamespace() != null) {
                // TODO Handle conflicts when two resources claim the same namespace:
                newResourcesByNamespace.put(resource.getNamespace(), resource);
            }
            return null;
        });
        logger.info("Maintained-resources refresh: {} rows from spaces repo, {} kept, {} skipped because their maintaining space was not in SpaceRepository. Missing spaces: {}",
                rowCount[0], newResourcesById.size(), rowCount[0] - newResourcesById.size(), missingSpaces);
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
                refresh();
            }
        } else if (System.currentTimeMillis() - lastRefreshTime > 60_000) {
            refresh();
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
     * Refresh the maintained resources and mark each as needing a downstream data update.
     */
    public void refreshAndInvalidate() {
        refresh();
        for (MaintainedResource resource : resourceList) {
            resource.setDataNeedsUpdate();
        }
    }

}
