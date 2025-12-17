package com.knowledgepixels.nanodash;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Strings;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.component.QueryParamField;

/**
 * Utility class for APIs look up and parsing.
 */
public class LookupApis {

    private static final Logger logger = LoggerFactory.getLogger(LookupApis.class);

    private LookupApis() {
    }  // no instances allowed

    /**
     * Parses a JSON response from a grlc API for nanopublications and extracts URIs and labels.
     *
     * @param grlcJsonObject the JSON object containing the grlc API response
     * @param labelMap       a map to store URIs and their corresponding labels
     * @param values         a list to store the extracted URIs
     */
    public static void parseNanopubGrlcApi(JSONObject grlcJsonObject, Map<String, String> labelMap, List<String> values) {
        // Aimed to resolve Nanopub grlc API: http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/find_signed_nanopubs_with_text?text=covid
        JSONArray resultsArray = grlcJsonObject.getJSONObject("results").getJSONArray("bindings");
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultObject = resultsArray.getJSONObject(i);
            // Get the nanopub URI
            String uri = resultObject.getJSONObject("thing").getString("value");
            // Get the string which matched with the search term
            String label = resultObject.getJSONObject("label").getString("value");
            values.add(uri);
            labelMap.put(uri, label);
        }
    }

    /**
     * Fetches possible values from an API based on the provided search term.
     *
     * @param apiString  the API endpoint URL to query
     * @param searchterm the search term to use for querying the API
     * @param labelMap   a map to store URIs and their corresponding labels
     * @param values     a list to store the extracted URIs
     */
    public static void getPossibleValues(String apiString, String searchterm, Map<String, String> labelMap, List<String> values) {
        // TODO This method is a mess and needs some serious clean-up and structuring...
        try {
            if (apiString.startsWith("https://w3id.org/np/l/nanopub-query-1.1/api/") || apiString.startsWith("http://purl.org/nanopub/api/find_signed_things?")) {
                String queryName = "find-things";
                if (apiString.startsWith("https://w3id.org/np/l/nanopub-query-1.1/api/")) {
                    queryName = apiString.replace("https://w3id.org/np/l/nanopub-query-1.1/api/", "");
                    if (queryName.contains("?")) queryName = queryName.substring(0, queryName.indexOf("?"));
                }
                Multimap<String, String> params = ArrayListMultimap.create();
                if (apiString.contains("?")) {
                    List<NameValuePair> urlParams = URLEncodedUtils.parse(apiString.substring(apiString.indexOf("?") + 1), StandardCharsets.UTF_8);
                    for (NameValuePair p : urlParams) {
                        params.put(p.getName(), p.getValue());
                    }
                }
                String queryId = queryName;
                if (!queryName.contains("/")) {
                    queryId = QueryApiAccess.getQueryId(queryName);
                }
                GrlcQuery q = GrlcQuery.get(queryId);
                if (q.getEndpoint().stringValue().endsWith("/text")) {
                    searchterm = expandSearchTerm(searchterm);
                }
                String queryParamName = "query";
                if (q.getPlaceholdersList().size() == 1) {
                    queryParamName = QueryParamField.getParamName(q.getPlaceholdersList().get(0));
                }
                params.put(queryParamName, searchterm);
                ApiResponse result = ApiCache.retrieveResponseSync(new QueryRef(queryName, params), false);
                int count = 0;
                for (ApiResponseEntry r : result.getData()) {
                    String uri = r.get("thing");
                    values.add(uri);
                    String desc = r.get("description");
                    if (desc == null) desc = "";
                    if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
                    if (!desc.isEmpty()) desc = " - " + desc;
                    labelMap.put(uri, r.get("label") + desc);
                    count++;
                    if (count > 9) return;
                }
                return;
            }

            if (apiString.startsWith("https://vodex.")) {
                searchterm = expandSearchTerm(searchterm);
            }
            String callUrl;
            if (apiString.contains(" ")) {
                callUrl = apiString.replaceAll(" ", URLEncoder.encode(searchterm, StandardCharsets.UTF_8.toString()));
            } else {
                callUrl = apiString + URLEncoder.encode(searchterm, StandardCharsets.UTF_8.toString());
            }
            HttpGet get = new HttpGet(callUrl);
            get.setHeader(HttpHeaders.ACCEPT, "application/json");
            get.setHeader("User-Agent", NanodashPreferences.get().getWebsiteUrl() + "#user-agent");
            String respString;
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpResponse resp = client.execute(get);
                if (resp.getStatusLine().getStatusCode() == 405) {
                    // Method not allowed, trying POST
                    HttpPost post = new HttpPost(apiString + URLEncoder.encode(searchterm, StandardCharsets.UTF_8.toString()));
                    resp = client.execute(post);
                }
                // TODO: support other content types (CSV, XML, ...)
                // System.err.println(resp.getHeaders("Content-Type")[0]);
                try (InputStream in = resp.getEntity().getContent()) {
                    respString = IOUtils.toString(in, StandardCharsets.UTF_8);
                }
            }
            // System.out.println(respString);

            if (apiString.startsWith("https://w3id.org/np/l/nanopub-query") || apiString.startsWith("https://grlc.") || apiString.contains("/sparql?")) {
                JSONArray resultsArray = new JSONObject(respString).getJSONObject("results").getJSONArray("bindings");
                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject resultObject = resultsArray.getJSONObject(i);
                    // Get the nanopub URI
                    String uri = resultObject.getJSONObject("thing").getString("value");
                    // Get the string which matched with the search term
                    String label = resultObject.getJSONObject("label").getString("value");
                    values.add(uri);
                    labelMap.put(uri, label);
                }
            } else if (apiString.startsWith("https://www.ebi.ac.uk/ols/api/select")) {
                // Resolve EBI Ontology Lookup Service
                // e.g. https://www.ebi.ac.uk/ols/api/select?q=interacts%20with
                // response.docs.[].iri/label
                JSONArray responseArray = new JSONObject(respString).getJSONObject("response").getJSONArray("docs");
                for (int i = 0; i < responseArray.length(); i++) {
                    String uri = responseArray.getJSONObject(i).getString("iri");
                    String label = responseArray.getJSONObject(i).getString("label");
                    try {
                        label += " - " + responseArray.getJSONObject(i).getJSONArray("description").getString(0);
                    } catch (Exception ex) {
                        logger.error("No description found for {}", uri, ex);
                    }
                    if (!values.contains(uri)) {
                        values.add(uri);
                        labelMap.put(uri, label);
                    }
                }
            } else if (apiString.startsWith("https://api.openaire.eu/graph/v")) {
                String type = apiString.replaceFirst("^https://api\\.openaire\\.eu/graph/(v[0-9]+/[a-zA-Z]+).*$", "$1");
                for (Object obj : new JSONObject(respString).getJSONArray("results")) {
                    if (obj instanceof JSONObject jsonObj) {
                        String uri = "https://api.openaire.eu/graph/" + type + "/" + jsonObj.getString("id");
                        if (!values.contains(uri)) {
                            values.add(uri);
                            String label = uri;
                            if (jsonObj.has("mainTitle")) {
                                label = jsonObj.getString("mainTitle");
                            } else if (jsonObj.has("legalShortName")) {
                                label = jsonObj.getString("legalShortName");
                            } else if (jsonObj.has("officialName")) {
                                label = jsonObj.getString("officialName");
                            } else if (jsonObj.has("title")) {
                                label = jsonObj.getString("title");
                            } else if (jsonObj.has("familyName")) {
                                label = jsonObj.getString("familyName");
                                if (jsonObj.has("givenName")) {
                                    label = jsonObj.getString("givenName") + " " + label;
                                }
                            }
                            labelMap.put(uri, label);
                        }
                    }
                }
            } else if (apiString.startsWith("https://api.gbif.org/v1/species/suggest")) {
                JSONArray responseArray = new JSONArray(respString);
                for (int i = 0; i < responseArray.length(); i++) {
                    String uri = "https://www.gbif.org/species/" + responseArray.getJSONObject(i).getString("key");
                    String label = responseArray.getJSONObject(i).getString("scientificName");
                    if (!values.contains(uri)) {
                        values.add(uri);
                        labelMap.put(uri, label);
                    }
                }
            } else if (apiString.startsWith("https://api.catalogueoflife.org/dataset/3LR/nameusage/search")) {
                JSONArray responseArray = new JSONObject(respString).getJSONArray("result");
                for (int i = 0; i < responseArray.length(); i++) {
                    String uri = "https://www.catalogueoflife.org/data/taxon/" + responseArray.getJSONObject(i).getString("id");
                    String label = responseArray.getJSONObject(i).getJSONObject("usage").getString("label");
                    if (!values.contains(uri)) {
                        values.add(uri);
                        labelMap.put(uri, label);
                    }
                }
            } else if (apiString.startsWith("https://vodex.")) {
                // TODO This is just a test and needs to be improved
                JSONArray responseArray = new JSONObject(respString).getJSONObject("response").getJSONArray("docs");
                for (int i = 0; i < responseArray.length(); i++) {
                    String uri = responseArray.getJSONObject(i).getString("id");
                    String label = responseArray.getJSONObject(i).getJSONArray("label").get(0).toString();
                    if (!values.contains(uri)) {
                        values.add(uri);
                        labelMap.put(uri, label);
                    }
                }
            } else if (apiString.startsWith("https://api.ror.org/organizations")) {
                // TODO This is just a test and needs to be improved
                JSONArray responseArray = new JSONObject(respString).getJSONArray("items");
                for (int i = 0; i < responseArray.length(); i++) {
                    String uri = responseArray.getJSONObject(i).getString("id");
                    String label = responseArray.getJSONObject(i).getString("name");
                    if (!values.contains(uri)) {
                        values.add(uri);
                        labelMap.put(uri, label);
                    }
                }
            } else {
                // TODO: create parseJsonApi() ?
                boolean foundId = false;
                JSONObject json = new JSONObject(respString);
                for (String key : json.keySet()) {
                    if (values.size() > 9) break;
                    if (!(json.get(key) instanceof JSONArray)) continue;
                    JSONArray a = json.getJSONArray(key);
                    for (int i = 0; i < a.length(); i++) {
                        if (values.size() > 9) break;
                        if (!(a.get(i) instanceof JSONObject)) continue;
                        JSONObject o = a.getJSONObject(i);
                        String uri = null;
                        for (String s : new String[]{"@id", "concepturi", "uri"}) {
                            if (o.has(s)) {
                                uri = o.get(s).toString();
                                foundId = true;
                                break;
                            }
                        }
                        if (uri != null) {
                            values.add(uri);
                            String label = "";
                            for (String s : new String[]{"prefLabel", "label"}) {
                                if (o.has(s)) {
                                    label = o.get(s).toString().replaceAll(" - ", " -- ");
                                    break;
                                }
                            }
                            String desc = "";
                            for (String s : new String[]{"definition", "description"}) {
                                if (o.has(s)) {
                                    desc = o.get(s).toString();
                                    break;
                                }
                            }
                            if (!label.isEmpty() && !desc.isEmpty()) desc = " - " + desc;
                            labelMap.put(uri, label + desc);
                        }
                    }
                }
                if (foundId == false) {
                    // ID key not found, try to get results for following format
                    // {result1: ["label 1", "label 2"], result2: ["label 3", "label 4"]}
                    // Aims to resolve https://name-resolution-sri.renci.org/docs#

                    // TODO: It seems this is triggered too often and adds 'https://identifiers.org/search' when it
                    //       shouldn't. Manually filtering these out for now...
                    for (String key : json.keySet()) {
                        if (!(json.get(key) instanceof JSONArray)) continue;
                        if ("search".equals(key)) continue;
                        JSONArray labelArray = json.getJSONArray(key);
                        String uri = key;
                        String label = "";
                        String desc = "";
                        if (labelArray.length() > 0) label = labelArray.getString(0);
                        if (labelArray.length() > 1) desc = labelArray.getString(1);
                        if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
                        if (!label.isEmpty() && !desc.isEmpty()) desc = " - " + desc;
                        // Quick fix to convert CURIE to URI, as Nanodash only accepts URIs here
                        if (!(uri.startsWith("http://") || uri.startsWith("https://"))) {
                            uri = "https://identifiers.org/" + uri;
                        }
                        values.add(uri);
                        labelMap.put(uri, label + desc);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error fetching possible values from API: {}", apiString, ex);
        }
    }

    private static String expandSearchTerm(String searchTerm) {
        String expanded = "";
        boolean insideQuotes = false;
        searchTerm = searchTerm.replaceAll("\\s+", " ").trim();
        for (char c : searchTerm.toCharArray()) {
            if (c == '\n') {
                continue;
            } else if (c == '"') {
                expanded += '"';
                insideQuotes = !insideQuotes;
            } else if (c == ' ') {
                if (insideQuotes) {
                    expanded += ' ';
                } else {
                    expanded += '\n';
                }
            } else if (("" + c).matches("\\w") || c == '-' || c == '_') {
                expanded += c;
            } else {
                if (insideQuotes) {
                    expanded += ' ';
                } else {
                    expanded += '\n';
                }
            }
        }
        String extra = "*";
        expanded = expanded.replaceAll("\\n+", "\n").replaceAll("\"", "\\\\\\\"").trim();
        if (expanded.endsWith("\"") || insideQuotes) extra = "";
        return "( " + Strings.join(" AND ", expanded.split("\n")) + extra + " )";
    }

}
