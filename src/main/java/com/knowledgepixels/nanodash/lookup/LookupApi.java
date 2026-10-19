package com.knowledgepixels.nanodash.lookup;

import java.util.List;
import java.util.Map;

/**
 * A kind of API that the possible values of a guided-choice placeholder can be looked up in,
 * as given by {@code nt:possibleValuesFromApi}.
 */
@FunctionalInterface
public interface LookupApi {

    /**
     * Looks up the values that match a search term.
     *
     * @param apiUrl     the API URL as given in the template
     * @param searchTerm the search term
     * @param labels     the map to add the labels of the found values to
     * @param values     the list to add the found values to, in the order the API gives them
     * @throws Exception if the API cannot be reached or its answer cannot be read
     */
    void lookUp(String apiUrl, String searchTerm, Map<String, String> labels, List<String> values) throws Exception;

}
