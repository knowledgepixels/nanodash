package com.knowledgepixels.nanodash.domain;

import org.nanopub.extra.services.ApiResponseEntry;

import java.util.HashSet;
import java.util.Set;

/**
 * Factory class for creating Space instances from API response entries.
 */
public class SpaceFactory {

    private SpaceFactory() {
    }

    /**
     * Retrieves an existing Space instance based on the API response entry or creates a new one if it doesn't exist.
     *
     * @param apiResponseEntry The API response entry containing space data.
     * @return A new Space instance initialized with the data from the API response entry.
     */
    public static Space getOrCreate(ApiResponseEntry apiResponseEntry) {
        String id = apiResponseEntry.get("space");
        AbstractResourceWithProfile existing = AbstractResourceWithProfile.get(id);
        if (existing instanceof Space space) {
            space.updateFromApi(apiResponseEntry);
            return space;
        }
        return new Space(apiResponseEntry);
    }

    /**
     * Removes Space instances that are no longer active based on the provided set of active IDs.
     *
     * @param activeIds A set of active space IDs that should be retained. Any Space instance with an ID not in this set will be removed.
     */
    public static void removeStale(Set<String> activeIds) {
        new HashSet<>(AbstractResourceWithProfile.getInstances(Space.class).keySet())
                .stream()
                .filter(id -> !activeIds.contains(id))
                .forEach(id -> AbstractResourceWithProfile.removeInstance(Space.class, id));
    }

}
