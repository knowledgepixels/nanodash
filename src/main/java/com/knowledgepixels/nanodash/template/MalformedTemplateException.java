package com.knowledgepixels.nanodash.template;

/**
 * This exception is thrown when a template is found to be malformed.
 */
public class MalformedTemplateException extends Exception {

    /**
     * Constructs a new MalformedTemplateException with the specified detail message.
     *
     * @param message the detail message, which provides more information about the exception.
     */
    public MalformedTemplateException(String message) {
        super(message);
    }

}
