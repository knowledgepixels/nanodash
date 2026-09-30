package com.knowledgepixels.nanodash;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knowledgepixels.nanodash.lookup.LookupApiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

/**
 * Looks up the possible values of guided-choice placeholders in the APIs their templates name,
 * in parallel and with a cache. How each API is called and its answer read is decided by the
 * {@link LookupApiRegistry}.
 */
public class LookupApis {

    private static final Logger logger = LoggerFactory.getLogger(LookupApis.class);

    private LookupApis() {
    }  // no instances allowed

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
            logLookupFailure(apiString, ex);
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
            logLookupFailure(apiString, ex);
        }
    }

    /**
     * Logs a lookup that failed. An API that could not be reached or did not answer in time is
     * an outage of that API rather than an error of Nanodash, so it is logged as a warning in
     * one line; any other failure is logged as an error, with its stack trace.
     *
     * @param apiString the API endpoint URL
     * @param ex        the reason the lookup failed
     */
    private static void logLookupFailure(String apiString, Exception ex) {
        if (ex instanceof IOException) {
            logger.warn("Could not get possible values from API {}: {}", apiString, ex.toString());
        } else {
            logger.error("Error fetching possible values from API: {}", apiString, ex);
        }
    }

    /**
     * Fetches possible values from an API with the lookup API registered for its URL, reporting
     * a failure to the caller instead of logging it, so that only a lookup that went through is
     * cached. A blank search term is not looked up: the APIs search by text, so they find nothing
     * for it, and some take long to answer it.
     *
     * @param apiString  the API endpoint URL to query
     * @param searchterm the search term to use for querying the API
     * @param labelMap   a map to store URIs and their corresponding labels
     * @param values     a list to store the extracted URIs
     * @throws Exception if the API cannot be reached or its response cannot be read
     */
    private static void fetchPossibleValues(String apiString, String searchterm, Map<String, String> labelMap, List<String> values) throws Exception {
        if (searchterm == null || searchterm.isBlank()) return;
        LookupApiRegistry.forUrl(apiString).lookUp(apiString, searchterm, labelMap, values);
    }

}
