package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Parser for the search API of the EBI Ontology Lookup Service, e.g.
 * {@code https://www.ebi.ac.uk/ols/api/select?q=}. The label of a term is followed by the
 * first line of its description, when it has one.
 */
public class OlsParser extends JsonLookupApi {

    private static final Logger logger = LoggerFactory.getLogger(OlsParser.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray docs = new JSONObject(response).getJSONObject("response").getJSONArray("docs");
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            String iri = doc.getString("iri");
            addIfNew(iri, labelOf(doc, iri), labels, values);
        }
    }

    private static String labelOf(JSONObject doc, String iri) {
        String label = doc.getString("label");
        try {
            label += " - " + doc.getJSONArray("description").getString(0);
        } catch (Exception ex) {
            logger.error("No description found for {}", iri, ex);
        }
        return label;
    }

}
