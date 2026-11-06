package com.knowledgepixels.nanodash;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Fetching many nanopubs at once, as a build of the spaces does after a restart: one by one
 * they kept every page waiting for minutes (issue #892). The fetch is handed in, so no test
 * goes near a registry.
 */
class UtilsGetNanopubsTest {

    private static final String NP_1 = np('1');
    private static final String NP_2 = np('2');
    private static final String NP_3 = np('3');
    private static final String NP_4 = np('4');

    private static String np(char c) {
        return "https://w3id.org/np/RA" + String.valueOf(c).repeat(43);
    }

    @BeforeEach
    @AfterEach
    void clearNanopubCache() throws Exception {
        Field f = Utils.class.getDeclaredField("nanopubs");
        f.setAccessible(true);
        ((Cache<?, ?>) f.get(null)).invalidateAll();
    }

    @Test
    void fetchesSeveralAtATime() {
        // Each fetch waits until all four have started, which they only can if they run
        // side by side; one after the other, the first would give up after its wait.
        CountDownLatch allStarted = new CountDownLatch(4);
        long start = System.currentTimeMillis();
        Map<String, Nanopub> found = Utils.getNanopubs(List.of(NP_1, NP_2, NP_3, NP_4), id -> {
            allStarted.countDown();
            try {
                return allStarted.await(5, TimeUnit.SECONDS) ? mock(Nanopub.class) : null;
            } catch (InterruptedException ex) {
                return null;
            }
        });
        assertEquals(Set.of(NP_1, NP_2, NP_3, NP_4), found.keySet());
        assertTrue(System.currentTimeMillis() - start < 5000, "the fetches ran side by side");
    }

    @Test
    void leavesOutWhatCouldNotBeFetched() {
        Nanopub np1 = mock(Nanopub.class);
        Map<String, Nanopub> found = Utils.getNanopubs(List.of(NP_1, NP_2), id -> id.equals(NP_1) ? np1 : null);
        assertEquals(Map.of(NP_1, np1), found);
    }

    @Test
    void fetchesNeitherNonNanopubsNorAnythingTwice() {
        Map<String, Integer> calls = new ConcurrentHashMap<>();
        Utils.getNanopubs(List.of(NP_1, NP_2, NP_1, "https://example.org/not-a-nanopub"), id -> {
            calls.merge(id, 1, Integer::sum);
            return mock(Nanopub.class);
        });
        assertEquals(Map.of(NP_1, 1, NP_2, 1), calls);
    }

    @Test
    void takesCachedNanopubsFromTheCache() {
        Nanopub cached = mock(Nanopub.class);
        Utils.importCachedNanopubs(Map.of("RA" + "1".repeat(43), cached));
        Map<String, Integer> calls = new ConcurrentHashMap<>();
        Map<String, Nanopub> found = Utils.getNanopubs(List.of(NP_1, NP_2), id -> {
            calls.merge(id, 1, Integer::sum);
            return mock(Nanopub.class);
        });
        assertSame(cached, found.get(NP_1));
        assertEquals(Map.of(NP_2, 1), calls, "only the one not cached is fetched");
    }

}
