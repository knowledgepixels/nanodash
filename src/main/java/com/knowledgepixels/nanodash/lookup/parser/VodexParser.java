package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;
import com.knowledgepixels.nanodash.lookup.SearchTerms;

import java.util.List;
import java.util.Map;

/**
 * Parser for the Solr-based vocabulary indexes of Vodex, e.g.
 * {@code https://vodex.petapico.org/ncbitaxon/query?q=label:}. The search term is sent as a
 * full-text query matching all of its words.
 */
public class VodexParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String prepareSearchTerm(String searchTerm) {
        return SearchTerms.expand(searchTerm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray docs = new JSONObject(response).getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            addIfNew(doc.getString("id"), doc.getJSONArray("label").get(0).toString(), labels, values);
        }
    }

}
