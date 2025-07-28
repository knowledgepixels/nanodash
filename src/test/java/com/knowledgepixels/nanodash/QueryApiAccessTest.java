package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void getReturnsApiResponseForValidQueryId() throws FailedApiCallException {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(queryId, new HashMap<>())).thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.get(queryId);

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void getReturnsApiResponseForValidQueryName() throws FailedApiCallException {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(queryId, new HashMap<>())).thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.get(queryName);

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void forcedGetRetriesUntilApiResponseIsNotNull() {
        ApiResponse expectedResponse = new ApiResponse();

        try (MockedStatic<QueryAccess> mockQueryAccess = mockStatic(QueryAccess.class)) {
            mockQueryAccess.when(() -> QueryAccess.get(queryId, new HashMap<>()))
                    .thenReturn(null)
                    .thenReturn(null)
                    .thenReturn(expectedResponse);

            ApiResponse response = QueryApiAccess.forcedGet(queryName);

            assertEquals(expectedResponse, response);
        }
    }

    @Test
    void getThrowsExceptionForUnknownQueryName() {
        String queryName = "unknownQueryName";
        Map<String, String> params = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> QueryApiAccess.get(queryName, params));
    }

}