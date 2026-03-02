package com.knowledgepixels.nanodash.domain;

import org.nanopub.extra.services.ApiResponseEntry;

/**
 * Factory class for creating or retrieving MaintainedResource instances based on API response entries and associated spaces.
 */
public class MaintainedResourceFactory {

    public static MaintainedResource getOrCreate(ApiResponseEntry apiResponseEntry, Space space) {
        return new MaintainedResource(apiResponseEntry, space);
    }

}
