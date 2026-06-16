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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        final Map<Space, Set<Space>> subspaceMap;
        final Map<Space, Set<Space>> superspaceMap;

        Snapshot(Map<String, Space> byId,
                 Map<String, Space> byAltId,
                 Map<Space, Set<Space>> subspaces,
                 Map<Space, Set<Space>> superspaces) {
            this.spacesById = byId;
            this.spacesByAltId = byAltId;
            this.subspaceMap = subspaces;
            this.superspaceMap = superspaces;
        }

        static final Snapshot EMPTY = new Snapshot(
                Collections.emptyMap(), Collections.emptyMap(),
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
     * Reduce the {@code get-spaces} rows to one representative entry per space IRI and
     * collect the distinct ref roots seen for each IRI. Rows are expected in
     * {@code DESC(?date)} order.
     * <p>
     * The ref-aware query (v3) returns {@code ?ref} (the space ref = space IRI + root
     * definition) and {@code ?root} (the ref's root nanopub) per (ref, definition) row.
     * We dedup by ref (latest definition within a ref wins), then keep one representative
     * per IRI — the globally latest definition, preserving the pre-ref-aware behaviour —
     * and return the full set of ref roots per IRI for the (deferred) multi-ref
     * disambiguation UI. See docs/space-ref-identity.md.
     * <p>
     * When rows carry no {@code ?ref} (the pre-v3 query, still live until v3 is
     * published) this degrades to dedup-by-IRI exactly as before, with empty ref-root
     * sets.
     */
    static RefReduction reduceByRef(List<ApiResponseEntry> rows) {
        Set<String> seenRef = new HashSet<>();
        Map<String, Set<String>> refRootsByIri = new LinkedHashMap<>();
        Map<String, ApiResponseEntry> representativeByIri = new LinkedHashMap<>();
        for (ApiResponseEntry r : rows) {
            String spaceIri = r.get("space_iri");
            if (spaceIri == null || spaceIri.isEmpty()) continue;
            String ref = r.get("ref");
            // Dedup by ref when available, else by IRI (pre-v3 query).
            String refKey = (ref != null && !ref.isEmpty()) ? ref : spaceIri;
            if (!seenRef.add(refKey)) continue;
            String root = r.get("root");
            if (root != null && !root.isEmpty()) {
                refRootsByIri.computeIfAbsent(spaceIri, k -> new LinkedHashSet<>()).add(root);
            }
            // First surviving row per IRI = latest definition overall (rows are DESC(date)).
            representativeByIri.putIfAbsent(spaceIri, r);
        }
        return new RefReduction(new ArrayList<>(representativeByIri.values()), refRootsByIri);
    }

    /** Result of {@link #reduceByRef}: one representative row per IRI plus the ref roots per IRI. */
    static final class RefReduction {
        final List<ApiResponseEntry> representatives;
        final Map<String, Set<String>> refRootsByIri;

        RefReduction(List<ApiResponseEntry> representatives, Map<String, Set<String>> refRootsByIri) {
            this.representatives = representatives;
            this.refRootsByIri = refRootsByIri;
        }
    }

    /**
     * Build the space lookup maps from the spaces-repo response. Pulls the
     * latest non-invalidated SpaceDefinition per space ref from
     * {@code npa:spacesGraph} and joins to the declaring nanopub for label and
     * type, then keeps one representative space per IRI (see {@link #reduceByRef}).
     * Each space additionally carries the set of ref roots claiming its IRI for the
     * (deferred) multi-ref disambiguation UI.
     */
    private Snapshot build(ApiResponse resp) {
        Map<String, Space> byId = new HashMap<>();
        Map<String, Space> byAltId = new HashMap<>();
        Map<Space, Set<Space>> subspaceMap = new HashMap<>();
        Map<Space, Set<Space>> superspaceMap = new HashMap<>();
        List<Space> spaceList = new ArrayList<>();
        RefReduction reduction = reduceByRef(resp.getData());
        for (ApiResponseEntry r : reduction.representatives) {
            String spaceIri = r.get("space_iri");
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("space", spaceIri);
            entry.add("np", r.get("np"));
            entry.add("label", r.get("space_iri_label"));
            entry.add("type", r.get("type"));
            String refRoot = r.get("root");
            if (refRoot != null && !refRoot.isEmpty()) entry.add("ref_root", refRoot);
            Space space = SpaceFactory.getOrCreate(entry);
            space.setRefRoots(reduction.refRootsByIri.getOrDefault(spaceIri, Collections.emptySet()));
            spaceList.add(space);
            byId.put(space.getId(), space);
            for (String altId : space.getAltIDs()) {
                byAltId.put(altId, space);
            }
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
        return new Snapshot(byId, byAltId, subspaceMap, superspaceMap);
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
