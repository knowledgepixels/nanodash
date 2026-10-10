package com.knowledgepixels.nanodash.lookup.parser;

import com.github.openjson.JSONObject;
import com.knowledgepixels.nanodash.lookup.JsonLookupApi;

import java.util.List;
import java.util.Map;

/**
 * Parser for the OpenAIRE Graph API, e.g. {@code https://api.openaire.eu/graph/v1/projects?search=}.
 * A value is the API URL of the record, and its label is taken from the first field the kind
 * of record names itself by.
 */
public class OpenAireParser extends JsonLookupApi {

    private static final String GRAPH_API = "https://api.openaire.eu/graph/";

    private static final String[] NAME_FIELDS = {"mainTitle", "legalShortName", "officialName", "title"};

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        String recordType = apiUrl.replaceFirst("^https://api\\.openaire\\.eu/graph/(v[0-9]+/[^/#?]+).*$", "$1");
        for (Object result : new JSONObject(response).getJSONArray("results")) {
            if (result instanceof JSONObject record) {
                String uri = GRAPH_API + recordType + "/" + record.getString("id");
                addIfNew(uri, labelOf(record, uri), labels, values);
            }
        }
    }

    private static String labelOf(JSONObject record, String uri) {
        for (String field : NAME_FIELDS) {
            if (record.has(field)) return record.getString(field);
        }
        if (record.has("familyName")) {
            String familyName = record.getString("familyName");
            return record.has("givenName") ? record.getString("givenName") + " " + familyName : familyName;
        }
        return uri;
    }

}
