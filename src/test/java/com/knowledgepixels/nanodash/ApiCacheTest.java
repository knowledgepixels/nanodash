package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCacheTest {

    @Mock
    private QueryRef mockQueryRef;
    private final String mockQueryId = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";
    private AutoCloseable mocks;

    private ConcurrentMap<String, ApiResponse> cachedResponses;
    private ConcurrentMap<String, Long> runAfter;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        when(mockQueryRef.getAsUrlString()).thenReturn(mockQueryId);

        cachedResponses = getPrivateStaticField("cachedResponses");
        runAfter = getPrivateStaticField("runAfter");

        cachedResponses.clear();
        runAfter.clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        cachedResponses.clear();
        runAfter.clear();

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

    @Test
    @DisplayName("clearCache should remove cached response for the given QueryRef")
    void clearCacheRemovesCachedResponse() {
        ApiResponse mockResponse = mock(ApiResponse.class);
        cachedResponses.put(mockQueryId, mockResponse);

        assertTrue(cachedResponses.containsKey(mockQueryId));

        final long waitTime = 1000L;
        ApiCache.clearCache(mockQueryRef, waitTime);

        assertFalse(cachedResponses.containsKey(mockQueryId));
    }

    @Test
    @DisplayName("clearCache should handle zero wait time")
    void clearCacheWithZeroWaitTime() {
        long waitMillis = 0L;
        long beforeCall = System.currentTimeMillis();

        ApiCache.clearCache(mockQueryRef, waitMillis);
        long afterCall = System.currentTimeMillis();

        assertTrue(runAfter.containsKey(mockQueryId));
        Long runAfterTime = runAfter.get(mockQueryId);
        assertNotNull(runAfterTime);

        assertTrue(runAfterTime >= beforeCall && runAfterTime <= afterCall);
    }

    @Test
    @DisplayName("clearCache should work when cache is empty")
    void clearCacheWhenCacheIsEmpty() {
        assertFalse(cachedResponses.containsKey(mockQueryId));
        assertDoesNotThrow(() -> ApiCache.clearCache(mockQueryRef, 1000L));
        assertTrue(runAfter.containsKey(mockQueryId));
    }

    @Test
    @DisplayName("clearCache should update runAfter on subsequent calls")
    void clearCacheUpdatesRunAfterOnSubsequentCalls() {
        ApiCache.clearCache(mockQueryRef, 1000L);
        Long firstRunAfter = runAfter.get(mockQueryId);
        assertNotNull(firstRunAfter);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }

        ApiCache.clearCache(mockQueryRef, 2000L);
        Long secondRunAfter = runAfter.get(mockQueryId);

        assertNotNull(secondRunAfter);
        assertTrue(secondRunAfter > firstRunAfter);
    }

    @Test
    @DisplayName("clearCache should throw IllegalArgumentException for negative wait time")
    void clearCacheWithNegativeWaitTime() {
        long waitMillis = -1000L;

        assertThrows(IllegalArgumentException.class, () -> ApiCache.clearCache(mockQueryRef, waitMillis));
        assertFalse(runAfter.containsKey(mockQueryId));
    }

}