package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.List;
import java.util.Map;

/**
 * Parser for the species suggestion API of GBIF, e.g.
 * {@code https://api.gbif.org/v1/species/suggest?q=}. A value is the GBIF page of the species.
 */
public class GbifParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray species = new JSONArray(response);
        for (int i = 0; i < species.length(); i++) {
            JSONObject entry = species.getJSONObject(i);
            String uri = "https://www.gbif.org/species/" + entry.getString("key");
            addIfNew(uri, entry.getString("scientificName"), labels, values);
        }
    }

}
