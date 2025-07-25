package com.knowledgepixels.nanodash.template;

/**
 * This exception is thrown when a template is found to be malformed.
 */
public class MalformedTemplateException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MalformedTemplateException with the specified detail message.
     *
     * @param message the detail message, which provides more information about the exception.
     */
    public MalformedTemplateException(String message) {
        super(message);
    }

}
