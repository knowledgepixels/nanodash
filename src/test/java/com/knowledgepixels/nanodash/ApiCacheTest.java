package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.nanopub.extra.services.ApiResponse;
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

    private ConcurrentMap<String, ApiResponse> cachedResponses;
    private ConcurrentMap<String, Long> runAfter;
    private ConcurrentMap<String, Long> refreshStart;
    private ConcurrentMap<String, Long> lastRefresh;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        when(mockQueryRef.getAsUrlString()).thenReturn(MOCK_CACHE_ID);

        cachedResponses = getPrivateStaticField("cachedResponses");
        runAfter = getPrivateStaticField("runAfter");
        refreshStart = getPrivateStaticField("refreshStart");
        lastRefresh = getPrivateStaticField("lastRefresh");

        cachedResponses.clear();
        runAfter.clear();
        refreshStart.clear();
        lastRefresh.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        cachedResponses.clear();
        runAfter.clear();
        refreshStart.clear();
        lastRefresh.clear();

        if (mocks != null) {
            mocks.close();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateStaticField(String fieldName) throws Exception {
        Field field = ApiCache.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    private void putCachedResponse(ApiResponse response, long ageMillis) {
        cachedResponses.put(MOCK_CACHE_ID, response);
        lastRefresh.put(MOCK_CACHE_ID, System.currentTimeMillis() - ageMillis);
    }

    @Test
    @DisplayName("retrieveResponseSync should return fresh cached response without API call")
    void retrieveResponseSync_ReturnsFreshCachedResponseWithoutApiCall() throws Exception {
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
    @DisplayName("clearCache should remove cached response for the given QueryRef")
    void clearCacheRemovesCachedResponse() {
        ApiResponse mockResponse = mock(ApiResponse.class);
        cachedResponses.put(MOCK_CACHE_ID, mockResponse);

        assertTrue(cachedResponses.containsKey(MOCK_CACHE_ID));

        final long waitTime = 1000L;
        ApiCache.clearCache(mockQueryRef, waitTime);

        assertFalse(cachedResponses.containsKey(MOCK_CACHE_ID));
    }

    @Test
    @DisplayName("clearCache should handle zero wait time")
    void clearCacheWithZeroWaitTime() {
        long waitMillis = 0L;
        long beforeCall = System.currentTimeMillis();

        ApiCache.clearCache(mockQueryRef, waitMillis);
        long afterCall = System.currentTimeMillis();

        assertTrue(runAfter.containsKey(MOCK_CACHE_ID));
        Long runAfterTime = runAfter.get(MOCK_CACHE_ID);
        assertNotNull(runAfterTime);

        assertTrue(runAfterTime >= beforeCall && runAfterTime <= afterCall);
    }

    @Test
    @DisplayName("clearCache should work when cache is empty")
    void clearCacheWhenCacheIsEmpty() {
        assertFalse(cachedResponses.containsKey(MOCK_CACHE_ID));
        assertDoesNotThrow(() -> ApiCache.clearCache(mockQueryRef, 1000L));
        assertTrue(runAfter.containsKey(MOCK_CACHE_ID));
    }

    @Test
    @DisplayName("clearCache should update runAfter on subsequent calls")
    void clearCacheUpdatesRunAfterOnSubsequentCalls() {
        ApiCache.clearCache(mockQueryRef, 1000L);
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
    void clearCacheWithNegativeWaitTime() {
        long waitMillis = -1000L;

        assertThrows(IllegalArgumentException.class, () -> ApiCache.clearCache(mockQueryRef, waitMillis));
        assertFalse(runAfter.containsKey(MOCK_CACHE_ID));
    }

    @Test
    @DisplayName("isRunning should return false when refreshStart does not contain the cache ID")
    void isRunningWhenCacheIdNotInRefreshStart() {
        assertFalse(refreshStart.containsKey(MOCK_CACHE_ID));
        boolean result = ApiCache.isRunning(mockQueryRef);
        assertFalse(result);
    }

    @Test
    @DisplayName("isRunning should return true when refresh started less than 60 seconds ago")
    void isRunningWhenRefreshIsRecent() {
        long currentTime = System.currentTimeMillis();
        refreshStart.put(MOCK_CACHE_ID, currentTime - 30000);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertTrue(result);
    }

    @Test
    @DisplayName("isRunning should return true when refresh started exactly at current time")
    void isRunningWhenRefreshJustStarted() {
        long currentTime = System.currentTimeMillis();
        refreshStart.put(MOCK_CACHE_ID, currentTime);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertTrue(result);
    }

    @Test
    @DisplayName("isRunning should return false when refresh started exactly 60 seconds ago")
    void isRunningExactlyAtThreshold() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        refreshStart.put(MOCK_CACHE_ID, currentTime - 60 * 1000);

        Thread.sleep(10);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertFalse(result, "isRunning should return false when refresh started exactly 60 seconds ago");
    }

    @Test
    @DisplayName("isRunning should return false when refresh started more than 60 seconds ago")
    void isRunningWhenRefreshIsOld() {
        long currentTime = System.currentTimeMillis();
        refreshStart.put(MOCK_CACHE_ID, currentTime - 120000);

        boolean result = ApiCache.isRunning(mockQueryRef);

        assertFalse(result, "isRunning should return false when refresh started more than 60 seconds ago");
    }

    @Test
    @DisplayName("isRunning should be consistent across multiple calls within timeout window")
    void isRunningConsistentAcrossMultipleCalls() {
        long currentTime = System.currentTimeMillis();
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
    void isRunningThreadSafety() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
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