package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests waiting for a background pool to go quiet (issue #668).
 */
class NanodashThreadPoolTest {

    /**
     * A caller that waits out a running task learns whether the pool went quiet, so a test
     * cleaning up after background work can tell the difference instead of assuming it.
     *
     * <p>Run on a pool of its own. The shared pool is never quiet during a full suite run —
     * other tests keep it loading spaces and refreshing caches over the network — so
     * asserting that <em>it</em> went idle was a claim about the rest of the suite, and one
     * that stopped holding as the suite grew.
     */
    @Test
    @DisplayName("awaitQuiescence should report the timeout while a task runs, and success once it is done")
    void reportsWhetherThePoolWentQuiet() throws Exception {
        ThreadPoolExecutor pool = NanodashThreadPool.newPool("nanodash-test-");
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            pool.submit(() -> {
                started.countDown();
                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS), "the task should have started");

            assertFalse(NanodashThreadPool.awaitQuiescence(pool, Duration.ofMillis(50)),
                    "the pool is busy, so the wait should lapse");

            release.countDown();
            assertTrue(NanodashThreadPool.awaitQuiescence(pool, Duration.ofSeconds(30)),
                    "the pool should go quiet once the task is done");
        } finally {
            pool.shutdownNow();
        }
    }

}
