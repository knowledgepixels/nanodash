package com.knowledgepixels.nanodash.events;

/**
 * A class representing the context of a refresh event.
 */
public enum RefreshContext {
    RESOURCE,
    QUERY;

    /**
     * Parse a string value into a RefreshContext enum.
     *
     * @param value the string value to parse
     * @return the corresponding RefreshContext enum, or null if the value is invalid
     */
    public static RefreshContext parse(String value) {
        return switch (value) {
            case "refresh:resource" -> RESOURCE;
            case "refresh:query" -> QUERY;
            default -> null;
        };
    }


    @Override
    public String toString() {
        return switch (this) {
            case RESOURCE -> "refresh:resource";
            case QUERY -> "refresh:query";
        };
    }

}
