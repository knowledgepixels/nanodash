package com.knowledgepixels.nanodash;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared thread pool for background tasks (cache refresh, data updates, etc.).
 * Uses a core pool of 16 threads that can grow up to 64 under load, with idle
 * threads above the core size reclaimed after 60 seconds.
 */
public class NanodashThreadPool {

    private NanodashThreadPool() {
    } // no instances allowed

    private static final int CORE_POOL_SIZE = 16;
    private static final int MAX_POOL_SIZE = 64;
    private static final long KEEP_ALIVE_SECONDS = 60;
    private static final long QUIESCENCE_POLL_INTERVAL_MS = 20;

    private static final ThreadPoolExecutor POOL = newPool("nanodash-bg-");

    /**
     * A new pool of the shape background tasks run in: a core of threads that grows under
     * load, no queue of its own — a {@link SynchronousQueue} hands each task straight to a
     * thread — and the caller running the task itself when every thread is taken.
     *
     * <p>Package-private because a test needs a pool nothing else submits to. The shared
     * one is busy for as long as the application runs, and under a full test suite it is
     * never idle for more than a moment, so anything asserting that it went quiet asserts
     * something about the rest of the suite rather than about the code under test.
     *
     * @param threadNamePrefix what this pool's threads are called, numbered from one
     * @return the new pool
     */
    static ThreadPoolExecutor newPool(String threadNamePrefix) {
        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, threadNamePrefix + counter.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static Future<?> submit(Runnable task) {
        return POOL.submit(task);
    }

    /**
     * Submits a task that produces a result. Note that under the caller-runs policy a
     * task may end up running in the calling thread when the pool is saturated, in which
     * case waiting on the returned future with a timeout doesn't bound the wait.
     *
     * @param task the task to run
     * @param <T>  the type of the task's result
     * @return a future holding the task's result
     */
    public static <T> Future<T> submit(Callable<T> task) {
        return POOL.submit(task);
    }

    /**
     * Waits until no submitted task is running or waiting to run any more, or until the
     * timeout lapses.
     * <p>
     * For a test that tears down something its background tasks write into: a refresh
     * submitted by an earlier test sleeps before it writes, so it can still be on its way
     * while the next test cleans up (issue #668). Unlike a shutdown, this leaves the pool
     * usable.
     *
     * @param timeout how long to wait at most
     * @return true once the pool is idle, false if the timeout lapsed first or the wait
     *         was interrupted
     */
    public static boolean awaitQuiescence(Duration timeout) {
        return awaitQuiescence(POOL, timeout);
    }

    /**
     * The same wait, on a pool given to it rather than on the shared one, so that the
     * waiting itself can be tested.
     *
     * @param pool    the pool to wait on
     * @param timeout how long to wait at most
     * @return true once that pool is idle, false if the timeout lapsed first or the wait
     *         was interrupted
     */
    static boolean awaitQuiescence(ThreadPoolExecutor pool, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (pool.getActiveCount() > 0 || !pool.getQueue().isEmpty()) {
            if (System.nanoTime() - deadline >= 0) return false;
            try {
                Thread.sleep(QUIESCENCE_POLL_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

}
