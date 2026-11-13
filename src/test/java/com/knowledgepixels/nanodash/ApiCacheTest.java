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
import org.nanopub.extra.services.QueryRefusedException;

import com.google.common.cache.Cache;

import java.lang.reflect.Field;
import java.time.Instant;
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
        resetMap("failures");
        resetMap("lastRefresh");
        resetMap("refreshStart");
        resetMap("runAfter");
        resetMap("forcedRefresh");
        resetMap("publishRefreshes");
        resetMap("ingestConfirmResults");
        resetMap("ingestConfirmLocks");

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

    private Object probeOutcome(String name) throws Exception {
        Class<?> c = Class.forName("com.knowledgepixels.nanodash.ApiCache$ProbeOutcome");
        for (Object constant : c.getEnumConstants()) {
            if (constant.toString().equals(name)) return constant;
        }
        throw new IllegalArgumentException(name);
    }

    private Object publishRefresh(String npId, boolean awaitMaterialization, long retryUntil) throws Exception {
        Class<?> c = Class.forName("com.knowledgepixels.nanodash.ApiCache$PublishRefresh");
        var ctor = c.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return ctor.newInstance(npId, awaitMaterialization, retryUntil);
    }

    /** Records the given number of consecutive failures, with the next attempt due at retryAt. */
    private static QueryRefusedException refusedUntil(long epochMilli) {
        return new QueryRefusedException("refused", Instant.ofEpochMilli(epochMilli));
    }

    private void putFailure(int count, long retryAt) throws Exception {
        Class<?> c = Class.forName("com.knowledgepixels.nanodash.ApiCache$FailureState");
        var ctor = c.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        getMap("failures").put(MOCK_CACHE_ID, ctor.newInstance(count, retryAt));
    }

    /** The recorded consecutive failures, or 0 if there are none. */
    private int failureCount() throws Exception {
        Object state = getMap("failures").get(MOCK_CACHE_ID);
        if (state == null) return 0;
        var accessor = state.getClass().getDeclaredMethod("count");
        accessor.setAccessible(true);
        return (int) accessor.invoke(state);
    }

    /** When the next attempt is due, read from the recorded failure state. */
    private long retryAt() throws Exception {
        Object state = getMap("failures").get(MOCK_CACHE_ID);
        var accessor = state.getClass().getDeclaredMethod("retryAt");
        accessor.setAccessible(true);
        return (long) accessor.invoke(state);
    }

    /** Makes the shared pool run submitted refreshes on this thread, where the static mocks apply. */
    private static void runSubmittedTasksInline(MockedStatic<NanodashThreadPool> threadPool) {
        threadPool.when(() -> NanodashThreadPool.submit(any(Runnable.class))).thenAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return null;
        });
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
            assertEquals(1, failureCount());
            assertTrue(ApiCache.isBackingOff(mockQueryRef));
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
    @DisplayName("backoffAfter should grow from 10s to 30s to 1min, then stay at 5min (issue #889)")
    void backoffAfter_growsToFiveMinutes() {
        assertEquals(10_000, ApiCache.backoffAfter(1));
        assertEquals(30_000, ApiCache.backoffAfter(2));
        assertEquals(60_000, ApiCache.backoffAfter(3));
        assertEquals(300_000, ApiCache.backoffAfter(4));
        assertEquals(300_000, ApiCache.backoffAfter(50));
    }

    @Test
    @DisplayName("backoffAfter should wait as long as a refusing query server asks, but at least the ladder step and at most 5min (issue #889)")
    void backoffAfter_honoursRetryAfter() {
        long now = 1_000_000;
        // Longer than the ladder step: the server's time wins.
        assertEquals(45_000, ApiCache.backoffAfter(1, refusedUntil(now + 45_000), now));
        // Shorter than the ladder step: the ladder still holds.
        assertEquals(30_000, ApiCache.backoffAfter(2, refusedUntil(now + 5_000), now));
        // Already past, or far off: bounded on both sides.
        assertEquals(10_000, ApiCache.backoffAfter(1, refusedUntil(now - 60_000), now));
        assertEquals(300_000, ApiCache.backoffAfter(1, refusedUntil(now + 3_600_000), now));
        // A refusal that names no time, and any other failure, follow the ladder.
        assertEquals(10_000, ApiCache.backoffAfter(1, new QueryRefusedException("refused", null), now));
        assertEquals(60_000, ApiCache.backoffAfter(3, new FailedApiCallException(new Exception("timed out")), now));
    }

    @Test
    @DisplayName("A refused query should not be tried again before the time the query server named (issue #889)")
    void refusedQueryWaitsForRetryAfter() throws Exception {
        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            long before = System.currentTimeMillis();
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenThrow(refusedUntil(before + 45_000));

            assertNull(ApiCache.retrieveResponseSync(mockQueryRef, false));

            assertEquals(1, failureCount());
            assertTrue(retryAt() >= before + 44_000, "the first failure alone would only wait 10s");
            assertTrue(retryAt() <= System.currentTimeMillis() + 45_000);
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should make no call while the query is backed off, forced or not, and keep the backoff (issue #889)")
    void retrieveResponseSync_makesNoCallWhileBackingOff() throws Exception {
        long retryAt = System.currentTimeMillis() + 60_000;
        putFailure(3, retryAt);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            assertNull(ApiCache.retrieveResponseSync(mockQueryRef, false));
            assertNull(ApiCache.retrieveResponseSync(mockQueryRef, true));

            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
            queryApiAccess.verify(() -> QueryApiAccess.forcedGet(any()), never());
            // Answering for the failure must not start the retries over.
            assertEquals(3, failureCount());
            assertEquals(retryAt, retryAt());
        }
    }

    @Test
    @DisplayName("retrieveResponseSync should serve the cached response while the query is backed off")
    void retrieveResponseSync_servesCachedResponseWhileBackingOff() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 2 * 60 * 1000L);
        putFailure(1, System.currentTimeMillis() + 60_000);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class);
             MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            assertSame(cached, ApiCache.retrieveResponseSync(mockQueryRef, false));

            threadPool.verify(() -> NanodashThreadPool.submit(any(Runnable.class)), never());
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
        }
    }

    @Test
    @DisplayName("Consecutive failures should push the next attempt further out")
    void consecutiveFailuresGrowTheBackoff() throws Exception {
        // The previous backoff is over, so this call tries again, and fails again.
        putFailure(1, System.currentTimeMillis() - 1);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenThrow(new FailedApiCallException(new Exception("timed out")));
            long before = System.currentTimeMillis();

            assertNull(ApiCache.retrieveResponseSync(mockQueryRef, false));

            queryApiAccess.verify(() -> QueryApiAccess.get(mockQueryRef), times(1));
            assertEquals(2, failureCount());
            assertTrue(retryAt() >= before + 30_000, "the second failure should put the next attempt 30s out");
        }
    }

    @Test
    @DisplayName("A successful attempt should clear the failures")
    void successClearsTheFailures() throws Exception {
        ApiResponse fresh = mock(ApiResponse.class);
        putFailure(4, System.currentTimeMillis() - 1);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            assertSame(fresh, ApiCache.retrieveResponseSync(mockQueryRef, false));

            assertEquals(0, failureCount());
            assertFalse(ApiCache.isBackingOff(mockQueryRef));
        }
    }

    @Test
    @DisplayName("A failed refresh of a cached query should count as a failure too, keeping the cached response")
    void failedRefreshOfCachedQueryCounts() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 2 * 60 * 1000L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class);
             MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenThrow(new FailedApiCallException(new Exception("timed out")));
            runSubmittedTasksInline(threadPool);

            assertSame(cached, ApiCache.retrieveResponseAsync(mockQueryRef));

            assertEquals(1, failureCount(), "a query that turns slow after having worked must be backed off as well");
            assertSame(cached, ApiCache.retrieveStaleResponse(mockQueryRef));
        }
    }

    @Test
    @DisplayName("retrieveResponseAsync should neither fetch nor throw while a query with nothing cached is backed off")
    void retrieveResponseAsync_answersNullWhileBackingOff() throws Exception {
        putFailure(3, System.currentTimeMillis() + 60_000);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class);
             MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            // Pages are built from this call, so a failing query must not throw out of it.
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));

            assertTrue(ApiCache.isBackingOff(mockQueryRef));
            threadPool.verify(() -> NanodashThreadPool.submit(any(Runnable.class)), never());
            queryApiAccess.verify(() -> QueryApiAccess.get(any()), never());
        }
    }

    @Test
    @DisplayName("retrieveResponseAsync should serve the cached response while backed off, even if a refresh was forced")
    void retrieveResponseAsync_servesCachedResponseWhileBackingOffDespiteForcedRefresh() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 5000L);
        ApiCache.clearCache(mockQueryRef, 0L);
        putFailure(2, System.currentTimeMillis() + 60_000);

        try (MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            assertSame(cached, ApiCache.retrieveResponseAsync(mockQueryRef),
                    "nothing newer can arrive before the next attempt, so what is cached is shown as it is");

            threadPool.verify(() -> NanodashThreadPool.submit(any(Runnable.class)), never());
            assertTrue(getSet("forcedRefresh").contains(MOCK_CACHE_ID), "the next attempt should still honour the forced refresh");
        }
    }

    @Test
    @DisplayName("retrieveResponseAsync should try again once the backoff is over")
    void retrieveResponseAsync_triesAgainAfterBackoff() throws Exception {
        putFailure(2, System.currentTimeMillis() - 1);

        try (MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));

            threadPool.verify(() -> NanodashThreadPool.submit(any(Runnable.class)), times(1));
            assertFalse(ApiCache.isBackingOff(mockQueryRef));
        }
    }

    @Test
    @DisplayName("retrieveResponseAsync should start only one refresh for concurrent callers")
    void retrieveResponseAsync_startsOneRefreshAtATime() throws Exception {
        // The submitted task is not run, so the first refresh stays in flight.
        try (MockedStatic<NanodashThreadPool> threadPool = mockStatic(NanodashThreadPool.class)) {
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));
            assertNull(ApiCache.retrieveResponseAsync(mockQueryRef));

            threadPool.verify(() -> NanodashThreadPool.submit(any(Runnable.class)), times(1));
            assertTrue(ApiCache.isRunning(mockQueryRef));
        }
    }

    @Test
    @DisplayName("retrieveResponseIfAvailable should answer with null while a failing query is backed off (issue #684)")
    void retrieveResponseIfAvailable_answersWithNullWhileBackingOff() throws Exception {
        putFailure(3, System.currentTimeMillis() + 60_000);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            // The callers of this hold the state whole pages are built from: a query that
            // cannot be answered has to read as "nothing to show yet", not as a failed page.
            assertNull(ApiCache.retrieveResponseIfAvailable(mockQueryRef));
        }
    }

    @Test
    @DisplayName("retrieveResponseIfAvailable should hand back a cached response like retrieveResponseSync")
    void retrieveResponseIfAvailable_returnsCachedResponse() throws Exception {
        ApiResponse cached = mock(ApiResponse.class);
        putCachedResponse(cached, 0L);

        assertSame(cached, ApiCache.retrieveResponseIfAvailable(mockQueryRef));
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
    @DisplayName("a publish-marked refresh should be released by ingest confirmation instead of the blind delay")
    void publishMarkedRefreshReleasedByIngestConfirmation() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x1";
        // A fallback delay far longer than the test may take: with a positive probe, the
        // refresh must not wait it out.
        ApiCache.clearCache(mockQueryRef, 60000L, npId, false);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.isNanopubLoaded(npId)).thenReturn(true);
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            long start = System.currentTimeMillis();
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            long elapsed = System.currentTimeMillis() - start;

            assertSame(fresh, result);
            assertTrue(elapsed < 10000, "confirmation should release the refresh long before the 60s fallback, but took " + elapsed + "ms");
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubLoaded(npId));
            assertFalse(this.<String, Object>getMap("publishRefreshes").containsKey(MOCK_CACHE_ID), "the pending confirmation should be consumed");
            assertFalse(this.<String, Long>getMap("runAfter").containsKey(MOCK_CACHE_ID), "the fallback delay should be dropped on confirmation");
        }
    }

    @Test
    @DisplayName("a failing ingest probe should fall back to the blind delay")
    void failingIngestProbeFallsBackToBlindDelay() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x2";
        ApiCache.clearCache(mockQueryRef, 0L, npId, false);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.isNanopubLoaded(npId)).thenThrow(new FailedApiCallException(new Exception("probe broken")));
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            // A broken probe must never make publishing worse than before: the refresh
            // still runs after the (here elapsed) fallback delay.
            assertSame(fresh, result);
        }
    }

    @Test
    @DisplayName("a shared negative probe result should short-circuit later waiters")
    void sharedNegativeProbeResultShortCircuits() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x3";
        // Another view's refresh has already polled for this nanopub and given up.
        this.<String, Object>getMap("ingestConfirmResults").put(npId + "|loaded", probeOutcome("NOT_YET"));
        ApiCache.clearCache(mockQueryRef, 0L, npId, false);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(fresh, result);
            // The shared result answers instead of a second polling round.
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubLoaded(any()), never());
        }
    }

    @Test
    @DisplayName("a clear with no nanopub to wait for should still sit out its blind delay")
    void clearWithoutNanopubStillWaitsOutBlindDelay() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        long waitMillis = 1200L;
        ApiCache.clearCache(mockQueryRef, waitMillis);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            long start = System.currentTimeMillis();
            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);
            long elapsed = System.currentTimeMillis() - start;

            assertSame(fresh, result);
            assertTrue(elapsed >= waitMillis,
                    "the blind delay is all an ordinary cleared entry has, so it must be waited out, but took only " + elapsed + "ms");
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubLoaded(any()), never());
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubMaterialized(any()), never());
        }
    }

    @Test
    @DisplayName("a role grant's refresh should wait for materialization, not for ingest")
    void roleGrantRefreshWaitsForMaterialization() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x4";
        ApiCache.clearCache(mockQueryRef, 60000L, npId, true);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.isNanopubMaterialized(npId)).thenReturn(true);
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            assertSame(fresh, result);
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubMaterialized(npId));
            // Ingest is the wrong question here, and asking it is what released the
            // refresh too early: the row cannot be in the results until the authority
            // resolver has written it into the current space state.
            queryApiAccess.verify(() -> QueryApiAccess.isNanopubLoaded(any()), never());
            assertFalse(this.<String, Object>getMap("publishRefreshes").containsKey(MOCK_CACHE_ID));
        }
    }

    @Test
    @DisplayName("a refresh that fetched before its publication arrived should stay pending for another attempt")
    void unconfirmedPublishRefreshIsRetried() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x5";
        // A probing round has already run and concluded the publication is not there yet.
        this.<String, Object>getMap("ingestConfirmResults").put(npId + "|materialized", probeOutcome("NOT_YET"));
        ApiCache.clearCache(mockQueryRef, 0L, npId, true);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiCache.retrieveResponseSync(mockQueryRef, false);

            // The fetch went ahead, but it is not the final word: the entry stays on the
            // forced path so the next access runs the wait and the query again, which is
            // what the UI needs in order to go on waiting.
            assertTrue(getSet("forcedRefresh").contains(MOCK_CACHE_ID), "the refresh should still count as pending");
            assertTrue(ApiCache.hasPendingPublishRefresh(mockQueryRef), "the UI should be told to keep waiting");
            // and the stale verdict is dropped, so the next round really does probe again
            assertFalse(this.<String, Object>getMap("ingestConfirmResults").containsKey(npId + "|materialized"));
        }
    }

    @Test
    @DisplayName("a probe that cannot be run should not be retried")
    void unrunnableProbeIsNotRetried() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x6";
        ApiCache.clearCache(mockQueryRef, 0L, npId, true);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.isNanopubMaterialized(npId))
                    .thenThrow(new FailedApiCallException(new Exception("probe query not published")));
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiResponse result = ApiCache.retrieveResponseSync(mockQueryRef, false);

            // A probe that answers nothing is no evidence that anything is missing, so this
            // falls back to exactly the old behaviour: one fetch, then settle.
            assertSame(fresh, result);
            assertFalse(getSet("forcedRefresh").contains(MOCK_CACHE_ID));
            assertFalse(ApiCache.hasPendingPublishRefresh(mockQueryRef));
        }
    }

    @Test
    @DisplayName("retrying should stop once the publish refresh budget has run out")
    void publishRefreshRetryStopsAtDeadline() throws Exception {
        ApiResponse stale = mock(ApiResponse.class);
        ApiResponse fresh = mock(ApiResponse.class);
        putCachedResponse(stale, 5000L);
        String npId = "https://w3id.org/np/RAtest0000000000000000000000000000000000000x7";
        this.<String, Object>getMap("ingestConfirmResults").put(npId + "|materialized", probeOutcome("NOT_YET"));
        getSet("forcedRefresh").add(MOCK_CACHE_ID);
        // The publication was never confirmed and the budget for asking again is spent.
        this.<String, Object>getMap("publishRefreshes").put(MOCK_CACHE_ID,
                publishRefresh(npId, true, System.currentTimeMillis() - 1));

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            queryApiAccess.when(() -> QueryApiAccess.get(mockQueryRef)).thenReturn(fresh);

            ApiCache.retrieveResponseSync(mockQueryRef, false);

            // Giving up has to be complete: a cache id left on the forced path would put
            // every later render on the stale branch and re-submit a refresh each time.
            assertFalse(getSet("forcedRefresh").contains(MOCK_CACHE_ID));
            assertFalse(ApiCache.hasPendingPublishRefresh(mockQueryRef));
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
    @DisplayName("isRunning should still return true for a refresh that has been running for 90 seconds (issue #889)")
    void isRunningForAnAttemptThatTimesOutOnEveryServer() throws Exception {
        // One attempt at a query that times out everywhere: three rounds over three servers,
        // 10s each. Counting it as over after 60s let a second attempt start beside it.
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, System.currentTimeMillis() - 90 * 1000);

        assertTrue(ApiCache.isRunning(mockQueryRef));
    }

    @Test
    @DisplayName("isRunning should return false when refresh started exactly 5 minutes ago")
    void isRunningExactlyAtThreshold() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 5 * 60 * 1000);

        Thread.sleep(10);

        assertFalse(ApiCache.isRunning(mockQueryRef));
    }

    @Test
    @DisplayName("isRunning should return false when refresh started more than 5 minutes ago")
    void isRunningWhenRefreshIsOld() throws Exception {
        long currentTime = System.currentTimeMillis();
        ConcurrentMap<String, Long> refreshStart = getMap("refreshStart");
        refreshStart.put(MOCK_CACHE_ID, currentTime - 10 * 60 * 1000);

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