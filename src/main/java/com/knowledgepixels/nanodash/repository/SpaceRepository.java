package com.knowledgepixels.nanodash.repository;

import com.github.jsonldjava.shaded.com.google.common.collect.Ordering;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.SpaceFactory;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Repository class for managing Space instances, providing methods to refresh, retrieve, and query spaces based on API responses.
 */
public class SpaceRepository {

    private static final Logger logger = LoggerFactory.getLogger(SpaceRepository.class);

    private static final SpaceRepository INSTANCE = new SpaceRepository();

    /**
     * Get the singleton instance of SpaceRepository.
     *
     * @return The singleton instance of SpaceRepository.
     */
    public static SpaceRepository get() {
        return INSTANCE;
    }

    private List<Space> spaceList;
    private Map<String, List<Space>> spaceListByType;
    //private Map<String, Space> spacesByCoreInfo = new HashMap<>();
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
        logger.info("Refreshing spaces from API response with {} entries", resp.getData().size());
        spaceList = new ArrayList<>();
        spaceListByType = new HashMap<>();
        //Map<String, Space> prevSpacesByCoreInfoPrev = spacesByCoreInfo;
        //spacesByCoreInfo = new HashMap<>();
        spacesById = new HashMap<>();
        subspaceMap = new HashMap<>();
        superspaceMap = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            //String id = getCoreInfoString(entry);
            Space space;
            //if (prevSpacesByCoreInfoPrev.containsKey(id)) {
            //    space = prevSpacesByCoreInfoPrev.get(id);
            //} else {
            space = SpaceFactory.getOrCreate(entry);
            //}
            spaceList.add(space);
            spaceListByType.computeIfAbsent(space.getType(), k -> new ArrayList<>()).add(space);
            //spacesByCoreInfo.put(space.getCoreInfoString(), space);
            spacesById.put(space.getId(), space);
        }
        SpaceFactory.removeStale(spacesById.keySet());
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

    /**
     * Force a refresh of the root spaces after a specified delay, allowing for any ongoing updates to complete before the next refresh.
     *
     * @param waitMillis The number of milliseconds to wait before allowing the next refresh to occur.
     */
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

    /**
     * Get spaces by their type.
     *
     * @param type The type of spaces to retrieve.
     * @return List of Space objects matching the specified type, or an empty list if none are found.
     */
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

    /**
     * Get subspaces of a given space that match a specific type.
     *
     * @param space The space for which to find subspaces.
     * @param type  The type of subspaces to filter by.
     * @return List of subspaces matching the specified type.
     */
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
            space.setDataNeedsUpdate();
        }
    }

    /*private String getCoreInfoString(ApiResponseEntry entry) {
        return entry.get("space") + " " + entry.get("np");
    }*/

    private Space getIdSuperspace(Space space) {
        String id = space.getId();
        if (!id.matches("https?://[^/]+/.*/[^/]*/?")) return null;
        String superId = id.replaceFirst("(https?://[^/]+/.*)/[^/]*/?", "$1");
        return spacesById.get(superId);
    }

}