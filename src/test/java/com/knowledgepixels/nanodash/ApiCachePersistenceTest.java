package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import com.google.common.cache.Cache;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class ApiCachePersistenceTest {

    @TempDir
    File tempDir;

    private static final String RESPONSE_ID = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";
    private static final String MAP_ID = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-some-map";

    @BeforeEach
    void setUp() throws Exception {
        resetMap("cachedResponses");
        resetMap("cachedMaps");
        resetMap("failed");
        resetMap("lastRefresh");
        resetMap("refreshStart");
        resetMap("runAfter");
        resetMap("forcedRefresh");
        // The nanopub cache is saved along with the query caches and is populated by
        // other tests in the suite, so it needs the same reset.
        Field f = Utils.class.getDeclaredField("nanopubs");
        f.setAccessible(true);
        ((Cache<?, ?>) f.get(null)).invalidateAll();
        // The entry store is global too; tests that need it point it at their temp dir.
        ApiCachePersistence.initEntryStore(null);
    }

    @AfterEach
    void tearDown() {
        ApiCachePersistence.initEntryStore(null);
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
    private <K, V> ConcurrentMap<K, V> getMap(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object obj = f.get(null);
        if (obj instanceof Cache<?, ?> cache) {
            return (ConcurrentMap<K, V>) cache.asMap();
        }
        return (ConcurrentMap<K, V>) obj;
    }

    private static ApiResponse makeResponse(String value) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[] {"thing"});
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("thing", value);
        response.add(entry);
        return response;
    }

    private void putCachedResponse(String cacheId, ApiResponse response, long ageMillis) throws Exception {
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        ConcurrentMap<String, Long> lastRefresh = getMap("lastRefresh");
        cachedResponses.put(cacheId, response);
        lastRefresh.put(cacheId, System.currentTimeMillis() - ageMillis);
    }

    private void putCachedMap(String cacheId, Map<String, String> map, long ageMillis) throws Exception {
        ConcurrentMap<String, Map<String, String>> cachedMaps = getMap("cachedMaps");
        ConcurrentMap<String, Long> lastRefresh = getMap("lastRefresh");
        cachedMaps.put(cacheId, map);
        lastRefresh.put(cacheId, System.currentTimeMillis() - ageMillis);
    }

    @Test
    @DisplayName("save and load should round-trip responses and maps with their refresh timestamps")
    void saveAndLoadRoundTrip() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("some-value"), 5000L);
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        putCachedMap(MAP_ID, map, 5000L);
        ConcurrentMap<String, Long> lastRefreshBefore = getMap("lastRefresh");
        long responseRefreshTime = lastRefreshBefore.get(RESPONSE_ID);
        long mapRefreshTime = lastRefreshBefore.get(MAP_ID);

        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);
        assertTrue(file.isFile());

        setUp(); // back to an empty cache, as after a restart
        ApiCachePersistence.load(file);

        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        assertNotNull(cachedResponses.get(RESPONSE_ID));
        assertEquals("some-value", cachedResponses.get(RESPONSE_ID).getData().getFirst().get("thing"));
        ConcurrentMap<String, Map<String, String>> cachedMaps = getMap("cachedMaps");
        assertEquals(map, cachedMaps.get(MAP_ID));
        ConcurrentMap<String, Long> lastRefresh = getMap("lastRefresh");
        assertEquals(responseRefreshTime, lastRefresh.get(RESPONSE_ID));
        assertEquals(mapRefreshTime, lastRefresh.get(MAP_ID));
    }

    @Test
    @DisplayName("load should drop entries older than the maximum snapshot age")
    void loadDropsAncientEntries() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("ancient"), 8L * 24 * 60 * 60 * 1000);
        putCachedResponse(MAP_ID, makeResponse("recent"), 5000L);

        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);

        setUp();
        ApiCachePersistence.load(file);

        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        assertNull(cachedResponses.get(RESPONSE_ID));
        assertNotNull(cachedResponses.get(MAP_ID));
    }

    @Test
    @DisplayName("load should drop entries with a refresh timestamp from the future")
    void loadDropsFutureTimestamps() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("from-the-future"), -60 * 60 * 1000L);

        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);

        setUp();
        ApiCachePersistence.load(file);

        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        assertNull(cachedResponses.get(RESPONSE_ID));
    }

    @Test
    @DisplayName("save should not write an empty cache over an existing snapshot")
    void saveSkipsEmptyCache() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("kept"), 5000L);
        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);
        long lengthBefore = file.length();

        setUp();
        ApiCachePersistence.save(file);

        assertEquals(lengthBefore, file.length(), "the snapshot from the warm run should still be there");
        ApiCachePersistence.load(file);
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        assertNotNull(cachedResponses.get(RESPONSE_ID));
    }

    @Test
    @DisplayName("save should skip entries whose refresh timestamp is missing")
    void saveSkipsEntriesWithoutRefreshTimestamp() throws Exception {
        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        cachedResponses.put(RESPONSE_ID, makeResponse("mid-refresh"));
        putCachedResponse(MAP_ID, makeResponse("complete"), 5000L);

        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);

        setUp();
        ApiCachePersistence.load(file);

        assertNull(getMap("cachedResponses").get(RESPONSE_ID));
        assertNotNull(getMap("cachedResponses").get(MAP_ID));
    }

    @Test
    @DisplayName("load should ignore a corrupt snapshot file and leave the cache empty")
    void loadIgnoresCorruptFile() throws Exception {
        File file = new File(tempDir, "cache.ser");
        Files.write(file.toPath(), new byte[] {1, 2, 3, 4, 5});

        assertDoesNotThrow(() -> ApiCachePersistence.load(file));
        assertTrue(this.<String, ApiResponse>getMap("cachedResponses").isEmpty());
    }

    @Test
    @DisplayName("load should do nothing when the snapshot file does not exist")
    void loadHandlesMissingFile() {
        assertDoesNotThrow(() -> ApiCachePersistence.load(new File(tempDir, "does-not-exist.ser")));
    }

    @Test
    @DisplayName("load should not overwrite entries already in the cache")
    void loadKeepsExistingEntries() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("old"), 5000L);
        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);

        setUp();
        ApiResponse current = makeResponse("current");
        putCachedResponse(RESPONSE_ID, current, 0L);
        ApiCachePersistence.load(file);

        ConcurrentMap<String, ApiResponse> cachedResponses = getMap("cachedResponses");
        assertSame(current, cachedResponses.get(RESPONSE_ID));
    }

    private File initEntryStore() {
        File storeDir = new File(tempDir, "store");
        ApiCachePersistence.initEntryStore(storeDir);
        return storeDir;
    }

    @Test
    @DisplayName("entry store should round-trip a response with its refresh timestamp")
    void entryStoreRoundTrip() {
        initEntryStore();
        long refreshTime = System.currentTimeMillis() - 5000L;
        ApiCachePersistence.storeEntry(RESPONSE_ID, makeResponse("stored"), refreshTime);

        ApiCachePersistence.PersistedEntry entry = ApiCachePersistence.loadEntry(RESPONSE_ID);
        assertNotNull(entry);
        assertEquals(RESPONSE_ID, entry.cacheId);
        assertEquals(refreshTime, entry.lastRefresh);
        assertEquals("stored", ((ApiResponse) entry.value).getData().getFirst().get("thing"));
    }

    @Test
    @DisplayName("entry store should be a no-op while not initialized")
    void entryStoreDisabledWithoutInit() {
        ApiCachePersistence.storeEntry(RESPONSE_ID, makeResponse("ignored"), System.currentTimeMillis());
        assertNull(ApiCachePersistence.loadEntry(RESPONSE_ID));
        assertFalse(new File(tempDir, "store").exists());
    }

    @Test
    @DisplayName("retrieveStaleResponse should fall through to the entry store on a memory miss")
    void staleResponseReadsThroughToStore() throws Exception {
        initEntryStore();
        QueryRef queryRef = new QueryRef(RESPONSE_ID);
        String cacheId = queryRef.getAsUrlString();
        long refreshTime = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000; // way beyond memory expiry
        ApiCachePersistence.storeEntry(cacheId, makeResponse("evicted-but-stored"), refreshTime);

        ApiResponse result = ApiCache.retrieveStaleResponse(queryRef);

        assertNotNull(result);
        assertEquals("evicted-but-stored", result.getData().getFirst().get("thing"));
        // The entry is back in memory with its original timestamp, so the normal
        // staleness logic takes over from here.
        assertNotNull(this.<String, ApiResponse>getMap("cachedResponses").get(cacheId));
        assertEquals(refreshTime, this.<String, Long>getMap("lastRefresh").get(cacheId));
    }

    @Test
    @DisplayName("retrieveResponseSync should serve a stored entry without calling the API")
    void syncReadsThroughToStoreWithoutApiCall() {
        initEntryStore();
        QueryRef queryRef = new QueryRef(RESPONSE_ID);
        String cacheId = queryRef.getAsUrlString();
        // Fresh enough that no background refresh is due, so the call is fully deterministic.
        ApiCachePersistence.storeEntry(cacheId, makeResponse("from-store"), System.currentTimeMillis() - 5000L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            ApiResponse result = ApiCache.retrieveResponseSync(queryRef, false);

            assertNotNull(result);
            assertEquals("from-store", result.getData().getFirst().get("thing"));
            queryApiAccess.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("retrieveMap should serve a stored map without calling the API")
    void mapReadsThroughToStoreWithoutApiCall() {
        initEntryStore();
        QueryRef queryRef = new QueryRef(MAP_ID);
        String cacheId = queryRef.getAsUrlString();
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        ApiCachePersistence.storeEntry(cacheId, map, System.currentTimeMillis() - 5000L);

        try (MockedStatic<QueryApiAccess> queryApiAccess = mockStatic(QueryApiAccess.class)) {
            Map<String, String> result = ApiCache.retrieveMap(queryRef);

            assertEquals(map, result);
            queryApiAccess.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("storeEntryIfAbsent should not overwrite an existing entry")
    void storeEntryIfAbsentKeepsExisting() {
        initEntryStore();
        ApiCachePersistence.storeEntry(RESPONSE_ID, makeResponse("existing"), 1000L);
        ApiCachePersistence.storeEntryIfAbsent(RESPONSE_ID, makeResponse("newcomer"), 2000L);

        ApiCachePersistence.PersistedEntry entry = ApiCachePersistence.loadEntry(RESPONSE_ID);
        assertEquals("existing", ((ApiResponse) entry.value).getData().getFirst().get("thing"));
    }

    @Test
    @DisplayName("loadEntry should delete an unreadable entry file and report a miss")
    void corruptEntryFileIsDeletedOnRead() throws Exception {
        File storeDir = initEntryStore();
        ApiCachePersistence.storeEntry(RESPONSE_ID, makeResponse("soon-corrupt"), System.currentTimeMillis());
        File[] files = storeDir.listFiles();
        assertEquals(1, files.length);
        Files.write(files[0].toPath(), new byte[] {1, 2, 3, 4, 5});

        assertNull(ApiCachePersistence.loadEntry(RESPONSE_ID));
        assertFalse(files[0].isFile(), "the useless file should be gone");
    }

    @Test
    @DisplayName("load should backfill the entry store from the snapshot file")
    void loadBackfillsEntryStore() throws Exception {
        putCachedResponse(RESPONSE_ID, makeResponse("snapshot-only"), 5000L);
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        putCachedMap(MAP_ID, map, 5000L);
        File file = new File(tempDir, "cache.ser");
        ApiCachePersistence.save(file);

        setUp();
        initEntryStore();
        ApiCachePersistence.load(file);

        ApiCachePersistence.PersistedEntry responseEntry = ApiCachePersistence.loadEntry(RESPONSE_ID);
        assertNotNull(responseEntry);
        assertEquals("snapshot-only", ((ApiResponse) responseEntry.value).getData().getFirst().get("thing"));
        ApiCachePersistence.PersistedEntry mapEntry = ApiCachePersistence.loadEntry(MAP_ID);
        assertNotNull(mapEntry);
        assertEquals(map, mapEntry.value);
    }

}
