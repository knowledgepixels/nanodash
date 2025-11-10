package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

public class MaintainedResource implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(MaintainedResource.class);

    private static List<MaintainedResource> resourceList;
    private static Map<String, MaintainedResource> resourcesById;
    private static Map<String, MaintainedResource> resourcesByNamespace;
    private static Map<Space, List<MaintainedResource>> resourcesBySpace;
    private static boolean loaded = false;

    public static synchronized void refresh(ApiResponse resp) {
        resourceList = new ArrayList<>();
        resourcesById = new HashMap<>();
        resourcesBySpace = new HashMap<>();
        resourcesByNamespace = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            Space space = Space.get(entry.get("space"));
            if (space == null) continue;
            MaintainedResource resource = new MaintainedResource(entry, space);
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
            refresh(QueryApiAccess.forcedGet(new QueryRef("get-maintained-resources")));
        }
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
            resource.dataNeedsUpdate = true;
        }
    }

    private String id, label, nanopubId, namespace;
    private Space space;
    private Nanopub nanopub;
    private ResourceData data = new ResourceData();

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private static class ResourceData implements Serializable {
        List<ViewDisplay> topLevelViews = new ArrayList<>();
        List<ViewDisplay> partLevelViews = new ArrayList<>();
    }

    private MaintainedResource(ApiResponseEntry resp, Space space) {
        this.space = space;
        this.id = resp.get("resource");
        this.label = resp.get("label");
        this.nanopubId = resp.get("np");
        this.namespace = resp.get("namespace");
        if (namespace != null && namespace.isBlank()) namespace = null;
        this.nanopub = Utils.getAsNanopub(nanopubId);
    }

    public Space getSpace() {
        return space;
    }

    public String getId() {
        return id;
    }

    public String getNanopubId() {
        return nanopubId;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public String getLabel() {
        return label;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    public List<ViewDisplay> getTopLevelViews() {
        triggerDataUpdate();
        // TODO Check for compliance with target classes here too:
        return data.topLevelViews;
    }

    public List<ViewDisplay> getPartLevelViews(Set<IRI> classes) {
        triggerDataUpdate();
        List<ViewDisplay> viewDisplays = new ArrayList<>();
        for (ViewDisplay v : data.partLevelViews) {
            if (v.getView().hasTargetClasses()) {
                for (IRI c : classes) {
                    if (v.getView().hasTargetClass(c)) {
                        viewDisplays.add(v);
                        break;
                    }
                }
            } else {
                viewDisplays.add(v);
            }
        }
        return viewDisplays;
    }

    public boolean coversElement(String elementId) {
        triggerDataUpdate();
        for (ViewDisplay v : data.topLevelViews) {
            if (v.getView().coversElement(elementId)) return true;
        }
        for (ViewDisplay v : data.partLevelViews) {
            if (v.getView().coversElement(elementId)) return true;
        }
        return false;
    }

    private synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                try {
                    ResourceData newData = new ResourceData();

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-view-displays", "resource", id)).getData()) {
                        if (!space.isAdminPubkey(r.get("pubkey"))) continue;
                        try {
                            ViewDisplay vd = ViewDisplay.get(r.get("display"));
                            if (ResourceView.PART_LEVEL_VIEW_DISPLAY.stringValue().equals(r.get("displayType"))) {
                                newData.partLevelViews.add(vd);
                            } else {
                                newData.topLevelViews.add(vd);
                            }
                        } catch (IllegalArgumentException ex) {
                            logger.error("Couldn't generate view display object", ex);
                        }
                    }
                    Collections.sort(newData.topLevelViews);
                    Collections.sort(newData.partLevelViews);
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update space data: {}", ex);
                    dataNeedsUpdate = true;
                }
            }).start();
            dataNeedsUpdate = false;
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
