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

    // How long a query is left alone after its first, second, third, and every further
    // consecutive failure (issue #889). A query that kept failing used to be asked again
    // after a second, as fast as pages rendered it, on every instance. On 5 November 2026
    // that kept 6-9 copies of a single query that never finished in time running on every
    // query server: an attempt goes to the servers in turn, and a server goes on evaluating
    // a query for up to a minute after the caller has given up on it.
    private static final long[] FAILURE_BACKOFF_MS = {10_000, 30_000, 60_000, 5 * 60_000};

    // When a refresh that has not reported back counts as lost, so that another may start.
    // Only a safety net, since a refresh clears its own mark when it ends. It has to outlast
    // the slowest attempt: nanopub-java tries each query server in turn, for three rounds,
    // with up to 10s per try, so one attempt at a query that always times out takes about
    // 90s. At 60s, a second attempt used to start while the first was still running.
    private static final long REFRESH_PRESUMED_LOST_MS = 5 * 60 * 1000;

    // What claimRefresh returns when another refresh of the entry is running.
    private static final long NO_CLAIM = -1;

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
    // A query's consecutive failures and when the next attempt is due (see
    // FAILURE_BACKOFF_MS). Cleared only by a successful attempt, never by reporting the
    // failure, and kept apart from the cached content, so that neither showing an error nor
    // evicting an entry starts the retries over. The expiry only drops queries nobody asks
    // for anymore; one that keeps failing is written again at least every five minutes.
    private static final Cache<String, FailureState> failures = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    private record FailureState(int count, long retryAt) {
    }

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

    // How long a publish-triggered refresh keeps being retried while the probe still says
    // the publication has not arrived where the query reads it. One probing round is
    // bounded by INGEST_CONFIRM_MAX_WAIT_MS, so this is a handful of rounds at most; it is
    // deliberately the same budget RefreshingResultPanel gives the spinner, because past
    // it there is nobody left waiting for the answer.
    private static final long PUBLISH_REFRESH_MAX_WAIT_MS = 60 * 1000;

    // What a cache entry's pending publish-triggered refresh is waiting for: the nanopub
    // that was just published, whether it has to be materialized into the current space
    // state (a role grant) rather than merely ingested, and when to stop retrying.
    private record PublishRefresh(String nanopubId, boolean awaitMaterialization, long retryUntil) {
    }

    // Cache ids whose next refresh should wait for a just-published nanopub to arrive
    // rather than (only) sit out the blind runAfter delay; set by clearCache after a
    // publish, consumed by waitOutIngestDelay in the background refresh.
    private transient static ConcurrentMap<String, PublishRefresh> publishRefreshes = new ConcurrentHashMap<>();
    // Shared probe results, so several views refreshing after the same publish cost one
    // polling loop, not one each. A negative outcome is cached too, to keep late arrivals
    // from re-running a full polling round that already gave up; a retry invalidates the
    // entry first, so the next round does probe again.
    private static final Cache<String, ProbeOutcome> ingestConfirmResults = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();
    private transient static ConcurrentMap<String, Object> ingestConfirmLocks = new ConcurrentHashMap<>();

    /**
     * What a round of probing for a just-published nanopub concluded. The distinction that
     * matters is between a probe that answered "not there yet" — worth asking again — and
     * one that could not answer at all, which tells us nothing and so is no reason to keep
     * re-running the query.
     */
    private enum ProbeOutcome {
        /** The publication has arrived where the query reads it. */
        ARRIVED,
        /** The probe answered, and the publication is not there yet. */
        NOT_YET,
        /** The probe could not be run (unreachable, or an unpublished probe query). */
        UNKNOWN
    }

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
        runAfter.remove(cacheId);
        forcedRefresh.remove(cacheId);
        publishRefreshes.remove(cacheId);
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
        return System.currentTimeMillis() - start < REFRESH_PRESUMED_LOST_MS;
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
     * Claims the refresh of a cache entry, unless another refresh of it is running. Atomic, so
     * that of the many renders asking for the same query at once only one starts an attempt;
     * checking {@link #isRunning(String)} first and marking the start afterwards left a gap in
     * which several could.
     *
     * @param cacheId the cache id (the query's URL string)
     * @return the claim, to be released by {@link #runRefresh}, or {@link #NO_CLAIM}
     */
    private static long claimRefresh(String cacheId) {
        long now = System.currentTimeMillis();
        boolean[] claimed = {false};
        refreshStart.compute(cacheId, (id, start) -> {
            if (start != null && now - start < REFRESH_PRESUMED_LOST_MS) return start;
            claimed[0] = true;
            return now;
        });
        return claimed[0] ? now : NO_CLAIM;
    }

    /**
     * How long a query is left alone after the given number of consecutive failures.
     *
     * @param consecutiveFailures the number of failed attempts in a row, at least 1
     * @return the delay in milliseconds before the next attempt
     */
    static long backoffAfter(int consecutiveFailures) {
        int i = Math.min(Math.max(consecutiveFailures, 1), FAILURE_BACKOFF_MS.length) - 1;
        return FAILURE_BACKOFF_MS[i];
    }

    private static void recordFailure(String cacheId) {
        long now = System.currentTimeMillis();
        FailureState state = failures.asMap().merge(cacheId, new FailureState(1, now + backoffAfter(1)),
                (previous, first) -> new FailureState(previous.count() + 1, now + backoffAfter(previous.count() + 1)));
        logger.warn("Query failed {} time(s) in a row; not trying it again for {}s: {}",
                state.count(), backoffAfter(state.count()) / 1000, cacheId);
    }

    private static boolean isBackingOff(String cacheId) {
        FailureState state = failures.getIfPresent(cacheId);
        return state != null && System.currentTimeMillis() < state.retryAt();
    }

    /**
     * Whether the last attempt at a query failed and the next one is not due yet (issue #889).
     * Until then the cache makes no call for the query and serves whatever it holds for it. A
     * view with nothing to show from the cache should report the failure instead of waiting,
     * since nothing is going to arrive before the next attempt.
     *
     * @param queryRef the query reference
     * @return true if the query is being backed off
     */
    public static boolean isBackingOff(QueryRef queryRef) {
        return queryRef != null && isBackingOff(queryRef.getAsUrlString());
    }

    /**
     * Whether the most recent attempt at a query failed, whether or not the next one is due
     * yet. Whatever the cache serves for such a query is from before that attempt, so a view
     * showing it should say that its results are older than they look.
     *
     * @param queryRef the query reference
     * @return true if the query failed when it was last run
     */
    public static boolean lastAttemptFailed(QueryRef queryRef) {
        return queryRef != null && failures.getIfPresent(queryRef.getAsUrlString()) != null;
    }

    /**
     * One fetch updating a cache entry, as run by {@link #runRefresh}.
     */
    @FunctionalInterface
    private interface Fetch {
        void run() throws Exception;
    }

    /**
     * Runs a refresh attempt claimed by {@link #claimRefresh(String)} and records its outcome.
     * A success clears the entry's failures. A failure keeps whatever is cached — a query that
     * cannot be answered right now is a reason to go on showing the previous data, never to
     * throw it away — and puts off the next attempt (see {@link #FAILURE_BACKOFF_MS}). Either
     * way, the claim and the forced-refresh marking are released.
     *
     * @param cacheId the cache id (the query's URL string)
     * @param claim   the claim the attempt runs under
     * @param mayWait whether the thread can afford the jitter before fetching, which a request
     *                thread cannot
     * @param fetch   the fetch that updates the entry
     */
    private static void runRefresh(String cacheId, long claim, boolean mayWait, Fetch fetch) {
        boolean refreshIsFinal = true;
        try {
            refreshIsFinal = waitOutIngestDelay(cacheId);
            if (mayWait) {
                // Jitter, so that background refreshes of many queries do not arrive at the API
                // in lockstep.
                Thread.sleep(100 + new Random().nextLong(400));
            }
        } catch (InterruptedException ex) {
            logger.error("Interrupted while waiting to refresh cache: {}", ex.getMessage());
        }
        try {
            fetch.run();
            failures.invalidate(cacheId);
        } catch (Exception ex) {
            logger.error("Failed to update cache for {}: {}", cacheId, ex.getMessage());
            recordFailure(cacheId);
            lastRefresh.put(cacheId, System.currentTimeMillis());
        } finally {
            refreshStart.remove(cacheId, claim);
            // Dropped on failure too: a query we cannot reach must not keep every later render
            // on the stale path. Kept only while the publication this refresh waits for is
            // still unconfirmed, so the early fetch is not taken as the final answer.
            if (refreshIsFinal || !retryPublishRefresh(cacheId)) forcedRefresh.remove(cacheId);
        }
    }

    /**
     * Runs a claimed refresh attempt on the shared pool (see {@link #runRefresh}).
     */
    private static void submitRefresh(String cacheId, long claim, Fetch fetch) {
        try {
            NanodashThreadPool.submit(() -> runRefresh(cacheId, claim, true, fetch));
        } catch (RuntimeException ex) {
            refreshStart.remove(cacheId, claim);
            throw ex;
        }
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
     * {@link #clearCache(QueryRef, long, String, boolean)}), the wait is a measurement:
     * poll until the query services report the nanopub as having arrived where this query
     * reads it, plus a small margin. If there is none, or the probe fails or times out,
     * this falls back to the blind runAfter delay, so a broken probe never makes publishing
     * worse than before. Runs on background threads only; request threads are diverted
     * beforehand.
     *
     * @param cacheId the cache id (the query's URL string)
     * @return true if the refresh about to run is final, false if it may have to be
     * repeated because the publication had not arrived yet (see
     * {@link #retryPublishRefresh(String)})
     */
    private static boolean waitOutIngestDelay(String cacheId) throws InterruptedException {
        PublishRefresh pending = publishRefreshes.get(cacheId);
        if (pending == null) {
            // Nothing to measure: an ordinary cleared entry just sits out its delay.
            waitOutBlindDelay(cacheId);
            return true;
        }
        ProbeOutcome outcome = awaitNanopubArrival(pending);
        if (outcome == ProbeOutcome.ARRIVED) {
            publishRefreshes.remove(cacheId);
            Thread.sleep(INGEST_CONFIRM_MARGIN_MS);
            runAfter.remove(cacheId);
            return true;
        }
        waitOutBlindDelay(cacheId);
        // The fetch goes ahead either way — showing something beats showing nothing — but
        // only a probe that actually answered "not there yet" justifies asking again. A
        // probe that could not answer is no evidence that anything is missing.
        if (outcome == ProbeOutcome.UNKNOWN) {
            publishRefreshes.remove(cacheId);
            return true;
        }
        return false;
    }

    // Sits out a blind runAfter delay, if one is set for the entry.
    private static void waitOutBlindDelay(String cacheId) throws InterruptedException {
        Long after = runAfter.get(cacheId);
        if (after != null) {
            while (System.currentTimeMillis() < after) {
                Thread.sleep(100);
            }
            runAfter.remove(cacheId);
        }
    }

    /**
     * Re-arms a publish-triggered refresh that fetched before its publication had arrived,
     * so the entry stays on the stale-content path and the next access runs the whole wait
     * and fetch again. Without this the single early fetch is final: it leaves the cache
     * holding pre-publish data with a current timestamp, and
     * {@link com.knowledgepixels.nanodash.component.RefreshingResultPanel} settles on it
     * for good because the results it compares are unchanged.
     * <p>
     * Bounded by {@link #PUBLISH_REFRESH_MAX_WAIT_MS} from the publish: a cache id left on
     * the forced path indefinitely would put every later render on the stale branch and
     * re-submit a refresh each time.
     *
     * @param cacheId the cache id (the query's URL string)
     * @return true if another attempt is due, so the forced-refresh flag must be kept
     */
    private static boolean retryPublishRefresh(String cacheId) {
        PublishRefresh pending = publishRefreshes.get(cacheId);
        if (pending == null) return false;
        if (System.currentTimeMillis() >= pending.retryUntil()) {
            logger.info("Giving up on confirming {} for {}; keeping what the last fetch returned",
                    pending.nanopubId(), cacheId);
            publishRefreshes.remove(cacheId);
            return false;
        }
        // The previous round's negative verdict is shared with every other view waiting on
        // the same publication; dropping it is what lets the next round ask again.
        ingestConfirmResults.invalidate(probeKey(pending));
        runAfter.put(cacheId, System.currentTimeMillis());
        logger.info("Publication {} has not arrived yet for {}; will try again", pending.nanopubId(), cacheId);
        return true;
    }

    /**
     * Whether a publish-triggered refresh for the given query is still being retried, i.e.
     * the just-published nanopub has not been confirmed where this query reads it. Lets the
     * UI keep waiting instead of settling on results that cannot contain the publication
     * yet.
     *
     * @param queryRef the query reference
     * @return true if a publish-triggered refresh is still pending for it
     */
    public static boolean hasPendingPublishRefresh(QueryRef queryRef) {
        if (queryRef == null) return false;
        PublishRefresh pending = publishRefreshes.get(queryRef.getAsUrlString());
        // The deadline is checked here too, not only where retrying stops: an entry whose
        // query nothing touches again is never cleaned up, and it must not hold a spinner.
        return pending != null && System.currentTimeMillis() < pending.retryUntil();
    }

    // Probe results are shared per nanopub, but "ingested" and "materialized" are different
    // questions about it, so they must not share an answer.
    private static String probeKey(PublishRefresh pending) {
        return pending.nanopubId() + (pending.awaitMaterialization() ? "|materialized" : "|loaded");
    }

    /**
     * Polls the query services until they report the awaited nanopub as having arrived
     * where the refreshed query reads it — ingested, or materialized into the current space
     * state — bounded by {@link #INGEST_CONFIRM_MAX_WAIT_MS}. Concurrent callers for the
     * same question (the several views refreshing after one publish) share a single polling
     * loop: the first caller polls, the others wait on its result.
     *
     * @param pending what the refresh is waiting for
     * @return how the round ended; anything but {@link ProbeOutcome#ARRIVED} leaves the
     * caller to fall back to the blind delay
     */
    private static ProbeOutcome awaitNanopubArrival(PublishRefresh pending) throws InterruptedException {
        String key = probeKey(pending);
        ProbeOutcome known = ingestConfirmResults.getIfPresent(key);
        if (known != null) return known;
        Object lock = ingestConfirmLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            try {
                known = ingestConfirmResults.getIfPresent(key);
                if (known != null) return known;
                String npId = pending.nanopubId();
                long deadline = System.currentTimeMillis() + INGEST_CONFIRM_MAX_WAIT_MS;
                ProbeOutcome outcome = ProbeOutcome.NOT_YET;
                while (true) {
                    try {
                        boolean arrived = pending.awaitMaterialization()
                                ? QueryApiAccess.isNanopubMaterialized(npId)
                                : QueryApiAccess.isNanopubLoaded(npId);
                        if (arrived) {
                            outcome = ProbeOutcome.ARRIVED;
                            break;
                        }
                    } catch (Exception ex) {
                        logger.warn("Nanopub arrival probe ({}) failed for {}: {}",
                                pending.awaitMaterialization() ? "materialized" : "loaded", npId, ex.getMessage());
                        outcome = ProbeOutcome.UNKNOWN;
                        break;
                    }
                    if (System.currentTimeMillis() + INGEST_CONFIRM_POLL_INTERVAL_MS > deadline) break;
                    Thread.sleep(INGEST_CONFIRM_POLL_INTERVAL_MS);
                }
                if (outcome != ProbeOutcome.ARRIVED) {
                    logger.info("Nanopub {} not confirmed ({}), falling back to blind delay", npId, outcome);
                }
                ingestConfirmResults.put(key, outcome);
                return outcome;
            } finally {
                ingestConfirmLocks.remove(key, lock);
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

    /**
     * The response for a query if it can be had, and null if it cannot — nothing cached yet,
     * or a query the service could not answer.
     * <p>
     * For the callers that hold the state whole pages are built from, where a query that
     * cannot be answered means "nothing to show yet" rather than an error to raise. They
     * already treat a missing response that way; without this they would treat a failing one
     * as fatal, and a cold instance whose query service is unavailable could then not build a
     * page at all, its own error page included (issue #684).
     *
     * @param queryRef The query reference
     * @return the response, or null if there is none to be had
     */
    public static ApiResponse retrieveResponseIfAvailable(QueryRef queryRef) {
        try {
            return retrieveResponseSync(queryRef, false);
        } catch (Exception ex) {
            logger.error("Could not retrieve {}: {}", queryRef.getAsUrlString(), ex.toString());
            return null;
        }
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
        // A query that failed a moment ago is not asked again before its backoff is over,
        // whatever the caller wants (issue #889). The answer is what the failed attempt left
        // behind: the cached response if there is one, null otherwise. That goes for forced
        // calls too, since insisting on current data cannot make a failing service answer, only
        // keep it busy.
        if (isBackingOff(cacheId)) {
            logger.debug("Not trying {} again yet; it failed recently", cacheId);
            return cachedResponses.getIfPresent(cacheId);
        }
        // Waiting around is for background threads. A request thread must not sit out an
        // ingest delay or a politeness pause on the user's time: it takes what the cache has
        // and leaves the refresh to a thread that can afford to wait.
        boolean onRequestThread = RequestCycle.get() != null;
        Long after = runAfter.get(cacheId);
        boolean waitingForIngest = (after != null && System.currentTimeMillis() < after)
                || publishRefreshes.containsKey(cacheId);
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
        long claim = (needsRefresh || forced) ? claimRefresh(cacheId) : NO_CLAIM;
        if (claim != NO_CLAIM) {
            logger.info("Refreshing cache for {}", cacheId);
            runRefresh(cacheId, claim, !onRequestThread, () -> updateResponse(queryRef, forced));
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
        // A crawler is served whatever is already here and nothing is queued for it
        // (issue #804): every view on every crawled page would otherwise submit a query,
        // which is the fan-out that made crawlers an outage risk. The cache age is
        // deliberately ignored -- an answer from yesterday is real content served for free,
        // which beats the loading spinner a crawler would be given instead.
        if (CrawlerRequest.isCrawler()) {
            return cachedResponses.getIfPresent(cacheId);
        }
        // Nothing is fetched for a query that is being backed off, so a pending forced refresh
        // (a reload, a publish) cannot happen yet either. What is cached is served as it is,
        // since it is all there will be until the next attempt; the marking stays for that
        // attempt.
        boolean backingOff = isBackingOff(cacheId);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedResponses.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = (!forced || backingOff) && lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = forced || lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        if (needsRefresh && !backingOff) {
            long claim = claimRefresh(cacheId);
            if (claim != NO_CLAIM) submitRefresh(cacheId, claim, () -> updateResponse(queryRef, false));
        }
        return isCached ? cachedResponses.getIfPresent(cacheId) : null;
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
        if (needsRefresh && !isBackingOff(cacheId)) {
            long claim = claimRefresh(cacheId);
            if (claim != NO_CLAIM) submitRefresh(cacheId, claim, () -> updateMap(queryRef));
        }
        return isCached ? cachedMaps.getIfPresent(cacheId) : null;
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
        // Nothing is queued for a crawler; see retrieveResponseAsync (issue #804).
        if (CrawlerRequest.isCrawler()) {
            return cachedRdfModels.getIfPresent(cacheId);
        }
        boolean forced = forcedRefresh.contains(cacheId);
        // As in retrieveResponseAsync: nothing is fetched while the query is backed off, so
        // the cached model is served as it is, forced refresh or not.
        boolean backingOff = isBackingOff(cacheId);
        boolean isCached = false;
        boolean needsRefresh = true;
        if (cachedRdfModels.getIfPresent(cacheId) != null) {
            Long lastRefreshTime = lastRefresh.get(cacheId);
            isCached = (!forced || backingOff) && lastRefreshTime != null && timeNow - lastRefreshTime < MAX_CACHE_AGE_MS;
            needsRefresh = forced || lastRefreshTime == null || timeNow - lastRefreshTime > REFRESH_AGE_THRESHOLD_MS;
        }
        if (needsRefresh && !backingOff) {
            long claim = claimRefresh(cacheId);
            if (claim != NO_CLAIM) submitRefresh(cacheId, claim, () -> updateRdfModel(queryRef));
        }
        return isCached ? cachedRdfModels.getIfPresent(cacheId) : null;
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
     * backoffs, ingest delays, forced-refresh markings) is process-local by nature and stays out.
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
        clearCache(queryRef, waitMillis, null, false);
    }

    /**
     * Like {@link #clearCache(QueryRef, long)}, but for the refresh after a publish: the
     * refresh is released as soon as the query services confirm the given nanopub as having
     * arrived (plus a small margin), instead of after the blind delay (issue #629). The
     * delay stays in place as the fallback for when the confirmation probe fails, and one
     * confirmation wait is bounded by {@link #INGEST_CONFIRM_MAX_WAIT_MS}.
     * <p>
     * What counts as "arrived" depends on what the refreshed query reads. Most queries only
     * need the nanopub to be ingested. The space listing queries read role instantiations
     * out of the current space state, which the authority resolver writes in a later pass of
     * its own; for those, {@code awaitMaterialization} makes the wait a measurement of that
     * pass instead, since a refresh released on ingest alone re-runs the query before it can
     * possibly return the new row.
     * <p>
     * If the publication has still not arrived once the probe's budget runs out, the refresh
     * runs anyway — showing something beats showing nothing — but stays marked as
     * provisional and is repeated, for up to {@link #PUBLISH_REFRESH_MAX_WAIT_MS} from here
     * (see {@link #retryPublishRefresh(String)}).
     *
     * @param queryRef            The query reference for which to clear the cache.
     * @param waitMillis          The fallback delay in milliseconds, used if the nanopub's arrival cannot be confirmed.
     * @param nanopubId           The id of the just-published nanopub to wait for, or null for the plain delay.
     * @param awaitMaterialization Whether to wait for the nanopub to be materialized into the current space state rather than merely ingested.
     */
    public static void clearCache(QueryRef queryRef, long waitMillis, String nanopubId, boolean awaitMaterialization) {
        if (waitMillis < 0) {
            throw new IllegalArgumentException("waitMillis must be non-negative");
        }
        String cacheId = queryRef.getAsUrlString();
        forcedRefresh.add(cacheId);
        long timeNow = System.currentTimeMillis();
        runAfter.put(cacheId, timeNow + waitMillis);
        if (nanopubId != null) {
            publishRefreshes.put(cacheId, new PublishRefresh(nanopubId, awaitMaterialization,
                    timeNow + PUBLISH_REFRESH_MAX_WAIT_MS));
        }
    }

}
