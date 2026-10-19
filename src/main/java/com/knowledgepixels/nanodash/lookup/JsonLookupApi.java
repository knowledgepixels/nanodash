package com.knowledgepixels.nanodash.lookup;

import com.knowledgepixels.nanodash.NanodashPreferences;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * An API that is called over HTTP with the search term in its URL and answers in JSON. The
 * search term replaces a space in the API URL if there is one, and is appended to the URL
 * otherwise. An API that does not allow GET is called with POST.
 */
public abstract class JsonLookupApi implements LookupApi {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private static final int SOCKET_TIMEOUT_MS = 8_000;

    private static final int METHOD_NOT_ALLOWED = 405;

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(CONNECT_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS)
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public void lookUp(String apiUrl, String searchTerm, Map<String, String> labels, List<String> values) throws Exception {
        String response = fetch(apiUrl, prepareSearchTerm(searchTerm));
        parse(response, apiUrl, labels, values);
    }

    /**
     * Rewrites the search term into the form the API expects.
     *
     * @param searchTerm the search term as typed
     * @return the search term to send to the API
     */
    protected String prepareSearchTerm(String searchTerm) {
        return searchTerm;
    }

    /**
     * Reads the values and their labels from the answer of the API.
     *
     * @param response the answer of the API
     * @param apiUrl   the API URL as given in the template
     * @param labels   the map to add the labels of the found values to
     * @param values   the list to add the found values to
     */
    protected abstract void parse(String response, String apiUrl, Map<String, String> labels, List<String> values);

    /**
     * Adds a value and its label, unless the value has been added already.
     *
     * @param value  the value
     * @param label  the label of the value
     * @param labels the map of labels
     * @param values the list of values
     */
    protected static void addIfNew(String value, String label, Map<String, String> labels, List<String> values) {
        if (values.contains(value)) return;
        values.add(value);
        labels.put(value, label);
    }

    private static String fetch(String apiUrl, String searchTerm) throws IOException {
        String encodedTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        HttpGet get = new HttpGet(callUrl(apiUrl, encodedTerm));
        get.setHeader(HttpHeaders.ACCEPT, "application/json");
        get.setHeader("User-Agent", NanodashPreferences.get().getWebsiteUrl() + "#user-agent");
        get.setConfig(REQUEST_CONFIG);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse resp = client.execute(get);
            if (resp.getStatusLine().getStatusCode() == METHOD_NOT_ALLOWED) {
                EntityUtils.consume(resp.getEntity());
                HttpPost post = new HttpPost(apiUrl + encodedTerm);
                post.setConfig(REQUEST_CONFIG);
                resp = client.execute(post);
            }
            try (InputStream in = resp.getEntity().getContent()) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        }
    }

    private static String callUrl(String apiUrl, String encodedTerm) {
        if (apiUrl.contains(" ")) {
            return apiUrl.replaceAll(" ", encodedTerm);
        }
        return apiUrl + encodedTerm;
    }

}
