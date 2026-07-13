package com.knowledgepixels.nanodash;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;
import org.eclipse.rdf4j.model.Model;
import org.nanopub.extra.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A utility class for caching API responses and maps to reduce redundant API calls.
 * This class is thread-safe and ensures that cached data is refreshed periodically.
 */
public class ApiCache {

    private ApiCache() {
    } // no instances allowed

    private static final int MAX_CACHE_ENTRIES = 10_000;

    // How stale a cached response may be before the next access triggers a
    // background re-fetch. Must stay reasonably high: every render that finds a
    // query older than this submits a refresh to the shared pool, which uses a
    // CallerRunsPolicy — so too low a value turns page renders into a refresh
    // storm that can run queries synchronously on the request thread.
    private static final long REFRESH_AGE_THRESHOLD_MS = 60 * 1000;

    // How long a cached response is still served immediately (while refreshing in
    // the background) before it is treated as absent and the caller waits for a
    // fresh fetch. Acts as the stale-data fallback during API outages.
    private static final long MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000;

    // Upper bound a synchronous caller waits for an in-flight refresh started by
    // another thread when it has nothing cached yet. Without this wait the caller
    // returns null, letting repositories memoise an empty snapshot (see
    // retrieveResponseSync).
    private static final long SYNC_WAIT_FOR_INFLIGHT_MS = 10 * 1000;

    private static final Cache<String, ApiResponse> cachedResponses = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private static final Cache<String, Model> cachedRdfModels = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private transient static ConcurrentMap<String, Integer> failed = new ConcurrentHashMap<>();
    private static final Cache<String, Map<String, String>> cachedMaps = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private transient static ConcurrentMap<String, Long> lastRefresh = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> refreshStart = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> runAfter = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ApiCache.class);

    // Guava fires removal notifications also when an entry is REPLACED (every routine
    // refresh's put), and processes them lazily during later cache operations. Cleaning
    // up on a replacement would wipe the metadata of a still-cached entry — in
    // particular a missing lastRefresh timestamp used to make retrieveResponseSync
    // throw an NPE on every call until the entry expired.
    private static void cleanupMetadataOnRemoval(RemovalNotification<String, ?> n) {
        if (n.getCause() == RemovalCause.REPLACED) return;
        cleanupMetadata(n.getKey());
    }

    private static void cleanupMetadata(String cacheId) {
        lastRefresh.remove(cacheId);
        failed.remove(cacheId);
        runAfter.remove(cacheId);
    }

    /**
     * Checks if a cache refresh is currently running for the given cache ID.
     *
     * @param cacheId The unique identifier for the cache.
     * @return True if a refresh is running, false otherwise.
     */
    private static boolean isRunning(String cacheId) {
        Long start = refreshStart.get(cacheId);
        if (start == null) return false;
        return System.currentTimeMillis() - start < 60 * 1000;
    }

    /**
     * Checks if a cache refresh is currently running for the given QueryRef.
     *
     * @param queryRef The query reference
     * @return True if a refresh is running, false otherwise.
     */
    public static boolean isRunning(QueryRef queryRef) {
        return isRunning(queryRef.getAsUrlString());
    }

    /**
     * Request-scoped flag set by {@code NanodashPage} when the current request is a
     * genuine browser reload (the browser sends {@code Cache-Control: max-age=0} or
     * {@code no-cache}). When set, the first access to each query during the page
     * render evicts that query's cache so it re-fetches fresh, while normal
     * navigation, Ajax updates, and the auto-refresh redirect keep serving the
     * cache. Public so the page layer can set it.
     */
    public static final MetaDataKey<Boolean> FORCE_REFRESH_ON_RELOAD = new MetaDataKey<>() {};

    // The query cache-ids already force-evicted during the current reload request,
    // so each is evicted only once (the lazy-load that follows must not re-evict).
    private static final MetaDataKey<HashSet<String>> RELOAD_FORCED_IDS = new MetaDataKey<>() {};

    /**
     * On a genuine browser reload, returns true the first time a given query is
     * accessed this request (and records it), so callers evict its cache once.
     * Returns false on non-reload requests, off the request thread, and for any
     * query already handled this request — so it never triggers a refresh storm.
     */
    private static boolean isForcedReload(String cacheId) {
        RequestCycle rc = RequestCycle.get();
        if (rc == null) return false;
        Boolean force = rc.getMetaData(FORCE_REFRESH_ON_RELOAD);
        if (force == null || !force) return false;
        HashSet<String> handled = rc.getMetaData(RELOAD_FORCED_IDS);
        if (handled == null) {
            handled = new HashSet<>();
            rc.setMetaData(RELOAD_FORCED_IDS, handled);
        }
        return handled.add(cacheId);
    }

    /**
     * Updates the cached API response for a specific query reference.
     *
     * @param queryRef The query reference
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateResponse(QueryRef queryRef, boolean forced) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse response;
        if (forced) {
            response = QueryApiAccess.forcedGet(queryRef);
        } else {
            response = QueryApiAccess.get(queryRef);
        }
        String cacheId = queryRef.getAsUrlString();
        logger.info("Updating cached API response for {}", cacheId);
        cachedResponses.put(cacheId, response);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    public static ApiResponse retrieveResponseSync(QueryRef queryRef, boolean forced) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached API response synchronously for {}", cacheId);
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            // lastRefresh can be missing for a cached entry (racing invalidation or
            // refresh); treat that as stale rather than NPEing on the unboxing.
            Long lastRefreshTime = lastRefresh.get(cacheId);
            needsRefresh = lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if ((needsRefresh || forced) && !isRunning(cacheId)) {
            logger.info("Refreshing cache for {}", cacheId);
            refreshStart.put(cacheId, timeNow);
            try {
                Long after = runAfter.get(cacheId);
                if (after != null) {
                    while (System.currentTimeMillis() < after) {
                        Thread.sleep(100);
                    }
                    runAfter.remove(cacheId);
                }
                if (failed.get(cacheId) != null) {
                    // 1 second pause between failed attempts;
                    Thread.sleep(1000);
                }
                Thread.sleep(100 + new Random().nextLong(400));
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
            }
            try {
                ApiCache.updateResponse(queryRef, forced);
                failed.remove(cacheId);
            } catch (Exception ex) {
                logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                // Keep stale cached data if available, only invalidate if nothing was cached
                if (cachedResponses.getIfPresent(cacheId) == null) {
                    failed.merge(cacheId, 1, Integer::sum);
                }
                lastRefresh.put(cacheId, System.currentTimeMillis());
            } finally {
                refreshStart.remove(cacheId);
            }
        } else if (cachedResponses.getIfPresent(cacheId) == null && isRunning(cacheId)) {
            // Another thread is doing the first fetch of this query and we have
            // nothing cached yet. Wait for it rather than returning null: a null
            // here lets a caller (e.g. SpaceRepository) memoise an EMPTY snapshot,
            // which then poisons MaintainedResourceRepository.build() and breaks
            // the home page until the next refresh. This adds no new work; it only
            // waits on the refresh already in flight.
            try {
                long deadline = timeNow + SYNC_WAIT_FOR_INFLIGHT_MS;
                while (isRunning(cacheId)
                        && cachedResponses.getIfPresent(cacheId) == null
                        && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return cachedResponses.getIfPresent(cacheId);
    }

    /**
     * Retrieves a cached API response for a specific QueryRef.
     *
     * @param queryRef The QueryRef object containing the query name and parameters.
     * @return The cached API response, or null if not cached.
     */
    public static ApiResponse retrieveResponseAsync(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached API response asynchronously for {}", cacheId);
        if (isForcedReload(cacheId)) {
            cachedResponses.invalidate(cacheId);
            lastRefresh.remove(cacheId);
        }
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    Long after = runAfter.get(cacheId);
                    if (after != null) {
                        while (System.currentTimeMillis() < after) {
                            Thread.sleep(100);
                        }
                        runAfter.remove(cacheId);
                    }
                    if (failed.get(cacheId) != null) {
                        // 1 second pause between failed attempts;
                        Thread.sleep(1000);
                    }
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateResponse(queryRef, false);
                    failed.remove(cacheId);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    if (cachedResponses.getIfPresent(cacheId) == null) {
                        failed.merge(cacheId, 1, Integer::sum);
                    }
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedResponses.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    /**
     * Updates the cached map for a specific query reference.
     *
     * @param queryRef The query reference
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateMap(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        Map<String, String> map = new HashMap<>();
        List<ApiResponseEntry> respList = QueryApiAccess.get(queryRef).getData();
        while (respList != null && !respList.isEmpty()) {
            ApiResponseEntry resultEntry = respList.removeFirst();
            map.put(resultEntry.get("key"), resultEntry.get("value"));
        }
        String cacheId = queryRef.getAsUrlString();
        cachedMaps.put(cacheId, map);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached map for a specific query reference.
     * If the cache is stale, it triggers a background refresh.
     *
     * @param queryRef The query reference
     * @return The cached map, or null if not cached.
     */
    public static Map<String, String> retrieveMap(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        if (isForcedReload(cacheId)) {
            cachedMaps.invalidate(cacheId);
            lastRefresh.remove(cacheId);
        }
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedMaps.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    Long after = runAfter.get(cacheId);
                    if (after != null) {
                        while (System.currentTimeMillis() < after) {
                            Thread.sleep(100);
                        }
                        runAfter.remove(cacheId);
                    }
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateMap(queryRef);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    cachedMaps.invalidate(cacheId);
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                }  finally {
                    refreshStart.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedMaps.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    private static void updateRdfModel(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        final Model[] modelRef = new Model[1];
        QueryAccess qa = new QueryAccess() {
            @Override
            protected void processHeader(String[] line) {}
            @Override
            protected void processLine(String[] line) {}
            @Override
            protected void processRdfContent(Model model) {
                modelRef[0] = model;
            }
        };
        qa.call(queryRef);
        if (modelRef[0] == null) {
            throw new FailedApiCallException(new Exception("No RDF content in response for query: " + queryRef.getQueryId()));
        }
        String cacheId = queryRef.getAsUrlString();
        logger.info("Updating cached RDF model for {}", cacheId);
        cachedRdfModels.put(cacheId, modelRef[0]);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached RDF model for a CONSTRUCT query, triggering a background fetch if needed.
     *
     * @param queryRef The QueryRef for the CONSTRUCT query.
     * @return The cached RDF Model, or null if not yet available.
     */
    public static Model retrieveRdfModelAsync(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached RDF model asynchronously for {}", cacheId);
        if (isForcedReload(cacheId)) {
            cachedRdfModels.invalidate(cacheId);
            lastRefresh.remove(cacheId);
        }
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedRdfModels.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    Long after = runAfter.get(cacheId);
                    if (after != null) {
                        while (System.currentTimeMillis() < after) {
                            Thread.sleep(100);
                        }
                        runAfter.remove(cacheId);
                    }
                    if (failed.get(cacheId) != null) {
                        Thread.sleep(1000);
                    }
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh RDF cache: {}", ex.getMessage());
                }
                try {
                    updateRdfModel(queryRef);
                    failed.remove(cacheId);
                } catch (Exception ex) {
                    logger.error("Failed to update RDF cache for {}: {}", cacheId, ex.getMessage());
                    if (cachedRdfModels.getIfPresent(cacheId) == null) {
                        failed.merge(cacheId, 1, Integer::sum);
                    }
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedRdfModels.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    /**
     * Clears the cached response for a specific query reference and sets a delay before the next refresh can occur.
     *
     * @param queryRef   The query reference for which to clear the cache.
     * @param waitMillis The amount of time in milliseconds to wait before allowing the cache to be refreshed again.
     */
    public static void clearCache(QueryRef queryRef, long waitMillis) {
        if (waitMillis < 0) {
            throw new IllegalArgumentException("waitMillis must be non-negative");
        }
        cachedResponses.invalidate(queryRef.getAsUrlString());
        runAfter.put(queryRef.getAsUrlString(), System.currentTimeMillis() + waitMillis);
    }

}
