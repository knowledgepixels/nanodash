package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract class representing a resource with a profile in the Nanodash application.
 * This class provides common functionality for resources that have associated profiles, such as spaces and users.
 */
public abstract class AbstractResourceWithProfile implements Serializable, ResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceWithProfile.class);

    protected static class ResourceData implements Serializable {
        List<ViewDisplay> viewDisplays = new ArrayList<>();
    }

    private static Map<String, AbstractResourceWithProfile> instances = new HashMap<>();

    public static void refresh() {
        for (AbstractResourceWithProfile r : instances.values()) {
            r.setDataNeedsUpdate();
        }
    }

    public static void forceRefresh(String id, long waitMillis) {
        if (isResourceWithProfile(id)) {
            instances.get(id).forceRefresh(waitMillis);
        }
    }

    public static boolean isResourceWithProfile(String id) {
        return instances.containsKey(id);
    }

    public static AbstractResourceWithProfile get(String id) {
        return instances.get(id);
    }

    private String id;
    private Space space;
    private ResourceData data = new ResourceData();
    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;
    private Long runUpdateAfter = null;

    protected AbstractResourceWithProfile(String id) {
        this.id = id;
        instances.put(id, this);
    }

    protected void initSpace(Space space) {
        this.space = space;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized Thread triggerDataUpdate() {
        if (dataNeedsUpdate) {
            Thread thread = new Thread(() -> {
                try {
                    if (runUpdateAfter != null) {
                        while (System.currentTimeMillis() < runUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runUpdateAfter = null;
                    }

                    ResourceData newData = new ResourceData();

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_VIEW_DISPLAYS, "resource", id), true).getData()) {
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
                    logger.error("Error while trying to update space data: {}", ex.getMessage());
                    dataNeedsUpdate = true;
                }
            });
            thread.start();
            dataNeedsUpdate = false;
            return thread;
        }
        return null;
    }

    public void forceRefresh(long waitMillis) {
        dataNeedsUpdate = true;
        dataInitialized = false;
        runUpdateAfter = System.currentTimeMillis() + waitMillis;
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

        // This is a temporary hack to always show the latest nanopubs view for users by default without needing to create a ViewDisplay for each user
        // TODO remove this once we have a better system for default views
        if (User.isUser(resourceId)) {
            ViewDisplay latestNpsViewDisplay = new ViewDisplay(View.get("https://w3id.org/np/RAjYa33Z3H1whRl486AW3LMnV11WQqkTqvuHROhKbmtlE/latest-nanopubs-example"));
            viewDisplays.add(latestNpsViewDisplay);
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
        List<Space> parents = current.getSuperspaces();
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
