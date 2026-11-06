package com.knowledgepixels.nanodash;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.core.request.handler.IPageClassRequestHandler;
import org.apache.wicket.core.request.handler.ListenerRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Logs how long nanodash spends on its queries, its page requests and the shared data it
 * builds, as one INFO line of {@code key=value} pairs per event:
 * <pre>
 * Query fetch stats: query=RA…/get-all-user-intros status=ok thread=background durationMs=8123
 * Request stats: page=SpacePage kind=render durationMs=212 queryWaitMs=0 retrievals=8350 inlineFetches=0 inlineFetchMs=0
 * Data build stats: step=user-data trigger=initial complete=true durationMs=11800
 * </pre>
 * The format is stable on purpose: the Grafana dashboards read these lines from Loki and take
 * the fields apart with LogQL, the same way they read nanopub-query's {@code Query stats:}
 * lines. Synchronous cache retrievals are only counted per request, not logged one by one: a
 * page can make thousands of them, and a line for each would slow the page down noticeably.
 */
public final class TimingStats {

    private static final Logger logger = LoggerFactory.getLogger(TimingStats.class);

    private static final MetaDataKey<RequestTally> REQUEST_TALLY = new MetaDataKey<>() {
    };

    private TimingStats() {
    }

    /**
     * Records one fetch of a query from the query service.
     *
     * @param cacheId   the cache id of the query (its URL string)
     * @param millis    how long the fetch took
     * @param succeeded whether the fetch returned a result
     */
    static void recordFetch(String cacheId, long millis, boolean succeeded) {
        RequestTally tally = currentTally();
        if (tally != null) {
            tally.inlineFetches++;
            tally.inlineFetchMillis += millis;
        }
        logger.info(fetchLine(cacheId, succeeded, tally != null, millis));
    }

    /**
     * Records one synchronous retrieval from {@link ApiCache}, whether it was answered from the
     * cache or had to wait for a fetch, in the tally of the request being served.
     *
     * @param millis how long the caller was blocked
     */
    static void recordSyncRetrieval(long millis) {
        RequestTally tally = currentTally();
        if (tally != null) {
            tally.syncRetrievals++;
            tally.syncRetrievalMillis += millis;
        }
    }

    /**
     * Builds a piece of shared data and records how long that took.
     *
     * @param step     what is built, e.g. {@code user-data}
     * @param trigger  why it is built, e.g. {@code initial} or {@code refresh}
     * @param build    the build, returning whether the result is complete
     * @return whether the result is complete
     */
    public static boolean timeDataBuild(String step, String trigger, Supplier<Boolean> build) {
        long start = System.nanoTime();
        boolean complete = false;
        try {
            complete = build.get();
            return complete;
        } finally {
            logger.info(dataBuildLine(step, trigger, complete, millisSince(start)));
        }
    }

    /**
     * Creates the request listener that keeps each page request's tally and logs it.
     *
     * @return the listener, to be added to the application's request cycle listeners
     */
    public static IRequestCycleListener requestListener() {
        return new RequestStatsListener();
    }

    /**
     * The query part of a cache id: the query's id without its parameters.
     *
     * @param cacheId the cache id (the query's URL string)
     * @return the query id
     */
    static String queryOf(String cacheId) {
        int parameters = cacheId.indexOf('?');
        return parameters < 0 ? cacheId : cacheId.substring(0, parameters);
    }

    /**
     * The stats line of one query fetch.
     *
     * @param cacheId         the cache id of the query
     * @param succeeded       whether the fetch returned a result
     * @param onRequestThread whether a request thread waited for it
     * @param millis          how long the fetch took
     * @return the line
     */
    static String fetchLine(String cacheId, boolean succeeded, boolean onRequestThread, long millis) {
        return "Query fetch stats: query=" + queryOf(cacheId)
                + " status=" + (succeeded ? "ok" : "failed")
                + " thread=" + (onRequestThread ? "request" : "background")
                + " durationMs=" + millis;
    }

    /**
     * The stats line of one page request.
     *
     * @param page                the simple name of the page class
     * @param kind                {@code render}, {@code ajax} or {@code listener}
     * @param millis              how long the request took
     * @param syncRetrievalMillis how long it waited on synchronous retrievals
     * @param syncRetrievals      how many synchronous retrievals it made
     * @param inlineFetches       how many of those had to fetch on the request thread
     * @param inlineFetchMillis   how long those fetches took
     * @return the line
     */
    static String requestLine(String page, String kind, long millis, long syncRetrievalMillis,
                              int syncRetrievals, int inlineFetches, long inlineFetchMillis) {
        return "Request stats: page=" + page + " kind=" + kind + " durationMs=" + millis
                + " queryWaitMs=" + syncRetrievalMillis + " retrievals=" + syncRetrievals
                + " inlineFetches=" + inlineFetches + " inlineFetchMs=" + inlineFetchMillis;
    }

    /**
     * The stats line of one build of shared data.
     *
     * @param step     what was built
     * @param trigger  why it was built
     * @param complete whether the result is complete
     * @param millis   how long the build took
     * @return the line
     */
    static String dataBuildLine(String step, String trigger, boolean complete, long millis) {
        return "Data build stats: step=" + step + " trigger=" + trigger
                + " complete=" + complete + " durationMs=" + millis;
    }

    /**
     * The time since a {@link System#nanoTime()} reading.
     *
     * @param startNanos the earlier reading
     * @return the elapsed time in milliseconds
     */
    static long millisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /**
     * The tally of the request the current thread is serving.
     *
     * @return the tally, or {@code null} on a thread that serves no request
     */
    private static RequestTally currentTally() {
        RequestCycle cycle = RequestCycle.get();
        return cycle == null ? null : cycle.getMetaData(REQUEST_TALLY);
    }

    /**
     * What one request spent on queries, and which page it was for.
     */
    private static final class RequestTally {

        private final long startNanos = System.nanoTime();
        private String page;
        private String kind;
        private int syncRetrievals;
        private long syncRetrievalMillis;
        private int inlineFetches;
        private long inlineFetchMillis;
    }

    /**
     * Starts a tally at the beginning of each request, notes the page it is for, and logs it at
     * the end. Requests that are not for a page, such as static resources, are not logged.
     */
    private static final class RequestStatsListener implements IRequestCycleListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeginRequest(RequestCycle cycle) {
            cycle.setMetaData(REQUEST_TALLY, new RequestTally());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler) {
            RequestTally tally = cycle.getMetaData(REQUEST_TALLY);
            if (tally == null || tally.page != null || !(handler instanceof IPageClassRequestHandler pageHandler)) {
                return;
            }
            tally.page = pageHandler.getPageClass().getSimpleName();
            tally.kind = kindOf(cycle, handler);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEndRequest(RequestCycle cycle) {
            RequestTally tally = cycle.getMetaData(REQUEST_TALLY);
            if (tally == null || tally.page == null) {
                return;
            }
            logger.info(requestLine(tally.page, tally.kind, millisSince(tally.startNanos),
                    tally.syncRetrievalMillis, tally.syncRetrievals, tally.inlineFetches, tally.inlineFetchMillis));
        }

        /**
         * What kind of page request a handler serves.
         *
         * @param cycle   the request cycle
         * @param handler the handler resolved for the request
         * @return {@code ajax} for an Ajax listener call, {@code listener} for another listener
         * call, and {@code render} otherwise
         */
        private static String kindOf(RequestCycle cycle, IRequestHandler handler) {
            if (!(handler instanceof ListenerRequestHandler)) {
                return "render";
            }
            boolean ajax = cycle.getRequest() instanceof WebRequest webRequest && webRequest.isAjax();
            return ajax ? "ajax" : "listener";
        }
    }
}
