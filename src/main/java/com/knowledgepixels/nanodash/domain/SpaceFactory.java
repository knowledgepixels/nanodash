package com.knowledgepixels.nanodash.domain;

import org.nanopub.extra.services.ApiResponseEntry;

/**
 * Factory class for creating Space instances from API response entries.
 */
public class SpaceFactory {

    /**
     * Creates a Space instance from the given API response entry.
     *
     * @param apiResponseEntry The API response entry containing space data.
     * @return A new Space instance initialized with the data from the API response entry.
     */
    public static Space create(ApiResponseEntry apiResponseEntry) {
        return new Space(apiResponseEntry);
    }

}
