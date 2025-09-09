package com.knowledgepixels.nanodash;

/**
 * Represents a release of the Nanodash application.
 * This class is used to deserialize JSON data from the GitHub API.
 */
public class NanodashRelease {

    private String tag_name;
    private String name;

    /**
     * Gets the name of the release.
     *
     * @return The name of the release.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the tag name of the release.
     *
     * @return The tag name of the release.
     */
    public String getTagName() {
        return tag_name;
    }

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
