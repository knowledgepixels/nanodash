package com.knowledgepixels.nanodash;

/**
 * Represents a release of the Nanodash application.
 *
 * @param tag_name The tag name of the release (e.g., "nanodash-1.0.0").
 * @param name     The name of the release.
 */
public record NanodashRelease(String tag_name, String name) {

    /**
     * Extracts and returns the version number from the tag name.
     * The tag name is expected to be in the format "nanodash-x.y.z".
     *
     * @return The version number as a string.
     */
    public String getVersionNumber() {
        return tag_name.replaceFirst("nanodash-", "");
    }

}
