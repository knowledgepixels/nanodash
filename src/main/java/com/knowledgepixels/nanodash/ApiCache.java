package com.knowledgepixels.nanodash;

import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;

import java.util.*;

/**
 * A utility class for caching API responses and maps to reduce redundant API calls.
 * This class is thread-safe and ensures that cached data is refreshed periodically.
 */
public class ApiCache {

    private ApiCache() {
    } // no instances allowed

    private transient static Map<String, ApiResponse> cachedResponses = new HashMap<>();
    private transient static Map<String, Map<String, String>> cachedMaps = new HashMap<>();
    private transient static Map<String, Long> lastRefresh = new HashMap<>();
    private transient static Map<String, Long> refreshStart = new HashMap<>();

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
     * Checks if a cache refresh is running for a specific query and parameters.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @return True if a refresh is running, false otherwise.
     */
    public static boolean isRunning(String queryName, Map<String, String> params) {
        return isRunning(getCacheId(queryName, params));
    }

    /**
     * Checks if a cache refresh is running for a specific query and a single parameter.
     *
     * @param queryName  The name of the query.
     * @param paramName  The name of the parameter.
     * @param paramValue The value of the parameter.
     * @return True if a refresh is running, false otherwise.
     */
    public static boolean isRunning(String queryName, String paramName, String paramValue) {
        Map<String, String> params = new HashMap<>();
        params.put(paramName, paramValue);
        return isRunning(getCacheId(queryName, params));
    }

    /**
     * Updates the cached API response for a specific query and parameters.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateResponse(String queryName, Map<String, String> params) throws FailedApiCallException {
        Map<String, String> nanopubParams = new HashMap<>();
        for (String k : params.keySet()) nanopubParams.put(k, params.get(k));
        ApiResponse response = QueryApiAccess.get(queryName, nanopubParams);
        String cacheId = getCacheId(queryName, params);
        cachedResponses.put(cacheId, response);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached API response for a specific query and a single parameter.
     *
     * @param queryName  The name of the query.
     * @param paramName  The name of the parameter.
     * @param paramValue The value of the parameter.
     * @return The cached API response, or null if not cached.
     */
    public static ApiResponse retrieveResponse(String queryName, String paramName, String paramValue) {
        Map<String, String> params = new HashMap<>();
        params.put(paramName, paramValue);
        return retrieveResponse(queryName, params);
    }

    /**
     * Retrieves a cached API response for a specific query and parameters.
     * If the cache is stale, it triggers a background refresh.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @return The cached API response, or null if not cached.
     */
    public static synchronized ApiResponse retrieveResponse(final String queryName, final Map<String, String> params) {
        long timeNow = System.currentTimeMillis();
        String cacheId = getCacheId(queryName, params);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.containsKey(cacheId) && cachedResponses.get(cacheId) != null) {
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
                    ex.printStackTrace();
                }
                try {
                    ApiCache.updateResponse(queryName, params);
                } catch (Exception ex) {
                    ex.printStackTrace();
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
            return cachedResponses.get(cacheId);
        } else {
            return null;
        }
    }

    /**
     * Updates the cached map for a specific query and parameters.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateMap(String queryName, Map<String, String> params) throws FailedApiCallException {
        Map<String, String> map = new HashMap<>();
        Map<String, String> nanopubParams = new HashMap<>();
        for (String k : params.keySet()) nanopubParams.put(k, params.get(k));
        List<ApiResponseEntry> respList = QueryApiAccess.get(queryName, nanopubParams).getData();
        while (respList != null && !respList.isEmpty()) {
            ApiResponseEntry resultEntry = respList.remove(0);
            map.put(resultEntry.get("key"), resultEntry.get("value"));
        }
        String cacheId = getCacheId(queryName, params);
        cachedMaps.put(cacheId, map);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached map for a specific query and parameters.
     * If the cache is stale, it triggers a background refresh.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @return The cached map, or null if not cached.
     */
    public static synchronized Map<String, String> retrieveMap(String queryName, Map<String, String> params) {
        long timeNow = System.currentTimeMillis();
        String cacheId = getCacheId(queryName, params);
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
                    ex.printStackTrace();
                }
                try {
                    ApiCache.updateMap(queryName, params);
                } catch (Exception ex) {
                    ex.printStackTrace();
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

    /**
     * Converts a map of parameters to a sorted string representation.
     *
     * @param params The map of parameters.
     * @return A string representation of the parameters.
     */
    private static String paramsToString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        String s = "";
        for (String k : keys) s += " " + k + "=" + params.get(k);
        return s;
    }

    /**
     * Generates a unique cache ID for a specific query and parameters.
     *
     * @param queryName The name of the query.
     * @param params    The parameters for the query.
     * @return The unique cache ID.
     */
    public static String getCacheId(String queryName, Map<String, String> params) {
        return queryName + " " + paramsToString(params);
    }
}
