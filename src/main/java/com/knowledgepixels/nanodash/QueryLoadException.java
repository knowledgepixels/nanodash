package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.page.ErrorPage;

/**
 * Thrown when a query cannot be loaded: its identifier doesn't denote a query, the
 * nanopublication holding it can't be retrieved, or the SPARQL code it carries is not
 * valid.
 * <p>
 * The message is written for the user rather than for the log, because it ends up on the
 * {@link ErrorPage} that a request for a query that can't be loaded is answered with. It
 * should therefore say what is wrong with the query in plain words, instead of exposing
 * internals. The {@link #getKind() kind} says whose problem it is, which that page turns
 * into what it tells the user to do about it.
 *
 * @see GrlcQuery#load(String)
 */
public class QueryLoadException extends RuntimeException {

    private final ErrorPage.Kind kind;

    /**
     * Constructs a new QueryLoadException with the given user-facing message.
     *
     * @param message explains, in plain words, why the query couldn't be loaded
     * @param kind    whose problem this is: the asking user's, the query author's, or Nanodash's
     */
    public QueryLoadException(String message, ErrorPage.Kind kind) {
        super(message);
        this.kind = kind;
    }

    /**
     * Constructs a new QueryLoadException with the given user-facing message and the
     * underlying failure.
     *
     * @param message explains, in plain words, why the query couldn't be loaded
     * @param kind    whose problem this is: the asking user's, the query author's, or Nanodash's
     * @param cause   the failure this message was derived from, kept for the log
     */
    public QueryLoadException(String message, ErrorPage.Kind kind, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Whose problem this is, and with that what the user can do about it.
     *
     * @return the kind of error
     */
    public ErrorPage.Kind getKind() {
        return kind;
    }

}
