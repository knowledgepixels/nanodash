package com.knowledgepixels.nanodash.domain;

import org.nanopub.extra.services.ApiResponseEntry;

import java.util.HashSet;
import java.util.Set;

/**
 * Factory class for creating or retrieving MaintainedResource instances based on API response entries and associated spaces.
 */
public class MaintainedResourceFactory {

    /**
     * Retrieves an existing MaintainedResource instance based on the API response entry and associated space, or creates a new one if it doesn't exist.
     *
     * @param apiResponseEntry The API response entry containing maintained resource data.
     * @param space            The associated Space instance for the maintained resource.
     * @return A MaintainedResource instance initialized with the data from the API response entry and associated space.
     */
    public static MaintainedResource getOrCreate(ApiResponseEntry apiResponseEntry, Space space) {
        String id = apiResponseEntry.get("resource");
        AbstractResourceWithProfile existing = AbstractResourceWithProfile.get(id);
        if (existing instanceof MaintainedResource resource) {
            return resource;
        }
        return new MaintainedResource(apiResponseEntry, space);
    }

    /**
     * Removes MaintainedResource instances that are no longer active based on the provided set of active IDs.
     *
     * @param activeIds A set of active maintained resource IDs that should be retained. Any MaintainedResource instance with an ID not in this set will be removed.
     */
    public static void removeStale(Set<String> activeIds) {
        new HashSet<>(AbstractResourceWithProfile.getInstances(MaintainedResource.class).keySet())
                .stream()
                .filter(id -> !activeIds.contains(id))
                .forEach(id -> AbstractResourceWithProfile.removeInstance(MaintainedResource.class, id));
    }

}
