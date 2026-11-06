package com.knowledgepixels.nanodash.domain;

import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How long a page waits for a resource to be seeded from the cache. The seed runs on the
 * update task; a page waits for it a little, so that the first render after a restart can
 * show the content, but not for as long as a seed takes that has to fetch nanopubs. Run on
 * the request thread under the resource's monitor, such a seed kept every other request for
 * the home page waiting (issue #892).
 * <p>
 * The state an update leaves is set directly: a real update would go to the query API.
 */
class SeedWaitTest {

    private static class TestResource extends AbstractResourceWithProfile {

        TestResource(String id) {
            super(id);
        }

        @Override
        public String getNanopubId() {
            return null;
        }

        @Override
        public Nanopub getNanopub() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public String getLabel() {
            return getId();
        }

    }

    private static void setField(AbstractResourceWithProfile r, String name, Object value) throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(r, value);
    }

    /** A resource whose update has started, with a seed that has not finished. */
    private static TestResource seeding(CompletableFuture<Void> seed, long waitMillis) throws Exception {
        TestResource r = new TestResource("https://example.org/seed-wait/" + System.nanoTime());
        setField(r, "dataNeedsUpdate", false);
        setField(r, "seedStep", seed);
        setField(r, "seedWaitUntil", System.currentTimeMillis() + waitMillis);
        return r;
    }

    @Test
    void aSeedThatFinishesInTimeIsWaitedFor() throws Exception {
        CompletableFuture<Void> seed = new CompletableFuture<>();
        TestResource r = seeding(seed, AbstractResourceWithProfile.SEED_WAIT_MS);
        new Thread(() -> {
            try {
                Thread.sleep(100);
                setField(r, "dataInitialized", true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            seed.complete(null);
        }).start();

        assertTrue(r.isDataInitialized(), "the first render gets the seeded content");
    }

    @Test
    void aSlowSeedIsWaitedForOnlyUntilTheTimeIsUp() throws Exception {
        TestResource r = seeding(new CompletableFuture<>(), 300);

        long start = System.currentTimeMillis();
        assertFalse(r.isDataInitialized(), "the page shows its loading state");
        long waited = System.currentTimeMillis() - start;
        assertTrue(waited >= 250 && waited < 2000, "waited " + waited + " ms");

        start = System.currentTimeMillis();
        assertFalse(r.isDataInitialized());
        assertTrue(System.currentTimeMillis() - start < 100,
                "a poll after the time is up does not wait again");
    }

}
