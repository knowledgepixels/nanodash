package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class QueryApiAccessTest {

    private final String queryName = "get-test-query";
    private final String queryId = "RAJmZoM0xCGE9L0OEgmQBOd1M58ggNkwZ0IUqHOAPRfvE/" + queryName;

    @BeforeEach
    void setUp() {
        QueryApiAccess.load(queryId);
    }

    @Test
    void getQueryId() {
        String result = QueryApiAccess.getQueryId(queryName);
        assertEquals(queryId, result);
    }

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
    void getReturnsApiResponseForValidQueryName() throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(any(QueryRef.class))).thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.get(new QueryRef(queryName));

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

            ApiResponse response = QueryApiAccess.forcedGet(new QueryRef(queryName));

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void getThrowsExceptionForUnknownQueryName() {
        String queryName = "unknownQueryName";

        assertThrows(IllegalArgumentException.class, () -> QueryApiAccess.get(new QueryRef(queryName)));
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