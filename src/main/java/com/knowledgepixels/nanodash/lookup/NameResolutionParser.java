package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Parser for answers that map each identifier to a list of names, the first of which is its
 * label and the second its description: {@code {"MONDO:0005148": ["type 2 diabetes", "..."]}}.
 * This is the format of the RENCI name resolution service,
 * {@code https://name-resolution-sri.renci.org/lookup?string=}. Identifiers given as CURIEs are
 * turned into identifiers.org URIs.
 */
public class NameResolutionParser extends JsonLookupApi {

    private static final int MAX_DESCRIPTION_LENGTH = 80;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONObject json = new JSONObject(response);
        for (String identifier : json.keySet()) {
            if (!(json.get(identifier) instanceof JSONArray names)) continue;
            String uri = toUri(identifier);
            values.add(uri);
            labels.put(uri, labelOf(names));
        }
    }

    private static String labelOf(JSONArray names) {
        String label = names.length() > 0 ? names.getString(0) : "";
        String description = names.length() > 1 ? shorten(names.getString(1)) : "";
        if (!label.isEmpty() && !description.isEmpty()) description = " - " + description;
        return label + description;
    }

    private static String shorten(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) return description;
        return description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }

    private static String toUri(String identifier) {
        if (identifier.startsWith("http://") || identifier.startsWith("https://")) return identifier;
        return "https://identifiers.org/" + identifier;
    }

}
