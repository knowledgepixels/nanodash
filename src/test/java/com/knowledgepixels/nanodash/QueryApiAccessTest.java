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

}