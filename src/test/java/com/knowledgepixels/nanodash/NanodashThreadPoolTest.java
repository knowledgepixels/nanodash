package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests waiting for the shared background pool to go quiet (issue #668).
 */
class NanodashThreadPoolTest {

    /**
     * A caller that waits out a running task learns whether the pool went quiet, so a test
     * cleaning up after background work can tell the difference instead of assuming it.
     */
    @Test
    @DisplayName("awaitQuiescence should report the timeout while a task runs, and success once it is done")
    void reportsWhetherThePoolWentQuiet() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        NanodashThreadPool.submit(() -> {
            started.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS), "the task should have started");

        assertFalse(NanodashThreadPool.awaitQuiescence(Duration.ofMillis(50)),
                "the pool is busy, so the wait should lapse");

        release.countDown();
        assertTrue(NanodashThreadPool.awaitQuiescence(Duration.ofSeconds(30)),
                "the pool should go quiet once the task is done");
    }

}
