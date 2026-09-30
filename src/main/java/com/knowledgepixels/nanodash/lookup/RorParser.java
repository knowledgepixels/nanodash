package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Parser for the organization search of the Research Organization Registry, e.g.
 * {@code https://api.ror.org/organizations?query=}.
 */
public class RorParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray items = new JSONObject(response).getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            JSONArray names = item.getJSONArray("names");
            if (names.length() == 0) continue;
            addIfNew(item.getString("id"), displayName(names), labels, values);
        }
    }

    /**
     * Picks the name to display for a ROR record. The ROR API does not order the names by
     * display preference, so the entry flagged "ror_display" is used, falling back to a
     * "label" entry and finally to the first name given.
     *
     * @param names the "names" array of a ROR record, not empty
     * @return the name to show to the user
     */
    private static String displayName(JSONArray names) {
        String labelName = null;
        for (int i = 0; i < names.length(); i++) {
            JSONObject name = names.getJSONObject(i);
            JSONArray types = name.optJSONArray("types");
            if (types == null) continue;
            for (int j = 0; j < types.length(); j++) {
                String type = types.getString(j);
                if ("ror_display".equals(type)) return name.getString("value");
                if ("label".equals(type) && labelName == null) labelName = name.getString("value");
            }
        }
        if (labelName != null) return labelName;
        return names.getJSONObject(0).getString("value");
    }

}
