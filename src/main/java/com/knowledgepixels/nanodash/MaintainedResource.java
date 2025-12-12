package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaintainedResource extends ProfiledResource {

    private static final Logger logger = LoggerFactory.getLogger(MaintainedResource.class);

    private static List<MaintainedResource> resourceList;
    private static Map<String, MaintainedResource> resourcesById;
    private static Map<String, MaintainedResource> resourcesByNamespace;
    private static Map<Space, List<MaintainedResource>> resourcesBySpace;
    private static boolean loaded = false;
    private static Long runRootUpdateAfter = null;

    public static synchronized void refresh(ApiResponse resp) {
        resourceList = new ArrayList<>();
        Map<String, MaintainedResource> previousResourcesById = resourcesById;
        resourcesById = new HashMap<>();
        resourcesBySpace = new HashMap<>();
        resourcesByNamespace = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            Space space = Space.get(entry.get("space"));
            if (space == null) continue;
            MaintainedResource resource = null;
            if (previousResourcesById != null) {
                resource = previousResourcesById.get(entry.get("resource"));
            }
            if (resource == null) {
                resource = new MaintainedResource(entry, space);
            }
            if (resourcesById.containsKey(resource.getId())) continue;
            resourceList.add(resource);
            resourcesById.put(resource.getId(), resource);
            resourcesBySpace.computeIfAbsent(space, k -> new ArrayList<>()).add(resource);
            if (resource.getNamespace() != null) {
                // TODO Handle conflicts when two resources claim the same namespace:
                resourcesByNamespace.put(resource.getNamespace(), resource);
            }
        }
        loaded = true;
    }

    /**
     * Check if the resources have been loaded.
     *
     * @return true if loaded, false otherwise.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Ensure that the resources are loaded, fetching them from the API if necessary.
     */
    public static void ensureLoaded() {
        if (resourceList == null) {
            try {
                if (runRootUpdateAfter != null) {
                    while (System.currentTimeMillis() < runRootUpdateAfter) {
                        Thread.sleep(100);
                    }
                    runRootUpdateAfter = null;
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
            refresh(QueryApiAccess.forcedGet(new QueryRef("get-maintained-resources")));
        }
    }

    public static void forceRootRefresh(long waitMillis) {
        resourceList = null;
        runRootUpdateAfter = System.currentTimeMillis() + waitMillis;
    }

    /**
     * Get the list of all maintained resources.
     *
     * @return List of resources.
     */
    public static List<MaintainedResource> getResourceList() {
        ensureLoaded();
        return resourceList;
    }

    public static List<MaintainedResource> getResourcesBySpace(Space space) {
        return resourcesBySpace.computeIfAbsent(space, k -> new ArrayList<>());
    }

    /**
     * Get a maintained resource by its id.
     *
     * @param id The id of the resource.
     * @return The corresponding MaintainedResource object, or null if not found.
     */
    public static MaintainedResource get(String id) {
        ensureLoaded();
        return resourcesById.get(id);
    }

    public static MaintainedResource getByNamespace(String namespace) {
        return resourcesByNamespace.get(namespace);
    }

    public static String getNamespace(Object stringOrIri) {
        return stringOrIri.toString().replaceFirst("([#/])[^#/]+$", "$1");
    }

    public static void refresh() {
        refresh(QueryApiAccess.forcedGet(new QueryRef("get-maintained-resources")));
        for (MaintainedResource resource : resourceList) {
            resource.setDataNeedsUpdate();
        }
    }

    private String label, nanopubId, namespace;
    private Nanopub nanopub;

    private MaintainedResource(ApiResponseEntry resp, Space space) {
        super(resp.get("resource"));
        initialize(resp, space);
    }

    private void initialize(ApiResponseEntry resp, Space space) {
        initSpace(space);
        this.label = resp.get("label");
        this.nanopubId = resp.get("np");
        this.namespace = resp.get("namespace");
        if (namespace != null && namespace.isBlank()) namespace = null;
        this.nanopub = Utils.getAsNanopub(nanopubId);
    }

    @Override
    public String getNanopubId() {
        return nanopubId;
    }

    @Override
    public Nanopub getNanopub() {
        return nanopub;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public boolean appliesTo(String elementId, Set<IRI> classes) {
        triggerDataUpdate();
        for (ViewDisplay v : getViewDisplays()) {
            if (v.appliesTo(elementId, classes)) return true;
        }
        return false;
    }

}
