package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiCacheTest {

    @Mock
    private QueryRef mockQueryRef;
    private final String MOCK_CACHE_ID = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        resetMap("cachedResponses");
        resetMap("cachedMaps");
        resetMap("failed");
        resetMap("lastRefresh");
        resetMap("refreshStart");
        resetMap("runAfter");

        mocks = MockitoAnnotations.openMocks(this);
        when(mockQueryRef.getAsUrlString()).thenReturn(MOCK_CACHE_ID);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    private void resetMap(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        ConcurrentMap<?, ?> map = (ConcurrentMap<?, ?>) f.get(null);
        map.clear();
    }

    @SuppressWarnings("unchecked")
    private <K, V> ConcurrentMap<K, V> getMap(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (ConcurrentMap<K, V>) f.get(null);
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
    @DisplayName("retrieveResponseSync should refresh stale cache and return fresh response through API call")
    void retrieveResponseSync_refreshesStaleCache() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 90000L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(fresh, result);
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
    @DisplayName("clearCache should remove cached response for the given QueryRef")
    void clearCacheRemovesCachedResponse() throws Exception {
        ApiResponse mockResponse = mock(ApiResponse.class);
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        cachedResponses.put(MOCK_CACHE_ID, mockResponse);

        assertTrue(cachedResponses.containsKey(MOCK_CACHE_ID));

        final long waitTime = 1000L;
        ApiCache.clearCache(mockQueryRef, waitTime);

        assertFalse(cachedResponses.containsKey(MOCK_CACHE_ID));
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

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertFalse(result, "isRunning should return false when refresh started exactly 60 seconds ago");
    }

    @Test
    @DisplayName("isRunning should return false when refresh started more than 60 seconds ago")
    void isRunningWhenRefreshIsOld() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 120000);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertFalse(result, "isRunning should return false when refresh started more than 60 seconds ago");
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