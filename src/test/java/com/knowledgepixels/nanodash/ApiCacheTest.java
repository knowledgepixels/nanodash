package com.knowledgepixels.nanodash;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryRef;

import com.google.common.cache.Cache;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiCacheTest {

    @Mock
    private QueryRef mockQueryRef;
    private final String MOCK_CACHE_ID = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";

    @BeforeEach
    void setUp() throws Exception {
        resetMap("cachedResponses");
        resetMap("cachedMaps");
        resetMap("failed");
        resetMap("lastRefresh");
        resetMap("refreshStart");
        resetMap("runAfter");
        resetMap("forcedRefresh");

        lenient().when(mockQueryRef.getAsUrlString()).thenReturn(MOCK_CACHE_ID);
    }

    private void resetMap(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object obj = f.get(null);
        if (obj instanceof Cache<?, ?> cache) {
            cache.invalidateAll();
        } else if (obj instanceof ConcurrentMap<?, ?> map) {
            map.clear();
        } else if (obj instanceof Set<?> set) {
            set.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getSet(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (Set<String>) f.get(null);
    }

    @SuppressWarnings("unchecked")
    private <K, V> ConcurrentMap<K, V> getMap(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object obj = f.get(null);
        if (obj instanceof Cache<?, ?> cache) {
            return (ConcurrentMap<K, V>) cache.asMap();
        }
        return (ConcurrentMap<K, V>) obj;
    }

    private void putCachedResponse(ApiResponse response, long ageMillis) throws Exception {
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        ConcurrentMap<String, Long> lastRefresh = getMap("lastRefresh");
        cachedResponses.put(MOCK_CACHE_ID, response);
        lastRefresh.put(MOCK_CACHE_ID, System.currentTimeMillis() - ageMillis);
    }

    private void putFailed(int count) throws Exception {
        ConcurrentMap<String, Integer> failed = getMap("failed");
        failed.put(MOCK_CACHE_ID, count);
    }

    @Test
    @DisplayName("retrieveResponseSync should use forcedGet when forced flag is true")
    void retrieveResponseSync_usesForcedGetWhenForcedFlagIsTrue() {
        ApiResponse response = mock(ApiResponse.class);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.forcedGet(mockQueryRef)).thenReturn(response);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, true);

            assertSame(response, result);
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
            queryApiAccess.verify(() -> QueryApiAccess.forcedGet(mockQueryRef), times(1));
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should return fresh cached response without API call")
    void retrieveResponseSync_returnsFreshCachedResponseWithoutApiCall() throws Exception {
        ApiResponse expected = mock(ApiResponse.class);
        putCachedResponse(expected, 5000L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(expected, result);
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
            queryApiAccess.verify(() -> QueryApiAccess.forcedGet(any()), never());
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should serve an outdated response and leave the re-fetch to the background")
    void retrieveResponseSync_servesOutdatedResponse() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        // Well past the refresh threshold, as e.g. every entry restored from the
        // persisted snapshot after a restart is.
        putCachedResponse(stale, 90000L);
        // A refresh is already in flight, so nothing new is submitted while the test runs.
        getMap("refreshStart").put(MOCK_CACHE_ID, System.currentTimeMillis());

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            long start = System.currentTimeMillis();
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            long elapsed = System.currentTimeMillis() - start;

            assertSame(stale, result, "the outdated response should be served straight away");
            assertTrue(elapsed < 1000, "should not have re-fetched inline, but took " + elapsed + "ms");
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should fetch and cache response when no cache exists")
    void retrieveResponseSync_fetchesAndCachesWhenNoCacheExists() {
        ApiResponse expected = mock(ApiResponse.class);
        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(expected);
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            assertSame(expected, result);
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should return null and record failure when API call fails")
    void retrieveResponseSync_returnsNullWhenApiCallFails() throws Exception {
        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenThrow(new FailedApiCallException(new Exception("API call failed")));

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            assertNull(result);
            ConcurrentMap<String, Integer> failed = getMap("failed");
            assertEquals(1, failed.get(MOCK_CACHE_ID));
        }
    }

    @Test
    @DisplayName("retrieveMap should keep the cached map when the refresh fails")
    void retrieveMap_keepsCachedMapWhenApiCallFails() throws Exception {
        ConcurrentMap<String, Map<String, String>> cachedMaps = getMap("cachedMaps");
        ConcurrentMap<String, Long> lastRefresh = getMap("lastRefresh");
        Map<String, String> cached = Map.of("key", "value");
        cachedMaps.put(MOCK_CACHE_ID, cached);
        // Outdated enough to trigger a refresh, but well within the maximum cache age.
        lastRefresh.put(MOCK_CACHE_ID, System.currentTimeMillis() - 2 * 60 * 1000);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class);
             MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef))
                    .thenThrow(new FailedApiCallException(new Exception("API call failed")));
            // Run the refresh job on this thread, where the mocked API access applies.
            threadPool.when(() -> NanodashThreadPool.submit(any(Runnable.class))).thenAnswer(inv -> {
                inv.getArgument(0, Runnable.class).run();
                return null;
            });

            Map<String, String> result = ApiCache.retrieveMap(mockQueryRef);

            assertSame(cached, result);
            assertSame(cached, cachedMaps.get(MOCK_CACHE_ID),
                    "a failed refresh must not throw away the cached map");
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should throw RuntimeException after three consecutive failures and the failed counter should be cleared so subsequent calls can retry")
    void retrieveResponseSync_throwsRuntimeExceptionAfterThreeFailures() throws Exception {
        putFailed(3);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            assertThrows(RuntimeException.class, () -> ApiCache.retrieveResponseSync(mockQueryRef, false));

            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
            queryApiAccess.verify(() -> QueryApiAccess.forcedGet(any()), never());
            ConcurrentMap<String, Integer> failed = getMap("failed");
            assertFalse(failed.containsKey(MOCK_CACHE_ID));
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should skip refresh when a refresh is already running for the same cache ID")
    void retrieveResponseSync_skipsRefreshWhenAlreadyRunning() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 90000L);

        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, System.currentTimeMillis());

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(cached, result);
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
        }
    }

    @Test
    @DisplayName("clearCache should mark the cached response as outdated, keeping it for the stale-content display")
    void clearCacheMarksCachedResponseAsOutdated() throws Exception {
        ApiResponse mockResponse = mock(ApiResponse.class);
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        cachedResponses.put(MOCK_CACHE_ID, mockResponse);

        ApiCache.clearCache(mockQueryRef, 1000L);

        assertTrue(getSet("forcedRefresh").contains(MOCK_CACHE_ID));
        assertSame(mockResponse, ApiCache.retrieveStaleResponse(mockQueryRef));
    }

    @Test
    @DisplayName("retrieveStaleResponse should return the cached response however outdated, and null when nothing is cached")
    void retrieveStaleResponseReturnsAnyCachedResponse() throws Exception {
        assertNull(ApiCache.retrieveStaleResponse(mockQueryRef));

        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 48 * 60 * 60 * 1000L);

        assertSame(cached, ApiCache.retrieveStaleResponse(mockQueryRef));
    }

    @Test
    @DisplayName("retrieveResponseSync should re-fetch a cache marked by clearCache instead of serving the kept response")
    void retrieveResponseSync_refreshesResponseMarkedByClearCache() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        // Young enough to be served without the marking.
        putCachedResponse(stale, 5000L);
        ApiCache.clearCache(mockQueryRef, 0L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(fresh, result);
            assertFalse(getSet("forcedRefresh").contains(MOCK_CACHE_ID), "the marking should be gone once the refresh has run");
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should not wait out an ingest delay on a request thread")
    void retrieveResponseSync_doesNotWaitOutIngestDelayOnRequestThread() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 90000L);
        // A refresh is already in flight, so nothing new is submitted while the test runs.
        getMap("refreshStart").put(MOCK_CACHE_ID, System.currentTimeMillis());
        // ...and a publication has just asked for a long pause before the next fetch.
        ApiCache.clearCache(mockQueryRef, 60000L);

        // A WicketTester binds a request cycle to this thread, which is what marks it as a
        // thread serving a user rather than a background one.
        WicketTester tester = new WicketTester();
        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            long start = System.currentTimeMillis();
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            long elapsed = System.currentTimeMillis() - start;

            assertSame(cached, result, "the outdated response should be served straight away");
            assertTrue(elapsed < 1000, "should not have waited for the ingest delay, but took " + elapsed + "ms");
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
        } finally {
            tester.destroy();
            ThreadContext.detach();
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should still re-fetch a clearCache-marked entry on a request thread")
    void retrieveResponseSync_stillRefreshesMarkedEntryOnRequestThread() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 90000L);
        ApiCache.clearCache(mockQueryRef, 0L);

        WicketTester tester = new WicketTester();
        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            // The marking means the kept entry must not be handed out as current, so the
            // request thread does wait for the re-fetch here.
            assertSame(fresh, result);
        } finally {
            tester.destroy();
            ThreadContext.detach();
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should drop the clearCache marking even when the refresh fails")
    void retrieveResponseSync_dropsMarkingWhenRefreshFails() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        ApiCache.clearCache(mockQueryRef, 0L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenThrow(new FailedApiCallException(new Exception("API call failed")));

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            // The kept response is the outage fallback, but the marking must not survive:
            // otherwise every later call would re-run the failing query.
            assertSame(stale, result);
            assertFalse(getSet("forcedRefresh").contains(MOCK_CACHE_ID));
        }
    }

    @Test
    @DisplayName("clearCache should handle zero wait time")
    void clearCacheWithZeroWaitTime() throws Exception {
        long waitMillis = 0L;
        long beforeCall = System.currentTimeMillis();

        ApiCache.clearCache(mockQueryRef, waitMillis);
        long afterCall = System.currentTimeMillis();

        ConcurrentMap<String, Long> runAfter = getMap("runAfter");
        assertTrue(runAfter.containsKey(MOCK_CACHE_ID));
        Long runAfterTime = runAfter.get(MOCK_CACHE_ID);
        assertNotNull(runAfterTime);

        assertTrue(runAfterTime >= beforeCall && runAfterTime <= afterCall);
    }

    @Test
    @DisplayName("clearCache should work when cache is empty")
    void clearCacheWhenCacheIsEmpty() throws Exception {
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        ConcurrentMap<String, Long> runAfter = getMap("runAfter");
        assertFalse(cachedResponses.containsKey(MOCK_CACHE_ID));
        assertDoesNotThrow(() -> ApiCache.clearCache(mockQueryRef, 1000L));
        assertTrue(runAfter.containsKey(MOCK_CACHE_ID));
    }

    @Test
    @DisplayName("clearCache should update runAfter on subsequent calls")
    void clearCacheUpdatesRunAfterOnSubsequentCalls() throws Exception {
        ApiCache.clearCache(mockQueryRef, 1000L);
        ConcurrentMap<String, Long> runAfter = getMap("runAfter");
        Long firstRunAfter = runAfter.get(MOCK_CACHE_ID);
        assertNotNull(firstRunAfter);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        ApiCache.clearCache(mockQueryRef, 2000L);
        Long secondRunAfter = runAfter.get(MOCK_CACHE_ID);

        assertNotNull(secondRunAfter);
        assertTrue(secondRunAfter > firstRunAfter);
    }

    @Test
    @DisplayName("clearCache should throw IllegalArgumentException for negative wait time")
    void clearCacheWithNegativeWaitTime() throws Exception {
        long waitMillis = -1000L;

        assertThrows(IllegalArgumentException.class, () -> ApiCache.clearCache(mockQueryRef, waitMillis));
        ConcurrentMap<String, Long> runAfter = getMap("runAfter");
        assertFalse(runAfter.containsKey(MOCK_CACHE_ID));
    }

    @Test
    @DisplayName("isRunning should return false when refreshStart does not contain the cache ID")
    void isRunningWhenCacheIdNotInRefreshStart() throws Exception {
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        assertFalse(refreshStart.containsKey(MOCK_CACHE_ID));
        boolean result = ApiCache.isRunning(mockQueryRef);
        assertFalse(result);
    }

    @Test
    @DisplayName("isRunning should return true when refresh started less than 60 seconds ago")
    void isRunningWhenRefreshIsRecent() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 30000);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertTrue(result);
    }

    @Test
    @DisplayName("isRunning should return true when refresh started exactly at current time")
    void isRunningWhenRefreshJustStarted() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertTrue(result);
    }

    @Test
    @DisplayName("isRunning should return false when refresh started exactly 60 seconds ago")
    void isRunningExactlyAtThreshold() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 60 * 1000);

        Thread.sleep(10);

        assertFalse(ApiCache.isRunning(mockQueryRef));
    }

    @Test
    @DisplayName("isRunning should return false when refresh started more than 60 seconds ago")
    void isRunningWhenRefreshIsOld() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 120000);

        assertFalse(ApiCache.isRunning(mockQueryRef));
    }

    @Test
    @DisplayName("isRunning should be consistent across multiple calls within timeout window")
    void isRunningConsistentAcrossMultipleCalls() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 10000); // 10 seconds ago

        boolean result1 = ApiCache.isRunning(mockQueryRef);
        boolean result2 = ApiCache.isRunning(mockQueryRef);
        boolean result3 = ApiCache.isRunning(mockQueryRef);

        assertTrue(result1, "First call should return true");
        assertTrue(result2, "Second call should return true");
        assertTrue(result3, "Third call should return true");
    }

    @Test
    @DisplayName("isRunning should handle null QueryRef gracefully by throwing exception")
    void isRunningWithNullQueryRef() {
        assertThrows(NullPointerException.class, () -> ApiCache.isRunning(null));
    }

    @Test
    @DisplayName("isRunning should be thread-safe for concurrent access")
    void isRunningThreadSafety() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 30000); // 30 seconds ago

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> results[index] = ApiCache.isRunning(mockQueryRef));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i]);
        }
    }

}