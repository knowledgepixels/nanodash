package com.knowledgepixels.nanodash.utils;

import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;
import org.nanopub.extra.services.QueryCall;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Serves the query API from canned answers for the duration of a test, so that nothing reaches
 * the network.
 * <p>
 * Every query nanodash runs goes through {@link QueryAccess#get(QueryRef)}, so intercepting that
 * one call covers the lot: the user data behind an agent field, the latest-version lookups, the
 * lists a view page is built from. A query nothing was registered for answers empty rather than
 * failing, which is what a test that does not care about a given lookup wants.
 * <p>
 * Without this, tests that touch these lookups depend on the live APIs being reachable and on
 * what happens to be in them. That is why a publish form can validate on a developer's machine
 * and fail on CI: the calls there time out, the data never arrives, and the form is left with
 * fields it cannot fill. Combine with {@link TestProfile} to also get a user to publish as.
 * <p>
 * Use it as a resource, so the static mocks are always released:
 * <pre>
 * try (TestApiStubs stubs = TestApiStubs.open()) {
 *     stubs.answer(QueryApiAccess.GET_LATEST_USERS, new String[]{"user"}, List.of(new String[]{"https://orcid.org/..."}));
 *     ...
 * }
 * </pre>
 * or as a field opened in {@code @BeforeEach} and closed in {@code @AfterEach}, which is how the
 * other tests here handle their static mocks.
 */
public final class TestApiStubs implements AutoCloseable {

    /** The query instances the code under test believes exist. Never contacted. */
    public static final List<String> API_INSTANCES = List.of("https://query.example.org/");

    private final Map<String, ApiResponse> answers = new HashMap<>();
    private final List<String> requested = new ArrayList<>();

    private final MockedStatic<QueryAccess> queryAccess;
    private final MockedStatic<QueryCall> queryCall;

    private TestApiStubs() {
        // Answers are looked up per call, so a test can register them after opening.
        queryAccess = mockStatic(QueryAccess.class);
        queryAccess.when(() -> QueryAccess.get(any(QueryRef.class))).thenAnswer(invocation -> {
            QueryRef ref = invocation.getArgument(0);
            requested.add(ref.getQueryId());
            return answers.getOrDefault(ref.getQueryId(), new ApiResponse());
        });
        // Instance discovery is a network call of its own, made while the application starts up.
        queryCall = mockStatic(QueryCall.class, invocation -> {
            if (invocation.getMethod().getName().equals("getApiInstances")) return API_INSTANCES;
            return null;
        });
    }

    /**
     * Starts serving the query API from canned answers. Close it to hand the API back.
     */
    public static TestApiStubs open() {
        return new TestApiStubs();
    }

    /**
     * Registers the response a query is to be answered with.
     *
     * @param queryId  the full query ID, as the constants on {@code QueryApiAccess} give it
     * @param response the response to answer with
     */
    public TestApiStubs answer(String queryId, ApiResponse response) {
        answers.put(queryId, response);
        return this;
    }

    /**
     * Registers the response a query is to be answered with, as a header and its rows.
     *
     * @param queryId the full query ID, as the constants on {@code QueryApiAccess} give it
     * @param header  the column names
     * @param rows    a value per column, in the order of the header
     */
    public TestApiStubs answer(String queryId, String[] header, List<String[]> rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(header);
        for (String[] row : rows) {
            ApiResponseEntry entry = new ApiResponseEntry();
            for (int i = 0; i < header.length; i++) {
                entry.add(header[i], i < row.length ? row[i] : "");
            }
            response.add(entry);
        }
        return answer(queryId, response);
    }

    /**
     * Registers a single-row response, given as column/value pairs.
     *
     * @param queryId the full query ID
     * @param columns the columns and their values, in order
     */
    public TestApiStubs answerRow(String queryId, LinkedHashMap<String, String> columns) {
        return answer(queryId, columns.keySet().toArray(new String[0]),
                List.<String[]>of(columns.values().toArray(new String[0])));
    }

    /**
     * Returns the IDs of the queries that were asked for, in order, so that a test can assert on
     * what the code under test went looking for.
     */
    public List<String> requestedQueryIds() {
        return List.copyOf(requested);
    }

    @Override
    public void close() {
        try {
            queryCall.close();
        } finally {
            queryAccess.close();
        }
    }

}
