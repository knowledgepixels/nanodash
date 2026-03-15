package com.knowledgepixels.nanodash;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared bounded thread pool for background tasks (cache refresh, data updates, etc.).
 */
public class NanodashThreadPool {

    private NanodashThreadPool() {
    } // no instances allowed

    private static final ExecutorService POOL = Executors.newFixedThreadPool(16, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "nanodash-bg-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public static Future<?> submit(Runnable task) {
        return POOL.submit(task);
    }

}
