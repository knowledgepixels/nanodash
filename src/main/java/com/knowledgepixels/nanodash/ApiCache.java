package com.knowledgepixels.nanodash;

import org.nanopub.extra.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A utility class for caching API responses and maps to reduce redundant API calls.
 * This class is thread-safe and ensures that cached data is refreshed periodically.
 */
public class ApiCache {

    private ApiCache() {
    } // no instances allowed

    private transient static ConcurrentMap<String, ApiResponse> cachedResponses = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Boolean> failed = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Map<String, String>> cachedMaps = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> lastRefresh = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> refreshStart = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ApiCache.class);

    /**
     * Checks if a cache refresh is currently running for the given cache ID.
     *
     * @param cacheId The unique identifier for the cache.
     * @return True if a refresh is running, false otherwise.
     */
    private static boolean isRunning(String cacheId) {
        if (!refreshStart.containsKey(cacheId)) return false;
        return System.currentTimeMillis() - refreshStart.get(cacheId) < 60 * 1000;
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
    private static void updateResponse(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse response = QueryApiAccess.get(queryRef);
        String cacheId = queryRef.getAsUrlString();
        cachedResponses.put(cacheId, response);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached API response for a specific QueryRef.
     *
     * @param queryRef The QueryRef object containing the query name and parameters.
     * @return The cached API response, or null if not cached.
     */
    public static ApiResponse retrieveResponse(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.containsKey(cacheId) && cachedResponses.get(cacheId) != null) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            isCached = cacheAge < 24 * 60 * 60 * 1000;
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (failed.get(cacheId) != null) {
            cachedResponses.remove(cacheId);
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            refreshStart.put(cacheId, timeNow);
            new Thread(() -> {
                try {
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateResponse(queryRef);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    cachedResponses.remove(cacheId);
                    failed.put(cacheId, true);
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                }
            }).start();
        }
        if (isCached) {
            return cachedResponses.get(cacheId);
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
            ApiResponseEntry resultEntry = respList.remove(0);
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
    public static synchronized Map<String, String> retrieveMap(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedMaps.containsKey(cacheId)) {
            long cacheAge = timeNow - lastRefresh.get(cacheId);
            isCached = cacheAge < 24 * 60 * 60 * 1000;
            needsRefresh = cacheAge > 60 * 1000;
        }
        if (needsRefresh && !isRunning(cacheId)) {
            refreshStart.put(cacheId, timeNow);
            new Thread(() -> {
                try {
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateMap(queryRef);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    cachedResponses.put(cacheId, null);
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                }
            }).start();
        }
        if (isCached) {
            if (cachedResponses.get(cacheId) == null) {
                cachedResponses.remove(cacheId);
                throw new RuntimeException("Query failed: " + cacheId);
            }
            return cachedMaps.get(cacheId);
        } else {
            return null;
        }
    }

}
