package com.knowledgepixels.nanodash;

import org.apache.http.client.methods.HttpGet;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.server.RegistryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells whether the services this instance is connected to are local/private ones, i.e. instances
 * that accept and serve nanopublications typed {@code npx:ProtectedNanopub} instead of rejecting
 * them. Such content is not part of the public network, so it is worth saying so in the interface
 * rather than letting it look like everything else (issue #671).
 * <p>
 * The Registry reports the mode as {@code isLocalInstance} in its info JSON (from version 1.12.0),
 * the Query service as the response header {@code Nanopub-Query-Local-Instance} (from version
 * 1.26.0). Both are probed once at startup and the answers are kept for the lifetime of the
 * process: pointing an instance at a different service means restarting it anyway, and a request
 * thread must never wait on these two calls.
 */
public class ServiceMode {

    private ServiceMode() {
    }  // no instances allowed

    /**
     * The header a Query service sets, and only sets, when it is configured as a local instance.
     */
    private static final String QUERY_LOCAL_INSTANCE_HEADER = "Nanopub-Query-Local-Instance";

    private static volatile Boolean registryIsLocal;
    private static volatile Boolean queryIsLocal;

    /**
     * Probes both services for their mode. Called at application startup, from where the (possibly
     * slow, possibly failing) requests do not delay any user request.
     */
    public static void init() {
        registryIsLocal = probeRegistry();
        queryIsLocal = probeQuery();
        if (isRestricted()) {
            logger.info("Connected to restricted services (registry local: {}, query local: {}); " +
                        "protected nanopublications are part of this deployment", registryIsLocal, queryIsLocal);
        }
    }

    /**
     * Returns whether this instance is connected to at least one local/private service, and
     * therefore is a deployment in which protected nanopublications can exist.
     *
     * @return true if the registry or the query service reports itself as a local instance
     */
    public static boolean isRestricted() {
        return isRegistryLocal() || isQueryLocal();
    }

    /**
     * Returns whether the main registry reports itself as a local/private instance. A registry
     * older than 1.12.0 does not report the field at all, which is read as "not local".
     *
     * @return true if the registry is a local instance
     */
    public static boolean isRegistryLocal() {
        if (registryIsLocal == null) registryIsLocal = probeRegistry();
        return registryIsLocal;
    }

    /**
     * Returns whether the main query service reports itself as a local/private instance. A query
     * service older than 1.26.0 does not send the header at all, which is read as "not local".
     *
     * @return true if the query service is a local instance
     */
    public static boolean isQueryLocal() {
        if (queryIsLocal == null) queryIsLocal = probeQuery();
        return queryIsLocal;
    }

    private static boolean probeRegistry() {
        String url = Utils.getMainRegistryUrl();
        try {
            return RegistryInfo.load(url).isLocalInstance();
        } catch (Exception ex) {
            // Not knowing the mode is the same as not being restricted: the flag is additional
            // information, and its absence must never keep the interface from working.
            logger.warn("Could not determine the mode of registry {}: {}", url, ex.toString());
            return false;
        }
    }

    private static boolean probeQuery() {
        String url = Utils.getMainQueryUrl();
        try {
            var response = NanopubUtils.getHttpClient().execute(new HttpGet(url));
            try {
                var header = response.getFirstHeader(QUERY_LOCAL_INSTANCE_HEADER);
                return header != null && "true".equalsIgnoreCase(header.getValue());
            } finally {
                org.apache.http.util.EntityUtils.consumeQuietly(response.getEntity());
            }
        } catch (Exception ex) {
            logger.warn("Could not determine the mode of query service {}: {}", url, ex.toString());
            return false;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ServiceMode.class);

}
