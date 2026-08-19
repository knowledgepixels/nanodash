package com.knowledgepixels.nanodash;

import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Persists the {@link ApiCache} content and the nanopub cache (see {@link Utils#getNanopub})
 * across restarts (issue #570): both are written to a snapshot file periodically and on
 * shutdown, and read back at startup. A freshly restarted or upgraded instance thereby has
 * content to show right away — the query responses re-fetch in the background while the
 * restored ones are served — instead of starting cold and re-fetching everything from the
 * query API and the registry at once.
 *
 * <p>The snapshot is a cache, not a store of record: a missing, corrupt, or (after a library
 * upgrade) unreadable file only means starting cold, never failing startup. The default file
 * location is inside {@code ~/.nanopub}, which the standard Docker setup already mounts as a
 * volume, so deployments get persistence without any configuration.</p>
 */
public class ApiCachePersistence {

    private ApiCachePersistence() {
    } // no instances allowed

    private static final Logger logger = LoggerFactory.getLogger(ApiCachePersistence.class);

    // How often the cache is snapshotted to disk. Also the most that a hard crash (where the
    // shutdown save never runs) can lose.
    private static final long SAVE_INTERVAL_MINUTES = 5;

    // Snapshot entries whose last refresh lies further back than this are dropped at load
    // time, so a long-unused snapshot does not resurrect ancient content. Deliberately much
    // longer than the in-memory 24h serving horizon: entries beyond that horizon are still
    // valuable as the stale-content fallback when the query API is unreachable after a
    // restart.
    private static final long MAX_SNAPSHOT_AGE_MS = 7 * 24 * 60 * 60 * 1000L;

    private static ScheduledExecutorService scheduler;
    private static File snapshotFile;

    /**
     * The root object written to the snapshot file: the query cache content together with
     * the cached nanopubs. The nanopubs matter as much as the query responses for a warm
     * start — building the user data, for instance, means fetching an intro nanopub per
     * user, serially, when they are not cached — and being immutable they can be restored
     * without any staleness considerations.
     */
    private static class PersistedState implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ApiCache.Snapshot queryCache;
        private final Map<String, Nanopub> nanopubs;
        // These two are null when reading a file from before the field existed.
        private final Map<String, org.apache.commons.lang3.tuple.Pair<Long, View>> resolvedViews;
        private final Map<String, View> views;

        private PersistedState(ApiCache.Snapshot queryCache, Map<String, Nanopub> nanopubs,
                Map<String, org.apache.commons.lang3.tuple.Pair<Long, View>> resolvedViews, Map<String, View> views) {
            this.queryCache = queryCache;
            this.nanopubs = nanopubs;
            this.resolvedViews = resolvedViews;
            this.views = views;
        }

    }

    /**
     * Loads the snapshot file into the {@link ApiCache} and starts the periodic saving. Meant
     * to run once at application startup, before the instance serves requests. Does nothing
     * when persistence is disabled (see {@link NanodashPreferences#getApiCacheFile()}).
     */
    public static synchronized void init() {
        if (scheduler != null) return;
        String path = NanodashPreferences.get().getApiCacheFile();
        if (path == null) {
            logger.info("API cache persistence is disabled");
            return;
        }
        snapshotFile = new File(path);
        load(snapshotFile);
        scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "nanodash-cache-persistence");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> save(snapshotFile), SAVE_INTERVAL_MINUTES, SAVE_INTERVAL_MINUTES, TimeUnit.MINUTES);
        logger.info("API cache persistence initialized with snapshot file {}", snapshotFile);
    }

    /**
     * Stops the periodic saving and writes a final snapshot. Meant to run once at application
     * shutdown.
     */
    public static synchronized void shutdown() {
        if (scheduler == null) return;
        scheduler.shutdown();
        scheduler = null;
        save(snapshotFile);
    }

    /**
     * Restores the cache content from the given snapshot file, if there is one and it is
     * readable. Any failure — including a snapshot written by an incompatible earlier version
     * of the involved classes — is logged and otherwise ignored: the instance then simply
     * starts with a cold cache, and the next periodic save replaces the unusable file.
     *
     * @param file the snapshot file to read
     */
    static void load(File file) {
        if (!file.isFile()) return;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            Object obj = in.readObject();
            if (obj instanceof PersistedState state) {
                int queryCount = ApiCache.importSnapshot(state.queryCache, MAX_SNAPSHOT_AGE_MS);
                int npCount = Utils.importCachedNanopubs(state.nanopubs);
                int resolvedCount = state.resolvedViews == null ? 0 : View.importResolvedViews(state.resolvedViews, MAX_SNAPSHOT_AGE_MS);
                int viewCount = state.views == null ? 0 : View.importViews(state.views);
                logger.info("Restored {} cached query results, {} cached nanopubs, {} views and {} view resolutions from {}",
                        queryCount, npCount, viewCount, resolvedCount, file);
            } else if (obj instanceof ApiCache.Snapshot snapshot) {
                // A file from before the snapshot also carried the nanopub cache.
                int count = ApiCache.importSnapshot(snapshot, MAX_SNAPSHOT_AGE_MS);
                logger.info("Restored {} cached query results from {}", count, file);
            } else {
                logger.warn("Ignoring cache snapshot file {} with unexpected content", file);
            }
        } catch (Exception ex) {
            logger.warn("Could not read cache snapshot file {}; starting with a cold cache: {}", file, ex.toString());
        }
    }

    /**
     * Writes the current cache content to the given snapshot file. The snapshot is written to
     * a temporary file first and then moved into place, so a crash mid-write leaves the
     * previous snapshot intact. An empty cache is not written out: an instance shutting down
     * before it warmed up must not wipe the useful snapshot from the previous run.
     *
     * @param file the snapshot file to write
     */
    static void save(File file) {
        try {
            ApiCache.Snapshot snapshot = ApiCache.exportSnapshot();
            Map<String, Nanopub> nanopubs = Utils.exportCachedNanopubs();
            if (snapshot.isEmpty() && nanopubs.isEmpty()) return;
            Map<String, org.apache.commons.lang3.tuple.Pair<Long, View>> resolvedViews = View.exportResolvedViews();
            Map<String, View> views = View.exportViews();
            File dir = file.getAbsoluteFile().getParentFile();
            if (dir != null) dir.mkdirs();
            File tmpFile = new File(file.getPath() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
                out.writeObject(new PersistedState(snapshot, nanopubs, resolvedViews, views));
            }
            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logger.debug("Saved {} cached query results and {} cached nanopubs to {}", snapshot.size(), nanopubs.size(), file);
        } catch (Exception ex) {
            logger.warn("Could not write cache snapshot file {}: {}", file, ex.toString());
        }
    }

}
