package com.knowledgepixels.nanodash.lookup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Looks up values with a query of the nanopub network, e.g.
 * {@code https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89.../find-things?type=...}, rather
 * than by calling the URL. The query is run through the query cache with the URL's parameters
 * and the search term, which replaces any value the URL gives the search parameter, e.g. the
 * empty one of {@code ?query=}. The values are read from its {@code thing} column, their labels
 * from {@code label} and {@code description}, the description as plain text even where it is
 * HTML, as a Space's often is. The legacy
 * {@code http://purl.org/nanopub/api/find_signed_things?} URLs run the find-things query. The
 * results are read newest first, by their {@code date} column, each thing once, and at most ten
 * of them; the {@code pubkey} and {@code date} columns tell {@link ThingOrigins} who signed each
 * and when it was published (issues #765 and #817).
 */
public class NanopubQueryLookup implements LookupApi {

    /**
     * The prefix of nanopub network query API URLs.
     */
    public static final String QUERY_API_PREFIX = "https://w3id.org/np/l/nanopub-query-1.1/api/";

    /**
     * The prefix of the legacy nanopub API URLs that run the find-things query.
     */
    public static final String LEGACY_FIND_THINGS_PREFIX = "http://purl.org/nanopub/api/find_signed_things?";

    private static final int MAX_RESULTS = 10;

    private static final int MAX_DESCRIPTION_LENGTH = 80;

    /**
     * {@inheritDoc}
     */
    @Override
    public void lookUp(String apiUrl, String searchTerm, Map<String, String> labels, List<String> values) {
        String queryId = queryId(apiUrl);
        GrlcQuery query = GrlcQuery.get(queryId);
        if (query.getEndpoint().stringValue().endsWith("/text")) {
            searchTerm = SearchTerms.expand(searchTerm);
        }
        Multimap<String, String> params = urlParams(apiUrl);
        params.replaceValues(searchParamName(query), List.of(searchTerm));
        ApiResponse response = ApiCache.retrieveResponseSync(new QueryRef(queryId, params), false);
        List<String> found = new ArrayList<>();
        for (ApiResponseEntry entry : newestFirst(response.getData())) {
            if (values.size() >= MAX_RESULTS) break;
            String value = entry.get("thing");
            if (values.contains(value)) continue;
            values.add(value);
            found.add(value);
            labels.put(value, entry.get("label") + descriptionOf(entry));
            ThingOrigins.record(value, entry.get("pubkey"), entry.get("date"));
        }
        ThingOrigins.resolveOrigins(found);
    }

    /**
     * Orders query results by their {@code date} column, newest first. Results without a
     * readable date come last, in the order the query gave them.
     *
     * @param entries the query results
     * @return the results, newest first
     */
    static List<ApiResponseEntry> newestFirst(List<ApiResponseEntry> entries) {
        List<ApiResponseEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(NanopubQueryLookup::dateOf, Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    private static Instant dateOf(ApiResponseEntry entry) {
        String date = entry.get("date");
        if (date == null) return null;
        try {
            return OffsetDateTime.parse(date).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String queryId(String apiUrl) {
        if (!apiUrl.startsWith(QUERY_API_PREFIX)) return QueryApiAccess.FIND_THINGS;
        String queryId = apiUrl.substring(QUERY_API_PREFIX.length());
        return queryId.contains("?") ? queryId.substring(0, queryId.indexOf("?")) : queryId;
    }

    private static Multimap<String, String> urlParams(String apiUrl) {
        Multimap<String, String> params = ArrayListMultimap.create();
        if (apiUrl.contains("?")) {
            for (NameValuePair p : URLEncodedUtils.parse(apiUrl.substring(apiUrl.indexOf("?") + 1), StandardCharsets.UTF_8)) {
                params.put(p.getName(), p.getValue());
            }
        }
        return params;
    }

    private static String searchParamName(GrlcQuery query) {
        if (query.getPlaceholdersList().size() == 1) {
            return QueryTemplate.getParamName(query.getPlaceholdersList().get(0));
        }
        return "query";
    }

    /**
     * Returns the description of a query result as it is appended to its label: as plain text on
     * one line, shortened if it is long.
     *
     * @param entry the query result
     * @return the description with its separator, or an empty string if there is none
     */
    static String descriptionOf(ApiResponseEntry entry) {
        String description = entry.get("description");
        if (description == null) return "";
        description = Utils.toPlainText(description).strip().replaceAll("\\s+", " ");
        if (description.isEmpty()) return "";
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
        }
        return " - " + description;
    }

}
