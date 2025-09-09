package com.knowledgepixels.nanodash;

import java.io.IOException;

/**
 * Exception thrown when an HTTP request returns a non-200 status code.
 * Includes the status code in the exception message.
 */
public class HttpStatusException extends IOException {

    /**
     * Constructs a new HttpStatusException with the specified status code.
     *
     * @param statusCode the HTTP status code returned by the request
     */
    public HttpStatusException(int statusCode) {
        super("HTTP error: " + statusCode);
    }

}
