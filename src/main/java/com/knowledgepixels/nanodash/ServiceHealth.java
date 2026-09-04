package com.knowledgepixels.nanodash;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.server.RegistryInfo;
import org.nanopub.extra.services.QueryCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Watches whether the registry and the query service this instance talks to are in a state in
 * which they can actually answer, so that the interface can say so once instead of letting every
 * query-driven part of a page fail on its own with "API call failed." (issue #681).
 * <p>
 * A service that is still loading is filtered out by the nanopub library before any call is
 * attempted: {@link QueryCall} admits a query instance only when it reports {@code READY} or
 * {@code LOADING_UPDATES}, and {@code ServerIterator} skips a registry that is not {@code ready}
 * or {@code updating}. That state changes while an instance is running — a service that starts
 * with the deployment spends its first minutes loading — so the check is repeated in the
 * background rather than made once at startup, and request threads only read the last answer.
 */
public class ServiceHealth {

    private ServiceHealth() {
    }  // no instances allowed

    /**
     * How a service is doing, from the point of view of a caller that wants an answer from it.
     */
    public enum State {

        /**
         * Calls can be made: the service reports a status the nanopub library accepts.
         */
        HEALTHY,

        /**
         * The service answers but is still loading, so calls to it are not attempted yet. This
         * passes by itself, and the content it would serve is incomplete rather than wrong.
         */
        LOADING,

        /**
         * The service could not be reached at all.
         */
        UNREACHABLE,

        /**
         * Not established yet, or the check itself failed. Treated as "nothing to report": not
         * knowing must not turn into claiming an outage.
         */
        UNKNOWN

    }

    private static final long CHECK_INTERVAL_SECONDS = 30;

    private static volatile State queryState = State.UNKNOWN;
    private static volatile State registryState = State.UNKNOWN;

    private static ScheduledExecutorService scheduler;

    /**
     * Starts the periodic check. Meant to run once at application startup.
     */
    public static synchronized void init() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "nanodash-service-health");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(ServiceHealth::check, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stops the periodic check. Meant to run once at application shutdown.
     */
    public static synchronized void shutdown() {
        if (scheduler == null) return;
        scheduler.shutdownNow();
        scheduler = null;
    }

    /**
     * Returns the state of the query service as of the last check.
     *
     * @return the state of the query service
     */
    public static State getQueryState() {
        return queryState;
    }

    /**
     * Returns the state of the registry as of the last check.
     *
     * @return the state of the registry
     */
    public static State getRegistryState() {
        return registryState;
    }

    /**
     * Returns what to tell the user about the services, or null when there is nothing to say
     * because both are fine or their state is unknown.
     * <p>
     * A service that is loading and one that cannot be reached get different wording on purpose:
     * the first passes by itself and asks the reader to wait, the second is an outage.
     *
     * @return the note to show, or null if there is nothing to report
     */
    public static String getNote() {
        String query = switch (queryState) {
            case LOADING -> "The query service is still loading. Lists and status information are incomplete.";
            case UNREACHABLE -> "The query service cannot be reached. Lists and status information are unavailable.";
            default -> null;
        };
        String registry = switch (registryState) {
            case LOADING -> "The registry is still loading. Some nanopublications cannot be retrieved yet.";
            case UNREACHABLE -> "The registry cannot be reached. Nanopublications cannot be retrieved or published.";
            default -> null;
        };
        if (query == null) return registry;
        if (registry == null) return query;
        return query + " " + registry;
    }

    /**
     * Runs both checks and records their outcome. Package-private so that a test can run a check
     * without waiting for the scheduler.
     */
    static void check() {
        State newQueryState = checkQuery();
        State newRegistryState = checkRegistry();
        if (newQueryState != queryState || newRegistryState != registryState) {
            logger.info("Service health changed: query {} -> {}, registry {} -> {}",
                    queryState, newQueryState, registryState, newRegistryState);
        }
        queryState = newQueryState;
        registryState = newRegistryState;
    }

    private static State checkQuery() {
        try {
            // The authoritative question is not what one instance reports but whether the library
            // would dispatch a call at all, which is what every failing page element ran into.
            if (!QueryCall.getApiInstances().isEmpty()) return State.HEALTHY;
        } catch (Exception ex) {
            logger.debug("No query instance available: {}", ex.toString());
        }
        // None admitted: ask the configured service directly to tell loading from unreachable.
        return stateFromStatusHeader(Utils.getMainQueryUrl(), "Nanopub-Query-Status",
                status -> "READY".equalsIgnoreCase(status) || "LOADING_UPDATES".equalsIgnoreCase(status));
    }

    private static State checkRegistry() {
        String url = Utils.getMainRegistryUrl();
        try {
            String status = RegistryInfo.load(url).getStatus();
            return NanopubServerUtils.isReadyRegistryStatus(status) ? State.HEALTHY : State.LOADING;
        } catch (Exception ex) {
            logger.debug("Could not load registry info from {}: {}", url, ex.toString());
            return State.UNREACHABLE;
        }
    }

    private static State stateFromStatusHeader(String url, String headerName, java.util.function.Predicate<String> isReady) {
        try {
            HttpResponse response = NanopubUtils.getHttpClient().execute(new HttpGet(url));
            try {
                var header = response.getFirstHeader(headerName);
                String status = header == null ? null : header.getValue();
                if (status == null || status.isEmpty()) {
                    // An instance that reports no status at all is an older one, which the library
                    // treats as usable; whatever kept it out of the list is not its loading state.
                    return State.UNKNOWN;
                }
                return isReady.test(status) ? State.HEALTHY : State.LOADING;
            } finally {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Exception ex) {
            logger.debug("Could not reach {}: {}", url, ex.toString());
            return State.UNREACHABLE;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealth.class);

}
