package com.knowledgepixels.nanodash.repository;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.MaintainedResourceFactory;
import com.knowledgepixels.nanodash.domain.Space;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaintainedResourceRepository {

    private static final MaintainedResourceRepository INSTANCE = new MaintainedResourceRepository();

    /**
     * Get the singleton instance of MaintainedResourceRepository.
     *
     * @return The singleton instance of MaintainedResourceRepository.
     */
    public static MaintainedResourceRepository get() {
        return INSTANCE;
    }

    private MaintainedResourceRepository() {
    }

    private static final class Snapshot {
        final Map<String, MaintainedResource> resourcesById;
        final Map<String, MaintainedResource> resourcesByNamespace;
        final Map<Space, List<MaintainedResource>> resourcesBySpace;

        Snapshot(Map<String, MaintainedResource> byId,
                 Map<String, MaintainedResource> byNs,
                 Map<Space, List<MaintainedResource>> bySpace) {
            this.resourcesById = byId;
            this.resourcesByNamespace = byNs;
            this.resourcesBySpace = bySpace;
        }

        static final Snapshot EMPTY = new Snapshot(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    // Source of truth is ApiCache (60s TTL on its own); we memoise the derived
    // maps keyed by the ApiResponse instance identity so they get rebuilt
    // exactly when ApiCache returns a fresh response and not on every call.
    private volatile ApiResponse cachedFor;
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    private Snapshot current() {
        ApiResponse resp = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES), false);
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
     * Build the resource lookup maps from the spaces-repo response. Pulls
     * server-validated {@code npa:isMaintainedBy} links from the current
     * space-state graph; only the most recent declaration per resource is kept
     * (the {@code get-maintained-resources} query orders by {@code DESC(?date)},
     * so the first row per resource wins).
     */
    private Snapshot build(ApiResponse resp) {
        Map<String, MaintainedResource> byId = new HashMap<>();
        Map<String, MaintainedResource> byNamespace = new HashMap<>();
        Map<Space, List<MaintainedResource>> bySpace = new HashMap<>();
        Set<String> seenResources = new HashSet<>();
        for (ApiResponseEntry r : resp.getData()) {
            String resourceId = r.get("resource");
            if (resourceId == null || resourceId.isEmpty()) continue;
            if (!seenResources.add(resourceId)) continue; // first row (newest date) wins
            String spaceId = r.get("space");
            Space space = SpaceRepository.get().findById(spaceId);
            if (space == null) continue;
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("resource", resourceId);
            entry.add("np", r.get("np"));
            String label = r.get("label");
            if (label != null && !label.isEmpty()) entry.add("label", label);
            String namespace = r.get("namespace");
            if (namespace != null && !namespace.isEmpty()) entry.add("namespace", namespace);
            MaintainedResource resource = MaintainedResourceFactory.getOrCreate(entry, space);
            byId.put(resourceId, resource);
            bySpace.computeIfAbsent(space, k -> new ArrayList<>()).add(resource);
            if (resource.getNamespace() != null) {
                // TODO Handle conflicts when two resources claim the same namespace:
                byNamespace.put(resource.getNamespace(), resource);
            }
        }
        MaintainedResourceFactory.removeStale(byId.keySet());
        return new Snapshot(byId, byNamespace, bySpace);
    }

    /**
     * Find a maintained resource by its namespace.
     *
     * @param namespace The namespace to search for.
     * @return The MaintainedResource with the given namespace, or null if not found.
     */
    public MaintainedResource findByNamespace(String namespace) {
        return current().resourcesByNamespace.get(namespace);
    }

    /**
     * Find the maintained resources belonging to a given space.
     *
     * @param space The space to look up.
     * @return The list of maintained resources for the space, possibly empty.
     */
    public List<MaintainedResource> findResourcesBySpace(Space space) {
        List<MaintainedResource> l = current().resourcesBySpace.get(space);
        return l != null ? l : new ArrayList<>();
    }

    /**
     * Get a maintained resource by its id.
     *
     * @param id The id of the resource.
     * @return The corresponding MaintainedResource object, or null if not found.
     */
    public MaintainedResource findById(String id) {
        return current().resourcesById.get(id);
    }

    /**
     * Invalidate the underlying ApiCache entry, optionally delaying the next refresh.
     *
     * @param waitMillis Delay in milliseconds before the next access may trigger a refresh; 0 for immediate.
     */
    public void forceRootRefresh(long waitMillis) {
        ApiCache.clearCache(new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES), waitMillis);
    }

}
