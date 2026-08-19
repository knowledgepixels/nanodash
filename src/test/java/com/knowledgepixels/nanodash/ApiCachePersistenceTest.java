package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.google.common.cache.Cache;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

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

}
