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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private SpaceRepository() {
    }

    private static final class Snapshot {
        final Map<String, Space> spacesById;
        final Map<String, Space> spacesByAltId;
        final Map<String, List<Space>> spaceListByType;
        final Map<Space, Set<Space>> subspaceMap;
        final Map<Space, Set<Space>> superspaceMap;

        Snapshot(Map<String, Space> byId,
                 Map<String, Space> byAltId,
                 Map<String, List<Space>> byType,
                 Map<Space, Set<Space>> subspaces,
                 Map<Space, Set<Space>> superspaces) {
            this.spacesById = byId;
            this.spacesByAltId = byAltId;
            this.spaceListByType = byType;
            this.subspaceMap = subspaces;
            this.superspaceMap = superspaces;
        }

        static final Snapshot EMPTY = new Snapshot(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap());
    }

    // Source of truth is ApiCache (60s TTL on its own); we memoise the derived
    // maps keyed by the ApiResponse instance identity for get-spaces, and
    // re-resolve the sub-space link response on each rebuild.
    private volatile ApiResponse cachedFor;
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    private Snapshot current() {
        ApiResponse resp = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), false);
        if (resp == null) {
            return snapshot;
        }
        if (resp == cachedFor) {
            return snapshot;
        }
        synchronized (this) {
            if (resp == cachedFor) {
                return snapshot;
            }
            Snapshot built = build(resp);
            snapshot = built;
            cachedFor = resp;
            return built;
        }
    }

    /**
     * Build the space lookup maps from the spaces-repo response. Pulls the
     * latest non-invalidated SpaceDefinition per Space IRI from
     * {@code npa:spacesGraph} and joins to the declaring nanopub for label and
     * type.
     * <p>
     * The {@code get-spaces} query returns one row per (SpaceRef, SpaceDefinition)
     * — many spaces have multiple contributing nanopubs (root + updates) and even
     * multiple SpaceRefs during the rootless transition phase. The query orders by
     * {@code DESC(?date)}, so dedup'ing by spaceIri here (first row wins) picks the
     * latest update per space.
     */
    private Snapshot build(ApiResponse resp) {
        Map<String, Space> byId = new HashMap<>();
        Map<String, Space> byAltId = new HashMap<>();
        Map<String, List<Space>> byType = new HashMap<>();
        Map<Space, Set<Space>> subspaceMap = new HashMap<>();
        Map<Space, Set<Space>> superspaceMap = new HashMap<>();
        List<Space> spaceList = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ApiResponseEntry r : resp.getData()) {
            String spaceIri = r.get("spaceIri");
            if (spaceIri == null || spaceIri.isEmpty()) continue;
            if (!seen.add(spaceIri)) continue; // first row (latest date) wins
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("space", spaceIri);
            entry.add("np", r.get("np"));
            entry.add("label", r.get("label"));
            entry.add("type", r.get("type"));
            Space space = SpaceFactory.getOrCreate(entry);
            spaceList.add(space);
            byType.computeIfAbsent(space.getType(), k -> new ArrayList<>()).add(space);
            byId.put(space.getId(), space);
            for (String altId : space.getAltIDs()) {
                byAltId.put(altId, space);
            }
        }
        Comparator<Space> byLabel = Comparator.comparing(Space::getLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        for (List<Space> spacesOfType : byType.values()) {
            spacesOfType.sort(byLabel);
        }
        logger.info("Refreshed spaces from spaces repo: {} distinct spaces", spaceList.size());
        SpaceFactory.removeStale(byId.keySet());
        populateSubspaceRelations(byId, subspaceMap, superspaceMap);
        // Mark each space's per-space detail data stale; the upstream spaces
        // listing has refreshed, so members/admins/roles should be re-fetched
        // on next access.
        for (Space space : spaceList) {
            space.setDataNeedsUpdate();
        }
        return new Snapshot(byId, byAltId, byType, subspaceMap, superspaceMap);
    }

    /**
     * Invalidate the underlying ApiCache entries, optionally delaying the next refresh.
     *
     * @param waitMillis Delay in milliseconds before the next access may trigger a refresh; 0 for immediate.
     */
    public void forceRootRefresh(long waitMillis) {
        ApiCache.clearCache(new QueryRef(QueryApiAccess.GET_SPACES), waitMillis);
        ApiCache.clearCache(new QueryRef(QueryApiAccess.GET_SUB_SPACE_LINKS), waitMillis);
    }

    /**
     * Get a space by its id.
     *
     * @param id The id of the space.
     * @return The corresponding Space object, or null if not found.
     */
    public Space findById(String id) {
        return current().spacesById.get(id);
    }

    /**
     * Get a space by one of its alternative IDs.
     *
     * @param altId The alternative ID of the space.
     * @return The corresponding Space object, or null if not found.
     */
    public Space findByAltId(String altId) {
        return current().spacesByAltId.get(altId);
    }

    /**
     * Get spaces by their type.
     *
     * @param type The type of spaces to retrieve.
     * @return List of Space objects matching the specified type, or an empty list if none are found.
     */
    public List<Space> findByType(String type) {
        List<Space> l = current().spaceListByType.get(type);
        return l != null ? l : new ArrayList<>();
    }

    /**
     * Get subspaces of a given space.
     *
     * @param space The space for which to find subspaces.
     * @return List of subspaces.
     */
    public List<Space> findSubspaces(Space space) {
        Set<Space> subspaces = current().subspaceMap.get(space);
        if (subspaces == null) return new ArrayList<>();
        List<Space> sorted = new ArrayList<>(subspaces);
        sorted.sort(Ordering.usingToString());
        return sorted;
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
        Set<Space> superspaces = current().superspaceMap.get(space);
        if (superspaces == null) return new ArrayList<>();
        List<Space> sorted = new ArrayList<>(superspaces);
        sorted.sort(Ordering.usingToString());
        return sorted;
    }

    private static void populateSubspaceRelations(
            Map<String, Space> spacesById,
            Map<Space, Set<Space>> subspaceMap,
            Map<Space, Set<Space>> superspaceMap) {
        ApiResponse resp = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SUB_SPACE_LINKS), false);
        if (resp == null) return;
        for (ApiResponseEntry r : resp.getData()) {
            Space child = spacesById.get(r.get("child"));
            Space parent = spacesById.get(r.get("parent"));
            if (child == null || parent == null) continue;
            subspaceMap.computeIfAbsent(parent, k -> new HashSet<>()).add(child);
            superspaceMap.computeIfAbsent(child, k -> new HashSet<>()).add(parent);
        }
    }

}
