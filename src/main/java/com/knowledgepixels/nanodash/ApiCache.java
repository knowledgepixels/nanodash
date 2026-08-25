package com.knowledgepixels.nanodash;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;
import org.eclipse.rdf4j.model.Model;
import org.nanopub.extra.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A utility class for caching API responses and maps to reduce redundant API calls.
 * This class is thread-safe and ensures that cached data is refreshed periodically.
 */
public class ApiCache {

    private ApiCache() {
    } // no instances allowed

    private static final int MAX_CACHE_ENTRIES = 10_000;

    // How stale a cached response may be before the next access triggers a
    // background re-fetch. Must stay reasonably high: every render that finds a
    // query older than this submits a refresh to the shared pool, which uses a
    // CallerRunsPolicy — so too low a value turns page renders into a refresh
    // storm that can run queries synchronously on the request thread.
    private static final long REFRESH_AGE_THRESHOLD_MS = 60 * 1000;

    // How long a cached response is still served immediately (while refreshing in
    // the background) before it is treated as absent and the caller waits for a
    // fresh fetch. Acts as the stale-data fallback during API outages.
    private static final long MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000;

    // Upper bound a synchronous caller waits for an in-flight refresh started by
    // another thread when it has nothing cached yet. Without this wait the caller
    // returns null, letting repositories memoise an empty snapshot (see
    // retrieveResponseSync).
    private static final long SYNC_WAIT_FOR_INFLIGHT_MS = 10 * 1000;

    private static final Cache<String, ApiResponse> cachedResponses = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private static final Cache<String, Model> cachedRdfModels = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private transient static ConcurrentMap<String, Integer> failed = new ConcurrentHashMap<>();
    private static final Cache<String, Map<String, String>> cachedMaps = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .removalListener(ApiCache::cleanupMetadataOnRemoval)
        .build();
    private transient static ConcurrentMap<String, Long> lastRefresh = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> refreshStart = new ConcurrentHashMap<>();
    private transient static ConcurrentMap<String, Long> runAfter = new ConcurrentHashMap<>();

    // Cache ids that must be re-fetched before their entry counts as current again: a
    // genuine browser reload or an explicit clearCache (e.g. after publishing). The
    // cached value itself is deliberately kept, so callers can go on showing the
    // outdated content while the refresh runs instead of only a spinner (issue #599);
    // it is retrieved with retrieveStaleResponse(). The flag is dropped once a refresh
    // attempt has completed, successfully or not.
    private static final Set<String> forcedRefresh = ConcurrentHashMap.newKeySet();

    // How long we keep polling for a just-published nanopub to show up at the query
    // services before giving up and refreshing anyway (issue #629). A hard bound: the
    // probe is a single indexed lookup, but an unbounded retry loop from many publishing
    // sessions is the load shape that has wedged the query API before.
    private static final long INGEST_CONFIRM_MAX_WAIT_MS = 20 * 1000;
    private static final long INGEST_CONFIRM_POLL_INTERVAL_MS = 1000;
    // Margin after a positive probe: the confirming instance has the nanopub, but its
    // other repos and the other instances may trail slightly behind.
    private static final long INGEST_CONFIRM_MARGIN_MS = 1000;

    // Cache ids whose next refresh should wait for the given nanopub to be ingested
    // rather than (only) sit out the blind runAfter delay; set by clearCache after a
    // publish, consumed by waitOutIngestDelay in the background refresh.
    private transient static ConcurrentMap<String, String> awaitIngest = new ConcurrentHashMap<>();
    // Shared probe results, so several views refreshing after the same publish cost one
    // polling loop, not one each. False (timed out or probe failed) is cached too, to
    // keep late arrivals from re-running a full polling round that already gave up.
    private static final Cache<String, Boolean> ingestConfirmResults = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();
    private transient static ConcurrentMap<String, Object> ingestConfirmLocks = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(ApiCache.class);

    // Guava fires removal notifications also when an entry is REPLACED (every routine
    // refresh's put), and processes them lazily during later cache operations. Cleaning
    // up on a replacement would wipe the metadata of a still-cached entry — in
    // particular a missing lastRefresh timestamp used to make retrieveResponseSync
    // throw an NPE on every call until the entry expired.
    private static void cleanupMetadataOnRemoval(RemovalNotification<String, ?> n) {
        if (n.getCause() == RemovalCause.REPLACED) return;
        cleanupMetadata(n.getKey());
    }

    private static void cleanupMetadata(String cacheId) {
        lastRefresh.remove(cacheId);
        failed.remove(cacheId);
        runAfter.remove(cacheId);
        forcedRefresh.remove(cacheId);
        awaitIngest.remove(cacheId);
    }

    /**
     * Fills a memory miss from the per-entry store (see
     * {@link ApiCachePersistence#loadEntry}): the stored response goes back into the
     * in-memory cache with its <em>original</em> refresh timestamp, so the normal staleness
     * logic takes over from there — the restored content is served while anything older than
     * {@link #REFRESH_AGE_THRESHOLD_MS} re-fetches in the background. This is what makes
     * memory eviction invisible to callers: the persistent tier never evicts, so content
     * that once arrived stays available (however outdated) until a re-fetch replaces it.
     * A timestamp from the future (a clock jump) is not adopted, leaving the entry to count
     * as stale rather than as fresh indefinitely.
     *
     * @param cacheId the cache id (the query's URL string)
     * @return the restored response, or null if the store has none
     */
    private static ApiResponse loadResponseFromStore(String cacheId) {
        ApiCachePersistence.PersistedEntry entry = ApiCachePersistence.loadEntry(cacheId);
        if (entry == null || !(entry.value instanceof ApiResponse response)) return null;
        cachedResponses.put(cacheId, response);
        if (entry.lastRefresh <= System.currentTimeMillis()) {
            lastRefresh.putIfAbsent(cacheId, entry.lastRefresh);
        }
        return response;
    }

    /**
     * The map counterpart of {@link #loadResponseFromStore(String)}.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> loadMapFromStore(String cacheId) {
        ApiCachePersistence.PersistedEntry entry = ApiCachePersistence.loadEntry(cacheId);
        if (entry == null || !(entry.value instanceof Map<?, ?> map)) return null;
        cachedMaps.put(cacheId, (Map<String, String>) map);
        if (entry.lastRefresh <= System.currentTimeMillis()) {
            lastRefresh.putIfAbsent(cacheId, entry.lastRefresh);
        }
        return (Map<String, String>) map;
    }

    /**
     * Checks if a cache refresh is currently running for the given cache ID.
     *
     * @param cacheId The unique identifier for the cache.
     * @return True if a refresh is running, false otherwise.
     */
    private static boolean isRunning(String cacheId) {
        Long start = refreshStart.get(cacheId);
        if (start == null) return false;
        return System.currentTimeMillis() - start < 60 * 1000;
    }

    /**
     * Checks if a cache refresh is currently running for the given QueryRef.
     *
     * @param queryRef The query reference
     * @return True if a refresh is running, false otherwise.
     */
    public static boolean isRunning(QueryRef queryRef) {
        return isRunning(queryRef.getAsUrlString());
    }

    /**
     * Request-scoped flag set by {@code NanodashPage} when the current request is a
     * genuine browser reload (the browser sends {@code Cache-Control: max-age=0} or
     * {@code no-cache}). When set, the first access to each query during the page
     * render evicts that query's cache so it re-fetches fresh, while normal
     * navigation, Ajax updates, and the auto-refresh redirect keep serving the
     * cache. Public so the page layer can set it.
     */
    public static final MetaDataKey<Boolean> FORCE_REFRESH_ON_RELOAD = new MetaDataKey<>() {};

    // The query cache-ids already force-evicted during the current reload request,
    // so each is evicted only once (the lazy-load that follows must not re-evict).
    private static final MetaDataKey<HashSet<String>> RELOAD_FORCED_IDS = new MetaDataKey<>() {};

    /**
     * On a genuine browser reload, returns true the first time a given query is
     * accessed this request (and records it), so callers evict its cache once.
     * Returns false on non-reload requests, off the request thread, and for any
     * query already handled this request — so it never triggers a refresh storm.
     */
    private static boolean isForcedReload(String cacheId) {
        RequestCycle rc = RequestCycle.get();
        if (rc == null) return false;
        Boolean force = rc.getMetaData(FORCE_REFRESH_ON_RELOAD);
        if (force == null || !force) return false;
        HashSet<String> handled = rc.getMetaData(RELOAD_FORCED_IDS);
        if (handled == null) {
            handled = new HashSet<>();
            rc.setMetaData(RELOAD_FORCED_IDS, handled);
        }
        return handled.add(cacheId);
    }

    /**
     * Waits out the post-publish ingest delay for a cache entry, if one is pending,
     * before its refresh is allowed to run. With a nanopub to wait for (see
     * {@link #clearCache(QueryRef, long, String)}), the wait is a measurement: poll
     * until the query services report the nanopub as loaded, plus a small margin. If
     * there is none, or the probe fails or times out, this falls back to the blind
     * runAfter delay, so a broken probe never makes publishing worse than before.
     * Runs on background threads only; request threads are diverted beforehand.
     *
     * @param cacheId the cache id (the query's URL string)
     */
    private static void waitOutIngestDelay(String cacheId) throws InterruptedException {
        String npId = awaitIngest.remove(cacheId);
        if (npId != null && awaitNanopubLoaded(npId)) {
            Thread.sleep(INGEST_CONFIRM_MARGIN_MS);
            runAfter.remove(cacheId);
            return;
        }
        Long after = runAfter.get(cacheId);
        if (after != null) {
            while (System.currentTimeMillis() < after) {
                Thread.sleep(100);
            }
            runAfter.remove(cacheId);
        }
    }

    /**
     * Polls the query services until they report the given nanopub as loaded, bounded by
     * {@link #INGEST_CONFIRM_MAX_WAIT_MS}. Concurrent callers for the same nanopub (the
     * several views refreshing after one publish) share a single polling loop: the first
     * caller polls, the others wait on its result.
     *
     * @param npId the nanopub id to wait for
     * @return true if the nanopub was confirmed as loaded, false if the probe timed out
     * or failed (callers then fall back to the blind delay)
     */
    private static boolean awaitNanopubLoaded(String npId) throws InterruptedException {
        Boolean known = ingestConfirmResults.getIfPresent(npId);
        if (known != null) return known;
        Object lock = ingestConfirmLocks.computeIfAbsent(npId, k -> new Object());
        synchronized (lock) {
            try {
                known = ingestConfirmResults.getIfPresent(npId);
                if (known != null) return known;
                long deadline = System.currentTimeMillis() + INGEST_CONFIRM_MAX_WAIT_MS;
                boolean loaded = false;
                while (true) {
                    try {
                        loaded = QueryApiAccess.isNanopubLoaded(npId);
                    } catch (Exception ex) {
                        logger.warn("Nanopub load probe failed for {}: {}", npId, ex.getMessage());
                        break;
                    }
                    if (loaded || System.currentTimeMillis() + INGEST_CONFIRM_POLL_INTERVAL_MS > deadline) break;
                    Thread.sleep(INGEST_CONFIRM_POLL_INTERVAL_MS);
                }
                if (!loaded) {
                    logger.info("Nanopub {} not confirmed as loaded, falling back to blind delay", npId);
                }
                ingestConfirmResults.put(npId, loaded);
                return loaded;
            } finally {
                ingestConfirmLocks.remove(npId, lock);
            }
        }
    }

    /**
     * Updates the cached API response for a specific query reference.
     *
     * @param queryRef The query reference
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateResponse(QueryRef queryRef, boolean forced) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse response;
        if (forced) {
            response = QueryApiAccess.forcedGet(queryRef);
        } else {
            response = QueryApiAccess.get(queryRef);
        }
        String cacheId = queryRef.getAsUrlString();
        logger.info("Updating cached API response for {}", cacheId);
        long timeNow = System.currentTimeMillis();
        cachedResponses.put(cacheId, response);
        lastRefresh.put(cacheId, timeNow);
        ApiCachePersistence.storeEntry(cacheId, response, timeNow);
    }

    public static ApiResponse retrieveResponseSync(QueryRef queryRef, boolean forced) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached API response synchronously for {}", cacheId);
        if (cachedResponses.getIfPresent(cacheId) == null) {
            loadResponseFromStore(cacheId);
        }
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            // lastRefresh can be missing for a cached entry (racing invalidation or
            // refresh); treat that as stale rather than NPEing on the unboxing.
            Long lastRefreshTime = lastRefresh.get(cacheId);
            // A pending forced refresh (clearCache, e.g. after publishing) always counts as
            // stale here: the entry is kept only so the UI can show the outdated content,
            // never to be handed to a synchronous caller as if it were current.
            needsRefresh = forcedRefresh.contains(cacheId) || lastRefreshTime == null
                    || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        // Waiting around is for background threads. A request thread must not sit out an
        // ingest delay or a politeness pause on the user's time: it takes what the cache has
        // and leaves the refresh to a thread that can afford to wait.
        boolean onRequestThread = RequestCycle.get() != null;
        Long after = runAfter.get(cacheId);
        boolean waitingForIngest = (after != null && System.currentTimeMillis() < after)
                || awaitIngest.containsKey(cacheId);
        if (onRequestThread && waitingForIngest) {
            logger.debug("Not waiting out the ingest delay for {} on a request thread", cacheId);
            // Hand the refresh to the background, where waiting out the delay costs nobody
            // anything, and answer with what we have meanwhile.
            retrieveResponseAsync(queryRef);
            return cachedResponses.getIfPresent(cacheId);
        }
        // A merely outdated entry is served right away on any thread, with the re-fetch
        // handed to the background, instead of running the query inline: a synchronous
        // caller that is fine with data from the last refresh cycle must not block on the
        // network for it, whether it serves a user directly (a request thread) or builds
        // the state pages are gated on (the repository and resource-data threads). This is
        // also what lets a restart come back up warm from the persisted snapshot (issue
        // #570) — every restored entry is older than the refresh threshold, and re-fetching
        // them synchronously would stall the first page render on the very queries the
        // snapshot was meant to cover. Callers that genuinely need current data say so, and
        // keep their blocking fetch: a forced call, or an entry marked by clearCache (e.g.
        // just after publishing).
        if (needsRefresh && !forced && !forcedRefresh.contains(cacheId)
                && cachedResponses.getIfPresent(cacheId) != null) {
            logger.debug("Serving outdated response for {}, refreshing in the background", cacheId);
            retrieveResponseAsync(queryRef);
            return cachedResponses.getIfPresent(cacheId);
        }
        if ((needsRefresh || forced) && !isRunning(cacheId)) {
            logger.info("Refreshing cache for {}", cacheId);
            refreshStart.put(cacheId, timeNow);
            try {
                waitOutIngestDelay(cacheId);
                if (!onRequestThread) {
                    if (failed.get(cacheId) != null) {
                        // 1 second pause between failed attempts;
                        Thread.sleep(1000);
                    }
                    // Jitter, so that background refreshes of many queries do not arrive at
                    // the API in lockstep. Pure latency on a request thread, so skipped there.
                    Thread.sleep(100 + new Random().nextLong(400));
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
            }
            try {
                ApiCache.updateResponse(queryRef, forced);
                failed.remove(cacheId);
            } catch (Exception ex) {
                logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                // Keep stale cached data if available, only invalidate if nothing was cached
                if (cachedResponses.getIfPresent(cacheId) == null) {
                    failed.merge(cacheId, 1, Integer::sum);
                }
                lastRefresh.put(cacheId, System.currentTimeMillis());
            } finally {
                refreshStart.remove(cacheId);
                forcedRefresh.remove(cacheId);
            }
        } else if (isRunning(cacheId)
                && (cachedResponses.getIfPresent(cacheId) == null || forcedRefresh.contains(cacheId))) {
            // Another thread is fetching this query and what we hold is either nothing at
            // all or an entry that is only being kept for the stale-content display. Wait
            // for that fetch rather than returning null or the outdated entry: a null here
            // lets a caller (e.g. SpaceRepository) memoise an EMPTY snapshot, which then
            // poisons MaintainedResourceRepository.build() and breaks the home page until
            // the next refresh. This adds no new work; it only waits on the refresh
            // already in flight.
            try {
                long deadline = timeNow + SYNC_WAIT_FOR_INFLIGHT_MS;
                while (isRunning(cacheId)
                        && (cachedResponses.getIfPresent(cacheId) == null || forcedRefresh.contains(cacheId))
                        && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return cachedResponses.getIfPresent(cacheId);
    }

    /**
     * Retrieves a cached API response for a specific QueryRef.
     *
     * @param queryRef The QueryRef object containing the query name and parameters.
     * @return The cached API response, or null if not cached.
     */
    public static ApiResponse retrieveResponseAsync(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached API response asynchronously for {}", cacheId);
        if (isForcedReload(cacheId)) {
            // Keep the entry (see retrieveStaleResponse) but stop treating it as current,
            // so the reload re-queries while the outdated content stays on screen.
            forcedRefresh.add(cacheId);
        }
        boolean forced = forcedRefresh.contains(cacheId);
        if (cachedResponses.getIfPresent(cacheId) == null) {
            loadResponseFromStore(cacheId);
        }
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = !forced && lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = forced || lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    waitOutIngestDelay(cacheId);
                    if (failed.get(cacheId) != null) {
                        // 1 second pause between failed attempts;
                        Thread.sleep(1000);
                    }
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateResponse(queryRef, false);
                    failed.remove(cacheId);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    if (cachedResponses.getIfPresent(cacheId) == null) {
                        failed.merge(cacheId, 1, Integer::sum);
                    }
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                    // Dropped on failure too: a query we cannot reach must not keep every
                    // later render on the stale path, re-submitting a refresh each time.
                    forcedRefresh.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedResponses.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    /**
     * Updates the cached map for a specific query reference.
     *
     * @param queryRef The query reference
     * @throws FailedApiCallException If the API call fails.
     */
    private static void updateMap(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        Map<String, String> map = new HashMap<>();
        List<ApiResponseEntry> respList = QueryApiAccess.get(queryRef).getData();
        while (respList != null && !respList.isEmpty()) {
            ApiResponseEntry resultEntry = respList.removeFirst();
            map.put(resultEntry.get("key"), resultEntry.get("value"));
        }
        String cacheId = queryRef.getAsUrlString();
        long timeNow = System.currentTimeMillis();
        cachedMaps.put(cacheId, map);
        lastRefresh.put(cacheId, timeNow);
        ApiCachePersistence.storeEntry(cacheId, (Serializable) map, timeNow);
    }

    /**
     * Retrieves a cached map for a specific query reference.
     * If the cache is stale, it triggers a background refresh.
     *
     * @param queryRef The query reference
     * @return The cached map, or null if not cached.
     */
    public static Map<String, String> retrieveMap(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        if (isForcedReload(cacheId)) {
            cachedMaps.invalidate(cacheId);
            lastRefresh.remove(cacheId);
        }
        if (cachedMaps.getIfPresent(cacheId) == null) {
            loadMapFromStore(cacheId);
        }
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedMaps.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    waitOutIngestDelay(cacheId);
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
                }
                try {
                    ApiCache.updateMap(queryRef);
                } catch (Exception ex) {
                    logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
                    // Keep whatever is cached, as the response and RDF-model paths do: a query we
                    // cannot reach right now is a reason to go on showing the previous data, never
                    // to throw it away. Only the refresh timestamp is bumped, so the next attempt
                    // waits out the usual interval instead of retrying on every access.
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                }  finally {
                    refreshStart.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedMaps.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    private static void updateRdfModel(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        final Model[] modelRef = new Model[1];
        QueryAccess qa = new QueryAccess() {
            @Override
            protected void processHeader(String[] line) {}
            @Override
            protected void processLine(String[] line) {}
            @Override
            protected void processRdfContent(Model model) {
                modelRef[0] = model;
            }
        };
        qa.call(queryRef);
        if (modelRef[0] == null) {
            throw new FailedApiCallException(new Exception("No RDF content in response for query: " + queryRef.getQueryId()));
        }
        String cacheId = queryRef.getAsUrlString();
        logger.info("Updating cached RDF model for {}", cacheId);
        cachedRdfModels.put(cacheId, modelRef[0]);
        lastRefresh.put(cacheId, System.currentTimeMillis());
    }

    /**
     * Retrieves a cached RDF model for a CONSTRUCT query, triggering a background fetch if needed.
     *
     * @param queryRef The QueryRef for the CONSTRUCT query.
     * @return The cached RDF Model, or null if not yet available.
     */
    public static Model retrieveRdfModelAsync(QueryRef queryRef) {
        long timeNow = System.currentTimeMillis();
        String cacheId = queryRef.getAsUrlString();
        logger.debug("Retrieving cached RDF model asynchronously for {}", cacheId);
        if (isForcedReload(cacheId)) {
            // As in retrieveResponseAsync: mark for re-fetch but keep the model, so a failed
            // refresh still has the previous one to fall back on.
            forcedRefresh.add(cacheId);
        }
        boolean forced = forcedRefresh.contains(cacheId);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedRdfModels.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = !forced && lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = forced || lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        Integer failedCount = failed.get(cacheId);
        if (failedCount != null && failedCount > 2) {
            failed.remove(cacheId);
            throw new RuntimeException("Query failed: " + cacheId);
        }
        if (needsRefresh && !isRunning(cacheId)) {
            NanodashThreadPool.submit(() -> {
                refreshStart.put(cacheId, System.currentTimeMillis());
                try {
                    waitOutIngestDelay(cacheId);
                    if (failed.get(cacheId) != null) {
                        Thread.sleep(1000);
                    }
                    Thread.sleep(100 + new Random().nextLong(400));
                } catch (InterruptedException ex) {
                    logger.error("Interrupted while waiting to refresh RDF cache: {}", ex.getMessage());
                }
                try {
                    updateRdfModel(queryRef);
                    failed.remove(cacheId);
                } catch (Exception ex) {
                    logger.error("Failed to update RDF cache for {}: {}", cacheId, ex.getMessage());
                    if (cachedRdfModels.getIfPresent(cacheId) == null) {
                        failed.merge(cacheId, 1, Integer::sum);
                    }
                    lastRefresh.put(cacheId, System.currentTimeMillis());
                } finally {
                    refreshStart.remove(cacheId);
                    forcedRefresh.remove(cacheId);
                }
            });
        }
        if (isCached) {
            return cachedRdfModels.getIfPresent(cacheId);
        } else {
            return null;
        }
    }

    /**
     * Returns whatever response is cached for a query reference, however outdated, without
     * triggering a refresh. A memory miss falls through to the per-entry store, which never
     * evicts, so this finds any response that ever arrived for the query — restored quickly
     * from a local file, never the network. Meant for showing the previous content
     * while a refresh is in flight (issue #599) — never as a substitute for the current data,
     * which is what {@link #retrieveResponseAsync(QueryRef)} and
     * {@link #retrieveResponseSync(QueryRef, boolean)} return.
     *
     * @param queryRef The query reference
     * @return The cached response of any age, or null if nothing is cached.
     */
    public static ApiResponse retrieveStaleResponse(QueryRef queryRef) {
        String cacheId = queryRef.getAsUrlString();
        ApiResponse response = cachedResponses.getIfPresent(cacheId);
        if (response != null) return response;
        return loadResponseFromStore(cacheId);
    }

    /**
     * The cache content worth carrying across restarts: the query responses and maps together
     * with when each was last refreshed. The transient bookkeeping (running refreshes, failure
     * counts, ingest delays, forced-refresh markings) is process-local by nature and stays out.
     * The cached RDF models are also left out for now: they would need a text serialization of
     * their own, and their queries re-fetch quickly enough.
     */
    static class Snapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Map<String, ApiResponse> responses;
        private final Map<String, Map<String, String>> maps;
        private final Map<String, Long> refreshTimes;

        private Snapshot(Map<String, ApiResponse> responses, Map<String, Map<String, String>> maps, Map<String, Long> refreshTimes) {
            this.responses = responses;
            this.maps = maps;
            this.refreshTimes = refreshTimes;
        }

        boolean isEmpty() {
            return responses.isEmpty() && maps.isEmpty();
        }

        int size() {
            return responses.size() + maps.size();
        }

    }

    /**
     * Captures the persistable cache content (see {@link Snapshot}). Entries whose refresh
     * timestamp is missing — typically because their first fetch is still in flight — are
     * left out, since without a timestamp the importer could not tell how stale they are.
     *
     * @return a snapshot of the current cache content
     */
    static Snapshot exportSnapshot() {
        Map<String, ApiResponse> responses = new HashMap<>(cachedResponses.asMap());
        Map<String, Map<String, String>> maps = new HashMap<>(cachedMaps.asMap());
        Map<String, Long> refreshTimes = new HashMap<>();
        for (String cacheId : responses.keySet()) {
            Long t = lastRefresh.get(cacheId);
            if (t != null) refreshTimes.put(cacheId, t);
        }
        for (String cacheId : maps.keySet()) {
            Long t = lastRefresh.get(cacheId);
            if (t != null) refreshTimes.put(cacheId, t);
        }
        responses.keySet().retainAll(refreshTimes.keySet());
        maps.keySet().retainAll(refreshTimes.keySet());
        return new Snapshot(responses, maps, refreshTimes);
    }

    /**
     * Restores a snapshot into the cache, meant to run once at startup before the instance
     * serves requests. Each entry keeps its original refresh timestamp, so the normal age
     * logic takes over from there: anything older than {@link #REFRESH_AGE_THRESHOLD_MS} is
     * re-fetched in the background on first access while the restored content is shown
     * meanwhile — the same stale-but-displayable behavior as within a single run.
     *
     * <p>Entries already present in the cache are left alone, as are entries older than the
     * given maximum age or carrying a timestamp from the future (a clock jump must not
     * produce entries that would count as fresh indefinitely).</p>
     *
     * @param snapshot the snapshot to restore
     * @param maxAgeMs entries whose last refresh lies further back than this are dropped
     * @return the number of restored entries
     */
    static int importSnapshot(Snapshot snapshot, long maxAgeMs) {
        long timeNow = System.currentTimeMillis();
        int count = 0;
        for (Map.Entry<String, ApiResponse> e : snapshot.responses.entrySet()) {
            Long t = snapshot.refreshTimes.get(e.getKey());
            if (t == null || t > timeNow || timeNow - t > maxAgeMs) continue;
            if (e.getValue() == null || cachedResponses.getIfPresent(e.getKey()) != null) continue;
            cachedResponses.put(e.getKey(), e.getValue());
            lastRefresh.putIfAbsent(e.getKey(), t);
            count++;
        }
        for (Map.Entry<String, Map<String, String>> e : snapshot.maps.entrySet()) {
            Long t = snapshot.refreshTimes.get(e.getKey());
            if (t == null || t > timeNow || timeNow - t > maxAgeMs) continue;
            if (e.getValue() == null || cachedMaps.getIfPresent(e.getKey()) != null) continue;
            cachedMaps.put(e.getKey(), e.getValue());
            lastRefresh.putIfAbsent(e.getKey(), t);
            count++;
        }
        return count;
    }

    /**
     * Copies a restored snapshot's entries into the per-entry store, so content saved by a
     * version from before the store existed is not lost to memory eviction again. Entries
     * the store already has are left alone (its version is at least as new), and no age
     * limit applies — unlike the in-memory import, the store keeps everything. Meant to run
     * once at startup, right after the snapshot file is read.
     *
     * @param snapshot the restored snapshot
     */
    static void backfillEntryStore(Snapshot snapshot) {
        for (Map.Entry<String, ApiResponse> e : snapshot.responses.entrySet()) {
            Long t = snapshot.refreshTimes.get(e.getKey());
            if (t == null || e.getValue() == null) continue;
            ApiCachePersistence.storeEntryIfAbsent(e.getKey(), e.getValue(), t);
        }
        for (Map.Entry<String, Map<String, String>> e : snapshot.maps.entrySet()) {
            Long t = snapshot.refreshTimes.get(e.getKey());
            if (t == null || e.getValue() == null) continue;
            ApiCachePersistence.storeEntryIfAbsent(e.getKey(), (Serializable) e.getValue(), t);
        }
    }

    /**
     * Marks the cached response for a specific query reference as outdated and sets a delay
     * before the next refresh can occur. The previous response is kept and remains available
     * through {@link #retrieveStaleResponse(QueryRef)} until the refresh lands, but it is no
     * longer served as current data.
     *
     * @param queryRef   The query reference for which to clear the cache.
     * @param waitMillis The amount of time in milliseconds to wait before allowing the cache to be refreshed again.
     */
    public static void clearCache(QueryRef queryRef, long waitMillis) {
        clearCache(queryRef, waitMillis, null);
    }

    /**
     * Like {@link #clearCache(QueryRef, long)}, but for the refresh after a publish: the
     * refresh is released as soon as the query services confirm the given nanopub as
     * loaded (plus a small margin), instead of after the blind delay (issue #629). The
     * delay stays in place as the fallback for when the confirmation probe fails, and the
     * confirmation wait itself is bounded by {@link #INGEST_CONFIRM_MAX_WAIT_MS}.
     *
     * @param queryRef   The query reference for which to clear the cache.
     * @param waitMillis The fallback delay in milliseconds, used if the nanopub's arrival cannot be confirmed.
     * @param nanopubId  The id of the just-published nanopub to wait for, or null for the plain delay.
     */
    public static void clearCache(QueryRef queryRef, long waitMillis, String nanopubId) {
        if (waitMillis < 0) {
            throw new IllegalArgumentException("waitMillis must be non-negative");
        }
        String cacheId = queryRef.getAsUrlString();
        forcedRefresh.add(cacheId);
        runAfter.put(cacheId, System.currentTimeMillis() + waitMillis);
        if (nanopubId != null) awaitIngest.put(cacheId, nanopubId);
    }

}
