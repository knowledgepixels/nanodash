package com.knowledgepixels.nanodash.domain;

import org.nanopub.Nanopub;
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
     * @param rootNanopub      The nanopub of the entry's {@code np}, or null if it could not be
     *                         fetched or is not needed (see {@link #needsRootNanopub}).
     * @return A new Space instance initialized with the data from the API response entry.
     */
    public static Space getOrCreate(ApiResponseEntry apiResponseEntry, Nanopub rootNanopub) {
        String id = apiResponseEntry.get("space");
        AbstractResourceWithProfile existing = AbstractResourceWithProfile.get(id);
        if (existing instanceof Space space) {
            space.updateFromApi(apiResponseEntry, rootNanopub);
            return space;
        }
        return new Space(apiResponseEntry, rootNanopub);
    }

    /**
     * Whether {@link #getOrCreate} needs the root nanopub of the given entry: for a space not
     * built yet, one whose definition changed, and one whose root nanopub could not be
     * fetched before. An unchanged space keeps the nanopub it has.
     *
     * @param spaceId the space's id
     * @param npId    the root nanopub id the entry gives
     * @return true if the nanopub is to be fetched
     */
    public static boolean needsRootNanopub(String spaceId, String npId) {
        return !(AbstractResourceWithProfile.get(spaceId) instanceof Space space)
                || !npId.equals(space.getNanopubId())
                || space.getNanopub() == null;
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
