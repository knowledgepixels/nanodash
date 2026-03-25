package com.knowledgepixels.nanodash;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.rdf4j.model.Model;
import org.nanopub.extra.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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

    private static final Cache<String, ApiResponse> cachedResponses = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(n -> cleanupMetadata(n.getKey().toString()))
        .build();
    private static final Cache<String, Model> cachedRdfModels = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(n -> cleanupMetadata(n.getKey().toString()))
        .build();
    private transient static ConcurrentMap<String, Integer> failed = new ConcurrentHashMap<>();
    private static final Cache<String, Map<String, String>> cachedMaps = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(n -> cleanupMetadata(n.getKey().toString()))
        .build();
    private transient static ConcurrentMap<String, Long> lastRefresh = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> refreshStart = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> runAfter = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ApiCache.class);

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
        logger.info("Retrieving cached API response synchronously for {}", cacheId);
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (failed.get(cacheId) != null && failed.get(cacheId) > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if ((needsRefresh || forced) && !isRunning(cacheId)) {
            logger.info("Refreshing cache for {}", cacheId);
            refreshStart.put(cacheId, timeNow);
            try {
                if (runAfter.containsKey(cacheId)) {
                    while (System.currentTimeMillis() < runAfter.get(cacheId)) {
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
        logger.info("Retrieving cached API response asynchronously for {}", cacheId);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            isCached = cacheAge < 24 * 60 * 60 * 1000;
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (failed.get(cacheId) != null && failed.get(cacheId) > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    if (runAfter.containsKey(cacheId)) {
                        while (System.currentTimeMillis() < runAfter.get(cacheId)) {
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
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedMaps.getIfPresent(cacheId) != null) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            isCached = cacheAge < 24 * 60 * 60 * 1000;
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    if (runAfter.containsKey(cacheId)) {
                        while (System.currentTimeMillis() < runAfter.get(cacheId)) {
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
        logger.info("Retrieving cached RDF model asynchronously for {}", cacheId);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedRdfModels.getIfPresent(cacheId) != null) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            isCached = cacheAge < 24 * 60 * 60 * 1000;
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (failed.get(cacheId) != null && failed.get(cacheId) > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    if (runAfter.containsKey(cacheId)) {
                        while (System.currentTimeMillis() < runAfter.get(cacheId)) {
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
