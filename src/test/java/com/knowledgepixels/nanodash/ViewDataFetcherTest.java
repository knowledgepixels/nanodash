package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ViewDataFetcherTest {

    @Nested
    @DisplayName("retrieveResponseWithWait")
    class RetrieveResponseWithWaitTest {

        @Test
        @DisplayName("should return response immediately when available")
        void returnsImmediately() {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(expected);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(1));
            }
        }

        @Test
        @DisplayName("should return null when no result and not running")
        void returnsNullWhenNotRunning() {
            QueryRef queryRef = mock(QueryRef.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(null);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(false);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertNull(result);
            }
        }

        @Test
        @DisplayName("should retry and return result when query finishes running")
        void retriesWhileRunning() {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                // First call: null (running), second call: result available
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false))
                        .thenReturn(null)
                        .thenReturn(expected);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(true);

                ApiResponse result = ViewDataFetcher.retrieveResponseWithWait(queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(2));
            }
        }
    }

}
