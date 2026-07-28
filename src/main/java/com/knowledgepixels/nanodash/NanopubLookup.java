package com.knowledgepixels.nanodash;

import net.trustyuri.TrustyUriUtils;
import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The outcome of looking up a nanopublication by its identifier.
 * <p>
 * Retrieving a nanopublication can fail in ways that mean very different things to the
 * user, and that should therefore be shown differently: a mistyped or truncated
 * identifier is a problem with the request, whereas a well-formed identifier that
 * doesn't resolve simply means the nanopublication isn't (yet) on the network. The
 * latter includes the case where the lookup takes too long: the network is queried
 * registry by registry, so a lookup gets a time budget and is reported as unresolved
 * once that is exceeded, rather than leaving the user waiting indefinitely.
 *
 * @see com.knowledgepixels.nanodash.page.NanopubNotFoundPage
 */
public class NanopubLookup {

    private static final Logger logger = LoggerFactory.getLogger(NanopubLookup.class);

    /**
     * Time budget for a single lookup, in milliseconds. The retrieval itself keeps
     * running in the background when this is exceeded, so its result is available
     * from the cache if the user tries again.
     */
    public static final long DEFAULT_TIMEOUT_MS = 15_000;

    /**
     * A last path segment that is apparently an attempt at an RDF-module artifact code
     * (starts with "RA", made up of artifact-code characters, roughly the right length)
     * without being a valid one — typically a truncated or mistyped copy-paste. A valid
     * code is "RA" followed by exactly 43 characters.
     */
    private static final Pattern APPARENT_ARTIFACT_CODE = Pattern.compile("^RA[A-Za-z0-9\\-_]{28,58}$");

    /**
     * Why a lookup did or didn't produce a nanopublication.
     */
    public enum Status {

        /**
         * The nanopublication was retrieved.
         */
        FOUND,

        /**
         * The given identifier is not a well-formed nanopublication (trusty) URI, so it
         * cannot refer to any nanopublication at all.
         */
        INVALID_ID,

        /**
         * The identifier is well-formed, but no nanopublication with it was found on the
         * network.
         */
        NOT_FOUND,

        /**
         * The identifier is well-formed, but the network didn't answer within the time
         * budget. The nanopublication may or may not exist.
         */
        TIMEOUT

    }

    private final Status status;
    private final Nanopub nanopub;
    private final String id;
    private final String errorMessage;

    private NanopubLookup(Status status, Nanopub nanopub, String id, String errorMessage) {
        this.status = status;
        this.nanopub = nanopub;
        this.id = id;
        this.errorMessage = errorMessage;
    }

    /**
     * Wraps an already-available nanopublication as a successful lookup, for callers that
     * got hold of it by other means than a network lookup.
     *
     * @param nanopub the nanopublication
     * @return a lookup with status {@link Status#FOUND}
     */
    public static NanopubLookup found(Nanopub nanopub) {
        return new NanopubLookup(Status.FOUND, nanopub, nanopub.getUri().stringValue(), null);
    }

    /**
     * Looks up the nanopublication with the given identifier, using the default time
     * budget.
     *
     * @param id the nanopublication identifier (trusty URI or artifact code)
     * @return the outcome of the lookup, never null
     */
    public static NanopubLookup lookUp(String id) {
        return lookUp(id, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Looks up the nanopublication with the given identifier.
     *
     * @param id        the nanopublication identifier (trusty URI or artifact code)
     * @param timeoutMs how long to wait for the network before giving up, in milliseconds
     * @return the outcome of the lookup, never null
     */
    public static NanopubLookup lookUp(String id, long timeoutMs) {
        return lookUp(id, timeoutMs, Utils::getNanopub);
    }

    /**
     * Looks up the nanopublication with the given identifier, using the given retrieval
     * function instead of the default one. For callers that fetch nanopublications in
     * their own way, and for tests, which must not depend on the network.
     *
     * @param id        the nanopublication identifier (trusty URI or artifact code)
     * @param timeoutMs how long to wait for the retrieval before giving up, in milliseconds
     * @param retriever fetches the nanopublication for an identifier, returning null if
     *                  there is none
     * @return the outcome of the lookup, never null
     */
    public static NanopubLookup lookUp(String id, long timeoutMs, Function<String, Nanopub> retriever) {
        if (id == null || id.isBlank()) {
            return new NanopubLookup(Status.INVALID_ID, null, id, "No nanopublication identifier was given.");
        }
        if (!isPotentialNanopubId(id)) {
            return new NanopubLookup(Status.INVALID_ID, null, id,
                    "'" + id + "' is not a valid nanopublication identifier. Such an identifier ends in a trusty URI" +
                            " artifact code, i.e. 'RA' followed by 43 letters, digits, hyphens or underscores.");
        }
        Future<Nanopub> retrieval = NanodashThreadPool.submit(() -> retriever.apply(id));
        try {
            Nanopub np = retrieval.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (np == null) {
                return new NanopubLookup(Status.NOT_FOUND, null, id, null);
            }
            return new NanopubLookup(Status.FOUND, np, id, null);
        } catch (TimeoutException ex) {
            // Deliberately not cancelled: the retrieval fills the nanopub cache when it
            // eventually completes, so a retry by the user can be answered right away.
            logger.info("Retrieving nanopublication timed out after {} ms: {}", timeoutMs, id);
            return new NanopubLookup(Status.TIMEOUT, null, id, null);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IllegalArgumentException) {
                logger.info("Not a nanopublication identifier: {}", id, cause);
                return new NanopubLookup(Status.INVALID_ID, null, id,
                        "'" + id + "' is not a valid nanopublication identifier: " + cause.getMessage());
            }
            logger.error("Error while retrieving nanopublication: {}", id, cause);
            return new NanopubLookup(Status.NOT_FOUND, null, id, null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while retrieving nanopublication: {}", id);
            return new NanopubLookup(Status.TIMEOUT, null, id, null);
        }
    }

    /**
     * Checks whether the given identifier could denote a nanopublication, i.e. whether it
     * ends in a valid trusty URI artifact code. This says nothing about whether such a
     * nanopublication exists.
     *
     * @param id the identifier to check
     * @return true if the identifier is a well-formed nanopublication identifier
     */
    public static boolean isPotentialNanopubId(String id) {
        if (id == null || id.isBlank()) return false;
        return TrustyUriUtils.isPotentialTrustyUri(id);
    }

    /**
     * Checks whether the given identifier is apparently meant to be a nanopublication
     * identifier without being a valid one, e.g. a trusty URI that lost characters on the
     * way. Used to tell a broken nanopublication link apart from an ordinary (non-trusty)
     * term URI, which is a perfectly valid thing to look up.
     *
     * @param id the identifier to check
     * @return true if the identifier looks like a malformed nanopublication identifier
     */
    public static boolean looksLikeMalformedNanopubId(String id) {
        if (id == null || id.isBlank() || isPotentialNanopubId(id)) return false;
        String lastSegment = id.replaceFirst("^.*[/#]", "");
        return APPARENT_ARTIFACT_CODE.matcher(lastSegment).matches();
    }

    /**
     * Returns why the lookup did or didn't produce a nanopublication.
     *
     * @return the status, never null
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Checks whether the nanopublication was found.
     *
     * @return true if the nanopublication was retrieved
     */
    public boolean isFound() {
        return status == Status.FOUND;
    }

    /**
     * Returns the retrieved nanopublication.
     *
     * @return the nanopublication, or null if it wasn't found
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the identifier this lookup was made for.
     *
     * @return the identifier, as given by the caller
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a message explaining what is wrong with the identifier.
     *
     * @return the message, or null if the identifier itself is fine
     */
    public String getErrorMessage() {
        return errorMessage;
    }

}
