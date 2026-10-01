package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.List;
import java.util.Map;

/**
 * Parser for the search API of the EBI Ontology Lookup Service, e.g.
 * {@code https://www.ebi.ac.uk/ols/api/select?q=}. The label of a term is followed by the
 * first line of its description, when it has one.
 */
public class OlsParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray docs = new JSONObject(response).getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            String iri = doc.getString("iri");
            addIfNew(iri, labelOf(doc), labels, values);
        }
    }

    private static String labelOf(JSONObject doc) {
        String label = doc.getString("label");
        JSONArray description = doc.optJSONArray("description");
        if (description == null || description.length() == 0) return label;
        return label + " - " + description.getString(0);
    }

}
