package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfiledResource implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ProfiledResource.class);

    protected static class ResourceData implements Serializable {
        List<ViewDisplay> viewDisplays = new ArrayList<>();
    }

    private String id;
    private Space space;
    private ResourceData data = new ResourceData();
    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    protected ProfiledResource(String id, Space space) {
        this.id = id;
        this.space = space;
    }

    public String getId() {
        return id;
    }

    protected synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                try {
                    ResourceData newData = new ResourceData();

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-view-displays", "resource", id)).getData()) {
                        if (space != null && !space.isAdminPubkey(r.get("pubkey"))) continue;
                        try {
                            newData.viewDisplays.add(ViewDisplay.get(r.get("display")));
                        } catch (IllegalArgumentException ex) {
                            logger.error("Couldn't generate view display object", ex);
                        }
                    }
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

    public void setDataNeedsUpdate() {
        dataNeedsUpdate = true;
    }

    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    public List<ViewDisplay> getViewDisplays() {
        return data.viewDisplays;
    }

    @Override
    public String toString() {
        return id;
    }

}
