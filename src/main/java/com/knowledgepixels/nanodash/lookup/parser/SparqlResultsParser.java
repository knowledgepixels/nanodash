package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.List;
import java.util.Map;

/**
 * Parser for SPARQL JSON results, as returned by grlc APIs and SPARQL endpoints. The values
 * are read from the {@code thing} binding and their labels from the {@code label} binding.
 */
public class SparqlResultsParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        parse(new JSONObject(response), labels, values);
    }

    /**
     * Reads the values and their labels from SPARQL JSON results.
     *
     * @param results the SPARQL JSON results
     * @param labels  the map to add the labels of the found values to
     * @param values  the list to add the found values to
     */
    public static void parse(JSONObject results, Map<String, String> labels, List<String> values) {
        JSONArray bindings = results.getJSONObject("results").getJSONArray("bindings");
        for (int i = 0; i < bindings.length(); i++) {
            JSONObject binding = bindings.getJSONObject(i);
            String value = binding.getJSONObject("thing").getString("value");
            values.add(value);
            labels.put(value, binding.getJSONObject("label").getString("value"));
        }
    }

}
