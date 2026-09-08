package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class QueryApiAccessTest {

    // Use a query that exists as a static field in QueryApiAccess (full ID only)
    private final String queryId = QueryApiAccess.GET_LATEST_USERS;
    private final String queryName = "get-latest-users";

    @Test
    void getReturnsApiResponseForValidQueryId() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class))).thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.get(new QueryRef(queryId));

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void forcedGetRetriesUntilApiResponseIsNotNull() {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class)))
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.forcedGet(new QueryRef(queryId));

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void getThrowsExceptionWhenNameIsNotFullQueryId() {
        assertThrows(IllegalArgumentException.class, () -> QueryApiAccess.get(new QueryRef("short-name-only")));
    }

    @Test
    void getQueryNameReturnsNullForNullIRI() {
        String result = QueryApiAccess.getQueryName(null);
        assertNull(result);
    }

    @Test
    void getQueryNameReturnsNullForInvalidIRI() {
        IRI queryIri = TestUtils.vf.createIRI("https://example.org/invalidIRI");
        String result = QueryApiAccess.getQueryName(queryIri);
        assertNull(result);
    }

    @Test
    void getQueryNameExtractsQueryNameFromValidIRI() {
        String result = QueryApiAccess.getQueryName(TestUtils.vf.createIRI("https://w3id.org/np/" + queryId));
        assertEquals(queryName, result);
    }

    // An identifier minted under a prefix carries no artifact code, so whether it is free has
    // to be asked of what has already been published (#646).
    @Test
    void isUriIntroducedReportsAnIdentifierThatIsAlreadyTaken() {
        ApiResponse response = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("np", "https://w3id.org/np/RAMVfH7NHhyXFkvbdYYBtimoivyFpQl6CrXgoKgmjGE6I");
        response.getData().add(entry);

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class))).thenReturn(response);

            assertTrue(QueryApiAccess.isUriIntroduced("https://w3id.org/spaces/example/bar"));
        }
    }

    @Test
    void isUriIntroducedReportsAnIdentifierThatIsFree() {
        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class))).thenReturn(new ApiResponse());

            assertFalse(QueryApiAccess.isUriIntroduced("https://w3id.org/spaces/example/unused"));
        }
    }

    // A check that cannot be made is not evidence of a collision: an unreachable query
    // service must not be what stops someone from publishing.
    @Test
    void isUriIntroducedAnswersFalseWhenTheQueryFails() {
        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class)))
                    .thenThrow(new APINotReachableException("no instance reachable"));

            assertFalse(QueryApiAccess.isUriIntroduced("https://w3id.org/spaces/example/bar"));
        }
    }

}