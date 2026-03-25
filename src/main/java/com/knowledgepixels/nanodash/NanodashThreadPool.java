package com.knowledgepixels.nanodash;

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

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "nanodash-bg-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static Future<?> submit(Runnable task) {
        return POOL.submit(task);
    }

}
