package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Parser for APIs that have no parser of their own. The answer is a JSON object whose array
 * fields hold the results, e.g. {@code {"results": [{"uri": "...", "label": "...", "description": "..."}]}}.
 * A result's value is taken from its {@code @id}, {@code concepturi} or {@code uri} field, its
 * label from {@code prefLabel} or {@code label}, and its description, shown after the label,
 * from {@code definition} or {@code description}. At most ten results are read.
 */
public class CanonicalParser extends JsonLookupApi {

    private static final int MAX_RESULTS = 10;

    private static final String[] VALUE_FIELDS = {"@id", "concepturi", "uri"};

    private static final String[] LABEL_FIELDS = {"prefLabel", "label"};

    private static final String[] DESCRIPTION_FIELDS = {"definition", "description"};

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONObject json = new JSONObject(response);
        for (String key : json.keySet()) {
            if (values.size() >= MAX_RESULTS) return;
            if (json.get(key) instanceof JSONArray results) {
                parseResults(results, labels, values);
            }
        }
    }

    private static void parseResults(JSONArray results, Map<String, String> labels, List<String> values) {
        for (int i = 0; i < results.length(); i++) {
            if (values.size() >= MAX_RESULTS) return;
            if (!(results.get(i) instanceof JSONObject result)) continue;
            String value = firstOf(result, VALUE_FIELDS);
            if (value == null) continue;
            values.add(value);
            labels.put(value, labelOf(result));
        }
    }

    private static String labelOf(JSONObject result) {
        String label = firstOf(result, LABEL_FIELDS);
        label = label == null ? "" : label.replaceAll(" - ", " -- ");
        String description = firstOf(result, DESCRIPTION_FIELDS);
        if (description == null) description = "";
        if (!label.isEmpty() && !description.isEmpty()) description = " - " + description;
        return label + description;
    }

    private static String firstOf(JSONObject result, String[] fields) {
        for (String field : fields) {
            if (result.has(field)) return result.get(field).toString();
        }
        return null;
    }

}
