package com.knowledgepixels.nanodash;

/**
 * Thrown when a query cannot be loaded: its identifier doesn't denote a query, the
 * nanopublication holding it can't be retrieved, or the SPARQL code it carries is not
 * valid.
 * <p>
 * The message is written for the user rather than for the log, because it ends up on the
 * {@link com.knowledgepixels.nanodash.page.ErrorPage} that a request for a query that
 * can't be loaded is answered with. It should therefore say what is wrong with the query
 * in plain words, instead of exposing internals.
 *
 * @see GrlcQuery#load(String)
 */
public class QueryLoadException extends RuntimeException {

    /**
     * Constructs a new QueryLoadException with the given user-facing message.
     *
     * @param message explains, in plain words, why the query couldn't be loaded
     */
    public QueryLoadException(String message) {
        super(message);
    }

    /**
     * Constructs a new QueryLoadException with the given user-facing message and the
     * underlying failure.
     *
     * @param message explains, in plain words, why the query couldn't be loaded
     * @param cause   the failure this message was derived from, kept for the log
     */
    public QueryLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}
