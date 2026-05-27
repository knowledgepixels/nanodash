package com.knowledgepixels.nanodash.repository;

import com.github.jsonldjava.shaded.com.google.common.collect.Ordering;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.SpaceFactory;
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

    private volatile List<Space> spaceList;
    private Map<String, List<Space>> spaceListByType;
    private Map<String, Space> spacesById;
    private Map<String, Space> spacesByAltId;
    private Map<Space, Set<Space>> subspaceMap;
    private Map<Space, Set<Space>> superspaceMap;
    private boolean loaded = false;
    private volatile Long runRootUpdateAfter = null;
    private volatile long lastRefreshTime = 0;

    private final Object loadLock = new Object();

    private SpaceRepository() {
    }

    /**
     * Refresh the list of spaces from the spaces repo. Pulls the latest
     * non-invalidated SpaceDefinition per Space IRI from {@code npa:spacesGraph}
     * and joins to the declaring nanopub for label and type.
     * <p>
     * The {@code get-spaces} query returns one row per (SpaceRef, SpaceDefinition)
     * — many spaces have multiple contributing nanopubs (root + updates) and even
     * multiple SpaceRefs during the rootless transition phase. The query orders by
     * {@code DESC(?date)}, so dedup'ing by spaceIri here (first row wins) picks the
     * latest update per space.
     */
    public synchronized void refresh() {
        List<Space> newSpaceList = new ArrayList<>();
        Map<String, List<Space>> newSpaceListByType = new HashMap<>();
        Map<String, Space> newSpacesById = new HashMap<>();
        Map<String, Space> newSpacesByAltId = new HashMap<>();
        Map<Space, Set<Space>> newSubspaceMap = new HashMap<>();
        Map<Space, Set<Space>> newSuperspaceMap = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), true).getData()) {
            String spaceIri = r.get("spaceIri");
            if (spaceIri == null || spaceIri.isEmpty()) continue;
            if (!seen.add(spaceIri)) continue; // first row (latest date) wins
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("space", spaceIri);
            entry.add("np", r.get("np"));
            entry.add("label", r.get("label"));
            entry.add("type", r.get("type"));
            Space space = SpaceFactory.getOrCreate(entry);
            newSpaceList.add(space);
            newSpaceListByType.computeIfAbsent(space.getType(), k -> new ArrayList<>()).add(space);
            newSpacesById.put(space.getId(), space);
            for (String altId : space.getAltIDs()) {
                newSpacesByAltId.put(altId, space);
            }
        }
        Comparator<Space> byLabel = Comparator.comparing(Space::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        for (List<Space> spacesOfType : newSpaceListByType.values()) {
            spacesOfType.sort(byLabel);
        }
        logger.info("Refreshed spaces from spaces repo: {} distinct spaces", newSpaceList.size());
        SpaceFactory.removeStale(newSpacesById.keySet());
        populateSubspaceRelations(newSpacesById, newSubspaceMap, newSuperspaceMap);
        for (Space space : newSpaceList) {
            space.setDataNeedsUpdate();
        }
        spacesById = newSpacesById;
        spacesByAltId = newSpacesByAltId;
        spaceListByType = newSpaceListByType;
        subspaceMap = newSubspaceMap;
        superspaceMap = newSuperspaceMap;
        loaded = true;
        lastRefreshTime = System.currentTimeMillis();
        spaceList = newSpaceList; // volatile write last — establishes happens-before for all above
    }

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
                synchronized (loadLock) {
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
            if (spaceList == null) { // double-check after potential wait
                refresh();
            }
        } else if (System.currentTimeMillis() - lastRefreshTime > 60_000) {
            refresh();
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
     * Get a space by one of its alternative IDs.
     *
     * @param altId The alternative ID of the space.
     * @return The corresponding Space object, or null if not found.
     */
    public Space findByAltId(String altId) {
        ensureLoaded();
        return spacesByAltId.get(altId);
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
     * Refresh the spaces and mark each as needing a downstream data update.
     */
    public void refreshAndInvalidate() {
        refresh();
        for (Space space : spaceList) {
            space.setDataNeedsUpdate();
        }
    }

    private static void populateSubspaceRelations(
            Map<String, Space> spacesById,
            Map<Space, Set<Space>> subspaceMap,
            Map<Space, Set<Space>> superspaceMap) {
        for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SUB_SPACE_LINKS), true).getData()) {
            Space child = spacesById.get(r.get("child"));
            Space parent = spacesById.get(r.get("parent"));
            if (child == null || parent == null) continue;
            subspaceMap.computeIfAbsent(parent, k -> new HashSet<>()).add(child);
            superspaceMap.computeIfAbsent(child, k -> new HashSet<>()).add(parent);
        }
    }

}