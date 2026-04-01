package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadRdfPageTest {

    /**
     * Invokes a private method on a DownloadRdfPage instance created via Unsafe (bypassing the constructor).
     */
    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        // Create instance without calling constructor (which requires Wicket context)
        DownloadRdfPage instance = createBareInstance();
        Method method = DownloadRdfPage.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception ex) throw ex;
            throw e;
        }
    }

    private static DownloadRdfPage createBareInstance() throws Exception {
        var unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        return (DownloadRdfPage) ((sun.misc.Unsafe) unsafe.get(null)).allocateInstance(DownloadRdfPage.class);
    }

    @Nested
    @DisplayName("addNanopub")
    class AddNanopubTest {

        @Test
        @DisplayName("should add a nanopub to an empty map")
        void addsToEmptyMap() throws Exception {
            Map<String, Nanopub> collected = new LinkedHashMap<>();
            Nanopub np = TestUtils.createNanopub();

            invokePrivate("addNanopub",
                    new Class[]{Map.class, Nanopub.class},
                    collected, np);

            assertEquals(1, collected.size());
            assertSame(np, collected.get(np.getUri().stringValue()));
        }

        @Test
        @DisplayName("should deduplicate nanopubs by URI")
        void deduplicatesByUri() throws Exception {
            Map<String, Nanopub> collected = new LinkedHashMap<>();
            Nanopub np1 = TestUtils.createNanopub();
            Nanopub np2 = TestUtils.createNanopub(); // same URI

            invokePrivate("addNanopub",
                    new Class[]{Map.class, Nanopub.class},
                    collected, np1);
            invokePrivate("addNanopub",
                    new Class[]{Map.class, Nanopub.class},
                    collected, np2);

            assertEquals(1, collected.size());
            assertSame(np1, collected.values().iterator().next(), "first instance should be kept");
        }

        @Test
        @DisplayName("should add nanopubs with different URIs")
        void addsDifferentUris() throws Exception {
            Map<String, Nanopub> collected = new LinkedHashMap<>();
            Nanopub np1 = TestUtils.createNanopub("https://w3id.org/np/test1");
            Nanopub np2 = TestUtils.createNanopub("https://w3id.org/np/test2");

            invokePrivate("addNanopub",
                    new Class[]{Map.class, Nanopub.class},
                    collected, np1);
            invokePrivate("addNanopub",
                    new Class[]{Map.class, Nanopub.class},
                    collected, np2);

            assertEquals(2, collected.size());
        }
    }

    @Nested
    @DisplayName("writeJsonLdArray")
    class WriteJsonLdArrayTest {

        @Test
        @DisplayName("should write empty array for empty list")
        void emptyList() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            invokePrivate("writeJsonLdArray",
                    new Class[]{OutputStream.class, List.class},
                    out, Collections.emptyList());

            String result = out.toString();
            assertEquals("[\n\n]\n", result);
        }

        @Test
        @DisplayName("should write single nanopub without comma separator")
        void singleNanopub() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Nanopub np = TestUtils.createNanopub();

            invokePrivate("writeJsonLdArray",
                    new Class[]{OutputStream.class, List.class},
                    out, List.of(np));

            String result = out.toString();
            assertTrue(result.startsWith("["), "should start with [");
            assertTrue(result.endsWith("]\n"), "should end with ]");
            assertFalse(result.contains(",\n{"), "single element should have no comma separator");
        }

        @Test
        @DisplayName("should wrap multiple nanopubs in array with both URIs present")
        void multipleNanopubs() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Nanopub np1 = TestUtils.createNanopub("https://w3id.org/np/test1");
            Nanopub np2 = TestUtils.createNanopub("https://w3id.org/np/test2");

            invokePrivate("writeJsonLdArray",
                    new Class[]{OutputStream.class, List.class},
                    out, List.of(np1, np2));

            String result = out.toString();
            assertTrue(result.startsWith("["), "should start with [");
            assertTrue(result.endsWith("]\n"), "should end with ]");
            assertTrue(result.contains("test1"), "should contain first nanopub");
            assertTrue(result.contains("test2"), "should contain second nanopub");
        }
    }

    @Nested
    @DisplayName("retrieveResponseWithWait")
    class RetrieveResponseWithWaitTest {

        @Test
        @DisplayName("should return response immediately when available")
        void returnsImmediately() throws Exception {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(expected);

                ApiResponse result = (ApiResponse) invokePrivate("retrieveResponseWithWait",
                        new Class[]{QueryRef.class},
                        queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(1));
            }
        }

        @Test
        @DisplayName("should return null when no result and not running")
        void returnsNullWhenNotRunning() throws Exception {
            QueryRef queryRef = mock(QueryRef.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false)).thenReturn(null);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(false);

                ApiResponse result = (ApiResponse) invokePrivate("retrieveResponseWithWait",
                        new Class[]{QueryRef.class},
                        queryRef);

                assertNull(result);
            }
        }

        @Test
        @DisplayName("should retry and return result when query finishes running")
        void retriesWhileRunning() throws Exception {
            QueryRef queryRef = mock(QueryRef.class);
            ApiResponse expected = mock(ApiResponse.class);

            try (MockedStatic<ApiCache> apiCache = mockStatic(ApiCache.class)) {
                // First call: null (running), second call: result available
                apiCache.when(() -> ApiCache.retrieveResponseSync(queryRef, false))
                        .thenReturn(null)
                        .thenReturn(expected);
                apiCache.when(() -> ApiCache.isRunning(queryRef)).thenReturn(true);

                ApiResponse result = (ApiResponse) invokePrivate("retrieveResponseWithWait",
                        new Class[]{QueryRef.class},
                        queryRef);

                assertSame(expected, result);
                apiCache.verify(() -> ApiCache.retrieveResponseSync(queryRef, false), times(2));
            }
        }
    }

    @Nested
    @DisplayName("FORMAT_MAP and EXTENSION_MAP")
    class FormatMapsTest {

        @Test
        @DisplayName("FORMAT_MAP should contain all supported formats")
        void formatMapComplete() throws Exception {
            var field = DownloadRdfPage.class.getDeclaredField("FORMAT_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, RDFFormat> map = (Map<String, RDFFormat>) field.get(null);

            assertEquals(RDFFormat.TRIG, map.get("trig"));
            assertEquals(RDFFormat.TRIX, map.get("trix"));
            assertEquals(RDFFormat.JSONLD, map.get("jsonld"));
            assertEquals(RDFFormat.NQUADS, map.get("nq"));
            assertEquals(RDFFormat.TURTLE, map.get("turtle"));
            assertEquals(RDFFormat.NTRIPLES, map.get("nt"));
            assertEquals(RDFFormat.RDFXML, map.get("rdfxml"));
            assertEquals(7, map.size());
        }

        @Test
        @DisplayName("EXTENSION_MAP should have matching entries for all formats")
        void extensionMapComplete() throws Exception {
            var field = DownloadRdfPage.class.getDeclaredField("EXTENSION_MAP");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) field.get(null);

            assertEquals(".trig", map.get("trig"));
            assertEquals(".xml", map.get("trix"));
            assertEquals(".jsonld", map.get("jsonld"));
            assertEquals(".nq", map.get("nq"));
            assertEquals(".ttl", map.get("turtle"));
            assertEquals(".nt", map.get("nt"));
            assertEquals(".rdf", map.get("rdfxml"));
            assertEquals(7, map.size());
        }
    }

    @Nested
    @DisplayName("collectResourceNanopubs")
    class CollectResourceNanopubsTest {

        @Test
        @DisplayName("should add resource declaration nanopub")
        void addsDeclaration() throws Exception {
            Map<String, Nanopub> collected = new LinkedHashMap<>();
            var resource = mock(com.knowledgepixels.nanodash.domain.MaintainedResource.class);
            Nanopub np = TestUtils.createNanopub();
            when(resource.getNanopub()).thenReturn(np);

            invokePrivate("collectResourceNanopubs",
                    new Class[]{Map.class, com.knowledgepixels.nanodash.domain.MaintainedResource.class},
                    collected, resource);

            assertEquals(1, collected.size());
            assertSame(np, collected.values().iterator().next());
        }

        @Test
        @DisplayName("should handle null nanopub gracefully")
        void handlesNullNanopub() throws Exception {
            Map<String, Nanopub> collected = new LinkedHashMap<>();
            var resource = mock(com.knowledgepixels.nanodash.domain.MaintainedResource.class);
            when(resource.getNanopub()).thenReturn(null);

            invokePrivate("collectResourceNanopubs",
                    new Class[]{Map.class, com.knowledgepixels.nanodash.domain.MaintainedResource.class},
                    collected, resource);

            assertTrue(collected.isEmpty());
        }
    }

    @Nested
    @DisplayName("MAX_NANOPUBS constant")
    class MaxNanopubsTest {

        @Test
        @DisplayName("MAX_NANOPUBS should be 1000")
        void maxNanopubsIs1000() throws Exception {
            var field = DownloadRdfPage.class.getDeclaredField("MAX_NANOPUBS");
            field.setAccessible(true);
            assertEquals(1000, field.getInt(null));
        }
    }

    @Nested
    @DisplayName("MOUNT_PATH constant")
    class MountPathTest {

        @Test
        @DisplayName("MOUNT_PATH should be /download-rdf")
        void mountPath() {
            assertEquals("/download-rdf", DownloadRdfPage.MOUNT_PATH);
        }
    }
}
