package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static boolean loaded = false;

    public static synchronized void refresh(ApiResponse resp) {
        resourceList = new ArrayList<>();
        resourcesById = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            Space space = Space.get(entry.get("space"));
            if (space == null) continue;
            MaintainedResource resource = new MaintainedResource(entry, space);
            if (resourcesById.containsKey(resource.getId())) continue;
            resourceList.add(resource);
            resourcesById.put(resource.getId(), resource);
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

    private String id, label, nanopubId;
    private Space space;
    private Nanopub nanopub;
    private ResourceData data = new ResourceData();

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private static class ResourceData implements Serializable {
        List<ResourceView> views = new ArrayList<>();
    }

    private MaintainedResource(ApiResponseEntry resp, Space space) {
        this.space = space;
        this.id = resp.get("resource");
        this.label = resp.get("label");
        this.nanopubId = resp.get("np");
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

    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    public List<ResourceView> getViews() {
        triggerDataUpdate();
        return data.views;
    }

    private synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                try {
                    ResourceData newData = new ResourceData();

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-views-for-resource", "resource", id)).getData()) {
                        if (!space.isAdminPubkey(r.get("pubkey"))) continue;
                        ResourceView view = ResourceView.get(r.get("view"));
                        if (view == null) continue;
                        newData.views.add(view);
                    }
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update space data: {}", ex);
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
