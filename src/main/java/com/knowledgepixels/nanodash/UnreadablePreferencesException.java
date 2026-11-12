package com.knowledgepixels.nanodash;

import java.io.File;

/**
 * Thrown when the preferences file exists but can't be read, so that nanodash doesn't run with
 * the defaults in place of the settings in the file (issue #905).
 */
public class UnreadablePreferencesException extends IllegalStateException {

    /**
     * Create the exception for a preferences file that couldn't be read.
     *
     * @param prefFile the preferences file
     * @param cause    why reading it failed
     */
    public UnreadablePreferencesException(File prefFile, Throwable cause) {
        super("Could not read preferences file at '" + prefFile + "': " + cause.getMessage()
                + ". Fix or remove the file and restart nanodash.", cause);
    }

}
