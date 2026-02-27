package com.knowledgepixels.nanodash.repository;

import com.github.jsonldjava.shaded.com.google.common.collect.Ordering;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.Space;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SpaceRepository {

    private static final Logger logger = LoggerFactory.getLogger(SpaceRepository.class);

    private static final SpaceRepository INSTANCE = new SpaceRepository();

    public static SpaceRepository get() {
        return INSTANCE;
    }

    private List<Space> spaceList;
    private Map<String, List<Space>> spaceListByType;
    private Map<String, Space> spacesByCoreInfo = new HashMap<>();
    private Map<String, Space> spacesById;
    private Map<Space, Set<Space>> subspaceMap;
    private Map<Space, Set<Space>> superspaceMap;
    private boolean loaded = false;
    private Long runRootUpdateAfter = null;

    private SpaceRepository() {
    }

    /**
     * Refresh the list of spaces from the API response.
     *
     * @param resp The API response containing space data.
     */
    public synchronized void refresh(ApiResponse resp) {
        spaceList = new ArrayList<>();
        spaceListByType = new HashMap<>();
        Map<String, Space> prevSpacesByCoreInfoPrev = spacesByCoreInfo;
        spacesByCoreInfo = new HashMap<>();
        spacesById = new HashMap<>();
        subspaceMap = new HashMap<>();
        superspaceMap = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            String id = getCoreInfoString(entry);
            Space space;
            if (prevSpacesByCoreInfoPrev.containsKey(id)) {
                space = prevSpacesByCoreInfoPrev.get(id);
            } else {
                space = new Space(entry);
            }
            spaceList.add(space);
            spaceListByType.computeIfAbsent(space.getType(), k -> new ArrayList<>()).add(space);
            spacesByCoreInfo.put(space.getCoreInfoString(), space);
            spacesById.put(space.getId(), space);
        }
        for (Space space : spaceList) {
            Space superSpace = this.getIdSuperspace(space);
            if (superSpace == null) continue;
            subspaceMap.computeIfAbsent(superSpace, k -> new HashSet<>()).add(space);
            superspaceMap.computeIfAbsent(space, k -> new HashSet<>()).add(superSpace);
            space.setDataNeedsUpdate();
        }
        loaded = true;
    }

    private static final String ensureLoadedLock = "";

    public boolean areAllSpacesInitialized() {
        for (Space space : spaceList) {
            if (!space.isDataInitialized()) return false;
        }
        return true;
    }

    public void triggerAllDataUpdates() {
        for (Space space : spaceList) {
            space.triggerDataUpdate();
        }
    }

    /**
     * Check if the spaces have been loaded.
     *
     * @return true if loaded, false otherwise.
     */
    public boolean isLoaded() {
        return loaded;
    }

    public void forceRootRefresh(long waitMillis) {
        spaceList = null;
        runRootUpdateAfter = System.currentTimeMillis() + waitMillis;
    }

    /**
     * Ensure that the spaces are loaded, fetching them from the API if necessary.
     */
    public void ensureLoaded() {
        if (spaceList == null) {
            try {
                synchronized (ensureLoadedLock) {
                    if (runRootUpdateAfter != null) {
                        while (System.currentTimeMillis() < runRootUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runRootUpdateAfter = null;
                    }
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
            refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), true));
        }
    }

    /**
     * Get a space by its id.
     *
     * @param id The id of the space.
     * @return The corresponding Space object, or null if not found.
     */
    public Space findById(String id) {
        ensureLoaded();
        return spacesById.get(id);
    }

    public List<Space> findAll() {
        ensureLoaded();
        return spaceList;
    }

    public List<Space> findByType(String type) {
        ensureLoaded();
        return spaceListByType.computeIfAbsent(type, k -> new ArrayList<>());
    }

    /**
     * Get subspaces of a given space.
     *
     * @param space The space for which to find subspaces.
     * @return List of subspaces.
     */
    public List<Space> findSubspaces(Space space) {
        if (subspaceMap.containsKey(space)) {
            List<Space> subspaces = new ArrayList<>(subspaceMap.get(space));
            subspaces.sort(Ordering.usingToString());
            return subspaces;
        }
        return new ArrayList<>();
    }

    public List<Space> findSubspaces(Space space, String type) {
        List<Space> l = new ArrayList<>();
        for (Space s : findSubspaces(space)) {
            if (s.getType().equals(type)) l.add(s);
        }
        return l;
    }

    /**
     * Get superspaces of this space.
     *
     * @return List of superspaces.
     */
    public List<Space> findSuperspaces(Space space) {
        if (superspaceMap.containsKey(space)) {
            List<Space> superspaces = new ArrayList<>(superspaceMap.get(space));
            superspaces.sort(Ordering.usingToString());
            return superspaces;
        }
        return new ArrayList<>();
    }

    /**
     * Mark all spaces as needing a data update.
     */
    public void refresh() {
        refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), true));
        for (Space space : spaceList) {
            space.dataNeedsUpdate = true;
        }
    }

    private static String getCoreInfoString(ApiResponseEntry entry) {
        return entry.get("space") + " " + entry.get("np");
    }

    private Space getIdSuperspace(Space space) {
        String id = space.getId();
        if (!id.matches("https?://[^/]+/.*/[^/]*/?")) return null;
        String superId = id.replaceFirst("(https?://[^/]+/.*)/[^/]*/?", "$1");
        return spacesById.get(superId);
    }

}