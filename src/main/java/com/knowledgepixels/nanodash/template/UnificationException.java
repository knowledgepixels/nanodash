package com.knowledgepixels.nanodash.template;

/**
 * UnificationException is thrown when there is an error during the unification process.
 */
public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new UnificationException with the specified detail message.
     *
     * @param message the detail message, which is saved for later retrieval by the
     */
    public UnificationException(String message) {
        super(message);
    }

}
