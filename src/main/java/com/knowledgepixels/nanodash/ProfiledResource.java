package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;

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

    public Space getSpace() {
        return space;
    }

    public String getNanopubId() {
        return null;
    }

    public Nanopub getNanopub() {
        return null;
    }
    public String getNamespace() {
        return null;
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

    public List<ViewDisplay> getViewDisplays(boolean toplevel, Set<IRI> classes) {
        triggerDataUpdate();
        List<ViewDisplay> viewDisplays = new ArrayList<>();
        Set<IRI> viewKinds = new HashSet<>();

        for (ViewDisplay vd : getViewDisplays()) {
            IRI kind = vd.getViewKindIri();
            if (kind != null) {
                if (viewKinds.contains(kind)) continue;
                viewKinds.add(vd.getViewKindIri());
            }
            if (vd.hasType(KPXL_TERMS.DEACTIVATED_VIEW_DISPLAY)) continue;

            if (vd.appliesTo(getId(), null)) {
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
    public String toString() {
        return id;
    }

}
