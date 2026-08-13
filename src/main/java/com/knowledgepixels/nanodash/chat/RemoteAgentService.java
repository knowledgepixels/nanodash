package com.knowledgepixels.nanodash.chat;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks remote AI agents acting for users via API tokens (see
 * docs/remote-mcp.md): which users have a recently active agent, and the
 * navigation requests those agents made via the open_page tool. Unlike the
 * local chat's per-session queue on {@link ClaudeSession}, these queues are
 * keyed by user IRI, because a remote agent has no Wicket session; any
 * Nanodash browser tab of that user picks the navigations up.
 */
public class RemoteAgentService {

    /**
     * How long after its last authenticated call an agent counts as active
     * (and the user's pages keep polling for navigations).
     */
    public static final long ACTIVITY_WINDOW_MILLIS = 30 * 60 * 1000;

    private static final int MAX_QUEUED_NAVIGATIONS = 20;
    private static final long SWEEP_INTERVAL_MILLIS = 10 * 60 * 1000;

    private static RemoteAgentService instance;

    /**
     * Get the singleton instance.
     *
     * @return the RemoteAgentService instance
     */
    public static synchronized RemoteAgentService get() {
        if (instance == null) {
            instance = new RemoteAgentService();
        }
        return instance;
    }

    private final Map<String, Queue<String>> pendingNavigations = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivity = new ConcurrentHashMap<>();

    private RemoteAgentService() {
        ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "remote-agent-sweeper");
            t.setDaemon(true);
            return t;
        });
        sweeper.scheduleAtFixedRate(this::sweep, SWEEP_INTERVAL_MILLIS, SWEEP_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Records that the given user's agent just made an authenticated call.
     *
     * @param userIri the user's IRI
     */
    public void markActive(String userIri) {
        lastActivity.put(userIri, System.currentTimeMillis());
    }

    /**
     * Whether the given user's agent made an authenticated call within the
     * activity window.
     *
     * @param userIri the user's IRI
     * @return true if recently active
     */
    public boolean isRecentlyActive(String userIri) {
        Long last = lastActivity.get(userIri);
        return last != null && System.currentTimeMillis() - last < ACTIVITY_WINDOW_MILLIS;
    }

    /**
     * Queues a navigation for the given user's browser tabs.
     *
     * @param userIri the user's IRI
     * @param path    the in-app path to navigate to (validated by the open_page tool)
     */
    public void requestNavigation(String userIri, String path) {
        markActive(userIri);
        Queue<String> queue = pendingNavigations.computeIfAbsent(userIri, (k) -> new ConcurrentLinkedQueue<>());
        queue.add(path);
        while (queue.size() > MAX_QUEUED_NAVIGATIONS) {
            queue.poll();
        }
    }

    /**
     * Retrieves and removes the next queued navigation for the given user.
     *
     * @param userIri the user's IRI
     * @return the next path, or null if none is queued
     */
    public String pollNavigation(String userIri) {
        Queue<String> queue = pendingNavigations.get(userIri);
        return queue == null ? null : queue.poll();
    }

    private void sweep() {
        long cutoff = System.currentTimeMillis() - ACTIVITY_WINDOW_MILLIS;
        for (Map.Entry<String, Long> entry : lastActivity.entrySet()) {
            if (entry.getValue() < cutoff) {
                lastActivity.remove(entry.getKey(), entry.getValue());
                pendingNavigations.remove(entry.getKey());
            }
        }
    }

}
