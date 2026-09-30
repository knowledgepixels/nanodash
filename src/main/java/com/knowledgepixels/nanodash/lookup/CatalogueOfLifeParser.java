package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Parser for the name usage search of the Catalogue of Life, e.g.
 * {@code https://api.catalogueoflife.org/dataset/3LR/nameusage/search?q=}. A value is the
 * Catalogue of Life page of the taxon.
 */
public class CatalogueOfLifeParser extends JsonLookupApi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parse(String response, String apiUrl, Map<String, String> labels, List<String> values) {
        JSONArray results = new JSONObject(response).getJSONArray("result");
        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            String uri = "https://www.catalogueoflife.org/data/taxon/" + result.getString("id");
            addIfNew(uri, result.getJSONObject("usage").getString("label"), labels, values);
        }
    }

}
