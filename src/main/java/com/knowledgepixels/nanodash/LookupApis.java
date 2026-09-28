package com.knowledgepixels.nanodash;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Utility class for APIs look up and parsing.
 */
public class LookupApis {

    private static final Logger logger = LoggerFactory.getLogger(LookupApis.class);

    private LookupApis() {
    }  // no instances allowed

    private static final int LOOKUP_CONNECT_TIMEOUT_MS = 5_000;

    private static final int LOOKUP_SOCKET_TIMEOUT_MS = 8_000;

    private static final RequestConfig LOOKUP_REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(LOOKUP_CONNECT_TIMEOUT_MS)
            .setSocketTimeout(LOOKUP_SOCKET_TIMEOUT_MS)
            .build();

    /**
     * How long a dropdown waits for all of its APIs together before it shows what has come in.
     */
    static final Duration ALL_LOOKUPS_TIMEOUT = Duration.ofSeconds(10);

    private static final Cache<String, LookupResult> lookupCache = CacheBuilder.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * What one API returned for one search term: the values in the order the API gave them,
     * and a label for each value that has one.
     *
     * @param values the values
     * @param labels the labels, by value
     */
    public record LookupResult(List<String> values, Map<String, String> labels) {

        /**
         * The result of a lookup that returned nothing or failed.
         */
        static final LookupResult EMPTY = new LookupResult(List.of(), Map.of());

    }

    /**
     * Looks up the possible values of several APIs at once, each on its own thread, so that the
     * slowest API sets the wait instead of the sum of them all (issue #88). The values are
     * merged in the order of the APIs, without duplicates; an API that has not answered within
     * {@link #ALL_LOOKUPS_TIMEOUT} is left out, and its answer, once it comes, is cached for the
     * next search.
     *
     * @param apiStrings the API endpoint URLs, in the order their values are to be listed
     * @param searchTerm the search term
     * @param labelMap   the map to add the labels of the values to
     * @return the values
     */
    public static List<String> lookUpAll(List<String> apiStrings, String searchTerm, Map<String, String> labelMap) {
        if (apiStrings.size() == 1) {
            return mergeInto(List.of(lookUp(apiStrings.get(0), searchTerm)), labelMap);
        }
        List<Future<LookupResult>> pending = new ArrayList<>();
        for (String apiString : apiStrings) {
            pending.add(NanodashThreadPool.submit(() -> lookUp(apiString, searchTerm)));
        }
        return mergeInto(awaitAll(pending, apiStrings), labelMap);
    }

    private static List<LookupResult> awaitAll(List<Future<LookupResult>> pending, List<String> apiStrings) {
        long deadline = System.nanoTime() + ALL_LOOKUPS_TIMEOUT.toNanos();
        List<LookupResult> results = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            results.add(await(pending.get(i), apiStrings.get(i), deadline));
        }
        return results;
    }

    private static LookupResult await(Future<LookupResult> lookup, String apiString, long deadline) {
        try {
            return lookup.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException ex) {
            logger.info("API lookup still running after {}, left out for now: {}", ALL_LOOKUPS_TIMEOUT, apiString);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            logger.error("API lookup failed: {}", apiString, ex.getCause());
        }
        return LookupResult.EMPTY;
    }

    /**
     * Merges lookup results into one list of values, in the order of the results and without
     * duplicates, and adds their labels to the given map.
     *
     * @param results  the lookup results, in order
     * @param labelMap the map to add the labels to
     * @return the merged values
     */
    static List<String> mergeInto(List<LookupResult> results, Map<String, String> labelMap) {
        Set<String> values = new LinkedHashSet<>();
        for (LookupResult result : results) {
            values.addAll(result.values());
            labelMap.putAll(result.labels());
        }
        return new ArrayList<>(values);
    }

    /**
     * Looks up the possible values of one API for a search term, answering from the cache when
     * the same search was made in the last minutes. A lookup that fails is not cached, so the
     * next search tries the API again.
     *
     * @param apiString  the API endpoint URL
     * @param searchTerm the search term
     * @return the result, empty if the lookup failed
     */
    public static LookupResult lookUp(String apiString, String searchTerm) {
        String key = apiString + "\n" + searchTerm;
        LookupResult cached = lookupCache.getIfPresent(key);
        if (cached != null) return cached;
        try {
            LookupResult result = fetch(apiString, searchTerm);
            lookupCache.put(key, result);
            return result;
        } catch (Exception ex) {
            logger.error("Error fetching possible values from API: {}", apiString, ex);
            return LookupResult.EMPTY;
        }
    }

    private static LookupResult fetch(String apiString, String searchTerm) throws Exception {
        List<String> values = new ArrayList<>();
        Map<String, String> labels = new HashMap<>();
        fetchPossibleValues(apiString, searchTerm, labels, values);
        values.removeIf(Objects::isNull);
        labels.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new LookupResult(List.copyOf(values), Map.copyOf(labels));
    }

    /**
     * Empties the lookup cache.
     */
    static void clearCache() {
        lookupCache.invalidateAll();
    }

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
     * Picks the name to display for a ROR record. The ROR API does not order the names by
     * display preference, so the entry flagged "ror_display" is used, falling back to a
     * "label" entry and finally to the first name given.
     *
     * @param namesArray the "names" array of a ROR record (must not be empty)
     * @return the name to show to the user
     */
    private static String getRorDisplayName(JSONArray namesArray) {
        String labelName = null;
        for (int i = 0; i < namesArray.length(); i++) {
            JSONObject nameObject = namesArray.getJSONObject(i);
            JSONArray types = nameObject.optJSONArray("types");
            if (types == null) continue;
            for (int j = 0; j < types.length(); j++) {
                String type = types.getString(j);
                if ("ror_display".equals(type)) return nameObject.getString("value");
                if ("label".equals(type) && labelName == null) labelName = nameObject.getString("value");
            }
        }
        if (labelName != null) return labelName;
        return namesArray.getJSONObject(0).getString("value");
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
        try {
            fetchPossibleValues(apiString, searchterm, labelMap, values);
        } catch (Exception ex) {
            logger.error("Error fetching possible values from API: {}", apiString, ex);
        }
    }

    /**
     * Fetches possible values from an API, reporting a failure to the caller instead of
     * logging it, so that only a lookup that went through is cached.
     *
     * @param apiString  the API endpoint URL to query
     * @param searchterm the search term to use for querying the API
     * @param labelMap   a map to store URIs and their corresponding labels
     * @param values     a list to store the extracted URIs
     * @throws Exception if the API cannot be reached or its response cannot be read
     */
    private static void fetchPossibleValues(String apiString, String searchterm, Map<String, String> labelMap, List<String> values) throws Exception {
        // TODO This method is a mess and needs some serious clean-up and structuring...
        if (apiString.startsWith("https://w3id.org/np/l/nanopub-query-1.1/api/") || apiString.startsWith("http://purl.org/nanopub/api/find_signed_things?")) {
            lookupNanopubNetwork(apiString, searchterm, labelMap, values);
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
        get.setConfig(LOOKUP_REQUEST_CONFIG);
        String respString;
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse resp = client.execute(get);
            if (resp.getStatusLine().getStatusCode() == 405) {
                // Method not allowed, trying POST
                EntityUtils.consume(resp.getEntity());
                HttpPost post = new HttpPost(apiString + URLEncoder.encode(searchterm, StandardCharsets.UTF_8.toString()));
                post.setConfig(LOOKUP_REQUEST_CONFIG);
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
            parseNanopubGrlcApi(new JSONObject(respString), labelMap, values);
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
            String type = apiString.replaceFirst("^https://api\\.openaire\\.eu/graph/(v[0-9]+/[^/#?]+).*$", "$1");
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
                JSONArray namesArray = responseArray.getJSONObject(i).getJSONArray("names");
                if (namesArray.length() == 0) continue;
                String label = getRorDisplayName(namesArray);
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
    }

    private static void lookupNanopubNetwork(String apiString, String searchterm, Map<String, String> labelMap, List<String> values) {
        String queryId = QueryApiAccess.FIND_THINGS;
        if (apiString.startsWith("https://w3id.org/np/l/nanopub-query-1.1/api/")) {
            queryId = apiString.replace("https://w3id.org/np/l/nanopub-query-1.1/api/", "");
            if (queryId.contains("?")) queryId = queryId.substring(0, queryId.indexOf("?"));
        }
        Multimap<String, String> params = ArrayListMultimap.create();
        if (apiString.contains("?")) {
            List<NameValuePair> urlParams = URLEncodedUtils.parse(apiString.substring(apiString.indexOf("?") + 1), StandardCharsets.UTF_8);
            for (NameValuePair p : urlParams) {
                params.put(p.getName(), p.getValue());
            }
        }
        GrlcQuery q = GrlcQuery.get(queryId);
        if (q.getEndpoint().stringValue().endsWith("/text")) {
            searchterm = expandSearchTerm(searchterm);
        }
        String queryParamName = "query";
        if (q.getPlaceholdersList().size() == 1) {
            queryParamName = QueryTemplate.getParamName(q.getPlaceholdersList().get(0));
        }
        params.put(queryParamName, searchterm);
        ApiResponse result = ApiCache.retrieveResponseSync(new QueryRef(queryId, params), false);
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
        return "( " + String.join(" AND ", expanded.split("\n")) + extra + " )";
    }

}
