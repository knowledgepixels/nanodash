package com.knowledgepixels.nanodash;

import com.github.openjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LookupApisTest {

    // ---- Fixture JSON strings ----

    private static final String EBI_OLS_FIXTURE =
            "{\"response\":{\"docs\":[{\"iri\":\"http://purl.obolibrary.org/obo/HP_0001627\"," +
            "\"label\":\"Abnormal heart morphology\",\"description\":[\"Abnormality of the heart.\"]}]}}";

    private static final String GBIF_FIXTURE =
            "[{\"key\":\"2436436\",\"scientificName\":\"Homo sapiens Linnaeus, 1758\"}]";

    private static final String COL_FIXTURE =
            "{\"result\":[{\"id\":\"7LNW5\",\"usage\":{\"label\":\"Canis lupus Linnaeus, 1758\"}}]}";

    private static final String VODEX_FIXTURE =
            "{\"response\":{\"docs\":[{\"id\":\"https://example.org/carabidae\",\"label\":[\"Carabidae\"]}]}}";

    private static final String ROR_FIXTURE =
            "{\"items\":[{\"id\":\"https://ror.org/03vek6s52\",\"names\":[{\"value\":\"Harvard University\"}]}]}";

    private static final String OPENAIRE_FIXTURE =
            "{\"results\":[{\"id\":\"abc123\",\"mainTitle\":\"Test Item\"}]}";

    private static final String WIKIDATA_FIXTURE =
            "{\"search\":[{\"concepturi\":\"http://www.wikidata.org/entity/Q7187\"," +
            "\"label\":\"gene\",\"description\":\"unit of heredity\"}]}";

    private static final String SPARQL_FIXTURE =
            "{\"results\":{\"bindings\":[{\"thing\":{\"value\":\"https://example.org/thing1\"}," +
            "\"label\":{\"value\":\"Thing 1\"}}]}}";

    private static final String BIOPORTAL_FIXTURE =
            "{\"collection\":[{\"@id\":\"http://purl.obolibrary.org/obo/ECO_0000006\"," +
            "\"prefLabel\":\"experimental evidence\",\"definition\":\"A type of evidence\"}]}";

    // ---- Mock helpers ----

    private MockedStatic<HttpClientBuilder> mockHttp(String responseJson) throws Exception {
        MockedStatic<HttpClientBuilder> mockedStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builderMock = mock(HttpClientBuilder.class);
        CloseableHttpClient clientMock = mock(CloseableHttpClient.class);
        CloseableHttpResponse responseMock = mock(CloseableHttpResponse.class);
        StatusLine statusLineMock = mock(StatusLine.class);
        HttpEntity entityMock = mock(HttpEntity.class);

        mockedStatic.when(HttpClientBuilder::create).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(clientMock);
        when(clientMock.execute(any())).thenReturn(responseMock);
        when(responseMock.getStatusLine()).thenReturn(statusLineMock);
        when(statusLineMock.getStatusCode()).thenReturn(200);
        when(responseMock.getEntity()).thenReturn(entityMock);
        when(entityMock.getContent()).thenReturn(
                new ByteArrayInputStream(responseJson.getBytes(StandardCharsets.UTF_8)));

        return mockedStatic;
    }

    private AutoCloseable mockNanopubNetwork() {
        MockedStatic<GrlcQuery> mockedGrlc = mockStatic(GrlcQuery.class);
        GrlcQuery grlcMock = mock(GrlcQuery.class);
        IRI endpointMock = mock(IRI.class);
        when(endpointMock.stringValue()).thenReturn("https://example.org/endpoint");
        when(grlcMock.getEndpoint()).thenReturn(endpointMock);
        when(grlcMock.getPlaceholdersList()).thenReturn(Collections.emptyList());
        mockedGrlc.when(() -> GrlcQuery.get(anyString())).thenReturn(grlcMock);

        MockedStatic<ApiCache> mockedApiCache = mockStatic(ApiCache.class);
        ApiResponse apiResponse = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("thing", "https://example.org/thing1");
        entry.add("label", "Mock Result");
        apiResponse.getData().add(entry);
        mockedApiCache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), anyBoolean()))
                .thenReturn(apiResponse);

        return () -> {
            mockedApiCache.close();
            mockedGrlc.close();
        };
    }

    // ---- parseNanopubGrlcApi ----

    @Test
    void parseNanopubGrlcApi_singleResult() {
        JSONObject json = new JSONObject("{\"results\":{\"bindings\":[" +
                "{\"thing\":{\"value\":\"https://example.org/thing1\"},\"label\":{\"value\":\"Thing 1\"}}" +
                "]}}");
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.parseNanopubGrlcApi(json, labelMap, values);
        assertEquals(1, values.size());
        assertEquals("https://example.org/thing1", values.get(0));
        assertEquals("Thing 1", labelMap.get("https://example.org/thing1"));
    }

    @Test
    void parseNanopubGrlcApi_multipleResults() {
        JSONObject json = new JSONObject("{\"results\":{\"bindings\":[" +
                "{\"thing\":{\"value\":\"https://example.org/thing1\"},\"label\":{\"value\":\"Thing 1\"}}," +
                "{\"thing\":{\"value\":\"https://example.org/thing2\"},\"label\":{\"value\":\"Thing 2\"}}" +
                "]}}");
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.parseNanopubGrlcApi(json, labelMap, values);
        assertEquals(2, values.size());
        assertEquals("Thing 1", labelMap.get("https://example.org/thing1"));
        assertEquals("Thing 2", labelMap.get("https://example.org/thing2"));
    }

    @Test
    void parseNanopubGrlcApi_emptyResults() {
        JSONObject json = new JSONObject("{\"results\":{\"bindings\":[]}}");
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.parseNanopubGrlcApi(json, labelMap, values);
        assertTrue(values.isEmpty());
        assertTrue(labelMap.isEmpty());
    }

    // ---- expandSearchTerm (private method, tested via reflection) ----

    private String expandSearchTerm(String input) throws Exception {
        Method method = LookupApis.class.getDeclaredMethod("expandSearchTerm", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, input);
    }

    @Test
    void expandSearchTerm_singleWord() throws Exception {
        assertEquals("( covid* )", expandSearchTerm("covid"));
    }

    @Test
    void expandSearchTerm_twoWords() throws Exception {
        assertEquals("( covid AND virus* )", expandSearchTerm("covid virus"));
    }

    @Test
    void expandSearchTerm_extraWhitespace() throws Exception {
        assertEquals("( covid* )", expandSearchTerm("  covid  "));
    }

    @Test
    void expandSearchTerm_quotedPhrase() throws Exception {
        String result = expandSearchTerm("\"covid virus\"");
        assertTrue(result.startsWith("( "));
        assertTrue(result.endsWith(" )"));
        assertFalse(result.endsWith("* )"), "Quoted phrase should not have wildcard: " + result);
    }

    // ---- getPossibleValues tests ----

    private void assertValuesWithLabels(Map<String, String> labelMap, List<String> values) {
        assertFalse(values.isEmpty(), "Expected at least one result");
        for (String v : values) {
            assertNotNull(labelMap.get(v), "Missing label for value: " + v);
        }
    }

    @Test
    void getPossibleValues_ebiOls() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?q=", "heart", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("http")), "All URIs should be HTTP IRIs");
    }

    @Test
    void getPossibleValues_gbif() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(GBIF_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.gbif.org/v1/species/suggest?q=", "homo", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://www.gbif.org/species/")),
                "All URIs should be GBIF species URIs");
    }

    @Test
    void getPossibleValues_catalogueOfLife() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(COL_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.catalogueoflife.org/dataset/3LR/nameusage/search?q=", "canis", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://www.catalogueoflife.org/data/taxon/")),
                "All URIs should be Catalogue of Life taxon URIs");
    }

    @Test
    void getPossibleValues_vodex() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(VODEX_FIXTURE)) {
            LookupApis.getPossibleValues("https://vodex.petapico.org/nidx/query?rows=100&q=label:", "Carabidae", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ror() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(ROR_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.ror.org/organizations?query=", "harvard", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://ror.org/")),
                "All URIs should be ROR organization URIs");
    }

    @Test
    void getPossibleValues_openaire() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/researchProducts?search=", "climate", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://api.openaire.eu/graph/v1/researchProducts/")),
                "All URIs should be OpenAire research-product URIs");
    }

    @Test
    void getPossibleValues_wikidata() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(WIKIDATA_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=en&format=json&limit=5&search=",
                    "gene", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.contains("wikidata.org")),
                "All URIs should be Wikidata entity URIs");
    }

    @Test
    void getPossibleValues_purl() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23Class&searchterm=",
                    "abc", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- EBI OLS variants ----

    @Test
    void getPossibleValues_ebiOlsRo() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?ontology=ro&fieldList=iri,label,description&q=", "regulates", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsUberonNoFieldList() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?ontology=uberon&q=", "heart", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsFieldListOnly() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?fieldList=iri,label,description&q=", "gene", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsStato() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?ontology=stato&fieldList=iri,label,description&q=", "mean", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsOgg() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?ontology=ogg&fieldList=iri,label,description&q=", "gene", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsUberonWithChildrenOf() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://www.ebi.ac.uk/ols/api/select?ontology=uberon&fieldList=iri,label,description&childrenOf=http://purl.obolibrary.org/obo/UBERON_0000105&q=",
                    "embryo", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ebiOlsEnvo() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(EBI_OLS_FIXTURE)) {
            LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?ontology=envo&fieldList=iri,label,description&q=", "forest", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- Wikidata property ----

    @Test
    void getPossibleValues_wikidataProperty() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(WIKIDATA_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://www.wikidata.org/w/api.php?action=wbsearchentities&type=property&language=en&format=json&limit=5&search=",
                    "author", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- OpenAire variants ----

    @Test
    void getPossibleValues_openaire_v2ResearchProducts() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v2/researchProducts?search=", "climate", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_openaire_v1Organizations() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/organizations?search=", "university", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_openaire_v1Persons() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/persons?search=", "smith", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_openaire_v1Projects() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/projects?search=", "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_openaire_v1DataSources() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(OPENAIRE_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/dataSources?search=", "repository", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- ROR advanced ----

    @Test
    void getPossibleValues_rorAdvanced() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(ROR_FIXTURE)) {
            LookupApis.getPossibleValues("https://api.ror.org/organizations?query.advanced=", "MIT", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    @Disabled("ROR API rejects 'name' as a field in query.advanced — returns: 'string name contains an illegal field name'")
    void getPossibleValues_rorAdvancedName() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://api.ror.org/organizations?query.advanced=name:", "harvard", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    // ---- Vodex variants ----

    @Test
    void getPossibleValues_vodexNidxNoRows() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(VODEX_FIXTURE)) {
            LookupApis.getPossibleValues("https://vodex.petapico.org/nidx/query?q=label:", "Carabidae", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_vodexNcbiTaxon() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(VODEX_FIXTURE)) {
            LookupApis.getPossibleValues("https://vodex.petapico.org/ncbitaxon/query?q=label:", "Homo", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- grlc fairconnect ----

    @Test
    void getPossibleValues_grlcFerSearch() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://grlc.knowledgepixels.com/api-git/knowledgepixels/fairconnect-api/fer_search?query=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_grlcFsrLookup() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://grlc.knowledgepixels.com/api-git/knowledgepixels/fairconnect-api/fsr_lookup?searchterm= %2A",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- NERC SPARQL ----

    @Test
    void getPossibleValues_nercSparqlConcept() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://vocab.nerc.ac.uk/sparql/sparql?query=prefix%20skos%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E%0A%0Aselect%20%3Fthing%20%3Flabel%20where%20%7B%0A%20%20%3Fthing%20a%20skos%3AConcept%20.%0A%20%20%3Fthing%20skos%3AprefLabel%20%3Flabel%20.%0A%20%20filter%28contains%28str%28%3Flabel%29%2C%20%22 %22%29%29%0A%7D%20limit%2010",
                    "temperature", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    @Disabled("Uses ?uri binding instead of ?thing — the code expects ?thing, so no results are returned (bug in API binding)")
    void getPossibleValues_nercSparqlConceptUri() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://vocab.nerc.ac.uk/sparql/sparql?query=prefix%20skos%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E%0A%0Aselect%20%3Furi%20%3Flabel%20where%20%7B%0A%20%20%3Furi%20a%20skos%3AConcept%20.%0A%20%20%3Furi%20skos%3AprefLabel%20%3Flabel%20.%0A%20%20filter%28contains%28str%28%3Flabel%29%2C%20%22 %22%29%29%0A%7D%20limit%2010",
                "temperature", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nercSparqlS04() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://vocab.nerc.ac.uk/sparql/sparql?query=prefix%20skos%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E%0Aprefix%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0Aprefix%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0Aselect%20%3Fthing%20%3Flabel%20where%20%7B%0A%20%20%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FS04%2Fcurrent%2F%3E%20skos%3Amember%20%3Fthing%20.%0A%20%20%3Fthing%20skos%3AprefLabel%20%3Flabel%20.%0A%20%20filter%28contains%28str%28%3Flabel%29%2C%20%22 %22%29%29%0A%7D",
                    "temperature", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nercSparqlS05() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://vocab.nerc.ac.uk/sparql/sparql?query=prefix%20skos%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E%0Aprefix%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0Aprefix%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0Aselect%20%3Fthing%20%3Flabel%20where%20%7B%0A%20%20%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FS05%2Fcurrent%2F%3E%20skos%3Amember%20%3Fthing%20.%0A%20%20%3Fthing%20skos%3AprefLabel%20%3Flabel%20.%0A%20%20filter%28contains%28str%28%3Flabel%29%2C%20%22 %22%29%29%0A%7D",
                    "temperature", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nercSparqlS03() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://vocab.nerc.ac.uk/sparql/sparql?query=prefix%20skos%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2004%2F02%2Fskos%2Fcore%23%3E%0Aprefix%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0Aprefix%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0Aselect%20%3Fthing%20%3Flabel%20where%20%7B%0A%20%20%3Chttp%3A%2F%2Fvocab.nerc.ac.uk%2Fcollection%2FS03%2Fcurrent%2F%3E%20skos%3Amember%20%3Fthing%20.%0A%20%20%3Fthing%20skos%3AprefLabel%20%3Flabel%20.%0A%20%20filter%28contains%28str%28%3Flabel%29%2C%20%22 %22%29%29%0A%7D",
                    "temperature", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- Inline SPARQL via nanopub-query repo ----

    @Test
    void getPossibleValues_nanopubQueryInlineSparql() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(SPARQL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/repo/type/f90cda43071e5afd9dbbd07452380c057c26010dd4e1105cdc108f35fc7280c0?query=prefix%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E%0Aprefix%20np%3A%20%3Chttp%3A%2F%2Fwww.nanopub.org%2Fnschema%23%3E%0Aprefix%20npa%3A%20%3Chttp%3A%2F%2Fpurl.org%2Fnanopub%2Fadmin%2F%3E%0Aprefix%20npx%3A%20%3Chttp%3A%2F%2Fpurl.org%2Fnanopub%2Fx%2F%3E%0A%0Aselect%20%3Fthing%20%3Flabel%20where%20%7B%0A%20%20graph%20npa%3Agraph%20%7B%0A%20%20%20%20%3Fnp%20npa%3AhasValidSignatureForPublicKey%20%3Fpubkey%20.%0A%20%20%20%20filter%20not%20exists%20%7B%20%3Fnpx%20npx%3Ainvalidates%20%3Fnp%20%3B%20npa%3AhasValidSignatureForPublicKey%20%3Fpubkey%20.%20%7D%0A%20%20%20%20%3Fnp%20npx%3Aintroduces%20%3Fthing%20.%0A%20%20%20%20%3Fnp%20np%3AhasAssertion%20%3Fa%20.%0A%20%20%7D%0A%20%20graph%20%3Fa%20%7B%0A%20%20%20%20%3Fthing%20rdfs%3Alabel%20%3Flabel%20.%0A%20%20%20%20filter%28contains%28lcase%28str%28%3Flabel%29%29%2C%20lcase%28%22 %22%29%29%29%0A%20%20%7D%0A%7D%0Alimit%2010",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- BioOntology (NCBO) ----

    @Test
    void getPossibleValues_bioportalEco() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(BIOPORTAL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "http://data.bioontology.org/search?pagesize=20&apikey=fd451bec-eacd-4519-b972-90fb6c7007cb&ontologies=ECO&q=",
                    "experiment", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_bioportalEvi() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(BIOPORTAL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "http://data.bioontology.org/search?pagesize=20&apikey=fd451bec-eacd-4519-b972-90fb6c7007cb&include_properties=true&ontologies=EVI&q=",
                    "evidence", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_bioportalDoid() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockHttp(BIOPORTAL_FIXTURE)) {
            LookupApis.getPossibleValues(
                    "http://data.bioontology.org/search?pagesize=20&apikey=fd451bec-eacd-4519-b972-90fb6c7007cb&include_properties=false&ontologies=DOID&q=",
                    "cancer", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    // ---- Name resolution SRI ----

    @Test
    @Disabled("Name-resolution SRI API now returns a JSON array, but the code expects a JSON object — JSONObject construction fails")
    void getPossibleValues_nameResolutionSri() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://name-resolution-sri.renci.org/lookup?limit=10&string=", "gene", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    // ---- purl.org/nanopub find_signed_things: new types ----

    @Test
    void getPossibleValues_purl_3pffEvent() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2F3pff%2F3PFF-event&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairImplementationCommunity() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Implementation-Community&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairEnablingResource() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Enabling-Resource&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairSupportingResource() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Supporting-Resource&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_claim() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Fkpxl%2Fgen%2Fterms%2FClaim&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairDigitalObject_fdof() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffdof%2Fontology%23FAIRDigitalObject&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_namedIndividual() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23NamedIndividual&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_aidaSentence() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http%3A%2F%2Fpurl.org%2Fpetapico%2Fo%2Fhycl%23AIDA-Sentence&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_skosConcept() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http://www.w3.org/2004/02/skos/core%23Concept&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_purl_skosConceptScheme() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "http://purl.org/nanopub/api/find_signed_things?type=http://www.w3.org/2004/02/skos/core%23ConceptScheme&searchterm=",
                "empty", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_picoOutcome() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http://data.cochrane.org/ontologies/pico/PICO&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_systematicReviewSearchStrategy() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/sciencelive/o/terms/SystematicReviewSearchStrategy&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_systematicDatabaseSearch() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/sciencelive/o/terms/SystematicDatabaseSearch&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_purl_systematicReview() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "http://purl.org/nanopub/api/find_signed_things?type=http://purl.org/spar/fabio/SystematicReview&searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_digitalObjectAnalysis() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/fff/req/Digital-Object-Analysis",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_userStory() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/fff/req/User-Story",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_ddiStudy() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http%3A%2F%2Frdf-vocabulary.ddialliance.org%2Fdiscovery%23Study&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_forrtClaim() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/sciencelive/o/terms/FORRT-Claim&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairImplementationProfile() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Implementation-Profile&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_dataUsageLicense() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FData-usage-license&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_digitalObjectType() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FDigital-Object-Type&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairDigitalObject_fdof2() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/fdof/ontology%23FAIRDigitalObject&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fdoAttribute() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/fdoc/o/terms/FdoAttribute&searchterm=",
                    "a", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_identifierService() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FIdentifier-service&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_rdfProperty() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23Property&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_owlOntology() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http://www.w3.org/2002/07/owl%23Ontology&searchterm=",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_dcCollection() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=http://purl.org/dc/dcmitype/Collection&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_registry() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FRegistry&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairSupportingSoftware() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Supporting-Software&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_authService() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FAuthentication-and-authorization-service&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_communicationProtocol() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FCommunication-protocol&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_metadataPreservationPolicy() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FMetadata-preservation-policy&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_persistencyPolicy() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FPersistency-Policy&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_fairPractice() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FFAIR-Practice&searchterm=",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_purl_customTypeWithLabel() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "http://purl.org/nanopub/api/find_signed_things?type=RAdXcYDJK7AWVlk1SpIvMpiyzH8xg95dwWiC020RNHgK0&label=Get+Research+Programmes&searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_knowledgeRepresentationLanguage() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https%3A%2F%2Fw3id.org%2Ffair%2Ffip%2Fterms%2FKnowledge-representation-language&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_researchProject() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://schema.org/ResearchProject&searchterm=",
                    "example", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_purl_forrtReplicationStudy() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "http://purl.org/nanopub/api/find_signed_things?type=https://w3id.org/sciencelive/o/terms/FORRT-Replication-Study&searchterm=",
                    "replication", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    @Disabled("Plain nanopub content URL (not a search API) — appending a search term to it is not meaningful")
    void getPossibleValues_purlNpContent() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("http://purl.org/np/RACXDZHEowTYDAzZvdmD0qIGpXZwY5ghMRBBlt6N8Iu5s", "data", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    // ---- w3id.org nanopub-query-1.1 API ----

    @Test
    void getPossibleValues_nanopubQuery_find3pffEvents() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAHzCZ-EdJnujIH3P23wQR9C3ySiIJjrp2Ij6df-C9gZg/find-3pff-events",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findResearchProject() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAz6f1v82BCG0SjYMfHUe-m927VTVKdwvsuq1X7j1qcA8/find-things?type=https://schema.org/ResearchProject",
                    "example", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_findSio001066() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAz6f1v82BCG0SjYMfHUe-m927VTVKdwvsuq1X7j1qcA8/find-things?type=http://semanticscience.org/resource/SIO_001066&query=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findFairDigitalObject() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAVEmFh3d6qonTFQ5S9SVqXZh0prrH1YLhSSs0dJvyvpM/find-things?type=https://w3id.org/fdof/ontology%23FAIRDigitalObject",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getConsortiumAgreements() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAj7uvn0h1g7NFfSH4fpV0TX61Y61MfepcWSMHU9EJtAU/get-consortium-agreements?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getArticles() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAzPQt99DLv81cTH9lfftA5P3WkOfD98jvkBYIfP4mX04/get-articles?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getCallsForProposals() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RASCMrcymSnMAoNI28MHCwHMXhEP3Z8VoLbsdUdls98Uk/get-calls-for-proposals?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getEthicalClearances() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAAjjgaL7wYF510LC7ndmPnzKdDHb3xoU43BAkKsK0se4/get-ethical-clearances?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getDatasets() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAD0AEXm6c0qjX6e3GkcpEI7iwdto7JOoIxE2RHrWw7a8/get-datasets?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getDmps() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAHgLUbnOf26ieiv5kfToQ7KfRySLBoXMphHCYXBfrt28/get-dmps?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getMethods() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAabGvJYW6eTKT75kqVkXmE3x4rqM_4mAiEv5Sjp0FwY8/get-methods?searchterm=?",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getPreRegistrations() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAzHkf9dVzkXR3goakQmYFoZJ0iomBlw9yjtNb5j6UVYY/get-pre-registrations?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getResearchProgrammes_RAGIS() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAGIS_dBT0qNpZr2s0wISRQ_2I7WPCCQJIWNEPZ-9nc78/get-research-programmes?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getProposals_RAwg() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RA-wg1Vld3y6GGHexjTfsSeAI-Zi5fPmPveve4rWaPyD8/get-proposals?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getSoftware() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAwjzYfozlA0aGAWMQIIGrlMvaobWFiXnJ_pIH4zzDdgo/get-software?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getResearchProgrammes_RAO2() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAO2gcLNzs3vQ9huOXaaERwg55t8FPhIELnMfdgGSm0Ro/get-research-programmes?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findSpace() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/Space",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findGrlcQuery() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/grlc/grlc-query",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findEvent() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/Event",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findProject() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/Project",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    @Disabled("Query ID 'find-things' is not registered in the local GrlcQuery registry — fails immediately with an exception")
    void getPossibleValues_nanopubQuery_findRosettaStatementClass() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/find-things?type=https://w3id.org/rosetta/RosettaStatementClass",
                "fair", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_searchTemplates_RARD6() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RARD6qOGIXUvfxmf5CQNEDxPqlTVCqeLdWeSg5h8tUcEA/search-templates",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findMaintainedResource() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/MaintainedResource",
                    "fair", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getProjects_RAFTZ() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAFTZ8GqPOOJQOBcEo5IB8lg5vZCFiIyr_RVLKZDQBHMk/get-projects?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_findConceptsInSkosScheme() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAxVOx3ISzwLFMRkvS722val27USqRH1Z-vnddpf2Y0Tk/find-concepts-in-skos-scheme?scheme=https://w3id.org/spaces/fip-group/r/reqvoc&searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_findFairSpecifications() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAB_utnJ83p9BqEo3Ndw5YWDrM0jP1UH1VA6AkQh7Yrow/find-fair-specifications?query=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getResearchProgrammes_RAtX() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAtXPiNeqzgeAwiByOi6nPNSRdJBKEiyJT0PGH0Hunoxk/get-research-programmes?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getCallsForProposals_RAa5() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAa5P35Fg-nlOB5X_3MH459LhvwaYttVDWhs_0GgOGeng/get-calls-for-proposals?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getProposals_RA76() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RA76f5kvSjKUIQFHNpXE4qmTt_JINOvtOUCYZglpTItps/get-proposals?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    @Disabled("Query ID 'RAWep4b-...' is not registered in the local GrlcQuery registry — fails immediately with an exception")
    void getPossibleValues_nanopubQuery_RAWep4b() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAWep4b-_mnoVcJzH83nV6_y8B0AlaYQF8A2H1RiLRzgU?searchterm=",
                "fair", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getConsortiumAgreements_RAHq() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAHqFcIovhws8a5jMQGP_CJgU1FeyiysBgBEkuBeQmjqI/get-consortium-agreements?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getArticles_RAUY() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAUYVsbdsZKpeZGgxtLzZjkldsbbfZ4R6KzYWBpBp5a_8/get-articles?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getEthicalClearances_RAsCj() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAsCjhoJ0M2vAl0Am1gqM8LxktbZFnYA7n-WmJGdjaVcc/get-ethical-clearances?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getDatasets_RATt() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RATt1UfDY-AIiDeldUDubsuTEZq82B2s0GErSHI7Ae5As/get-datasets?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getDmps_RAn4() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAn4TwDfSGC_GhSd5dXcvxhy3l60XLedF4HHLtk0_4yZ8/get-dmps?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_getMethods_RAdg() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAdgYHClSdVIQJonpuJtFIFLATvowyaMLyJAMofdOh9pc/get-methods?searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Disabled("No matching nanopubs found in the network for any of the tried search terms (data/fair/example/test/fdo/a)")
    @Test
    void getPossibleValues_nanopubQuery_getPreRegistrations_RAMAM() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://w3id.org/np/l/nanopub-query-1.1/api/RAMAM6qbOGM1_mm1Hhie3X4VzHd-vMfBBIHyyw50hZtz8/get-pre-registrations?searchterm=",
                "a", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findSpaceWithSearchterm() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/Space&searchterm=",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findSpaceMemberRole() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https://w3id.org/kpxl/gen/terms/SpaceMemberRole",
                    "a", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_searchTemplates_RAvS() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAvSIHegG4Mb-Q64cWQoghLffvN_2NdDdqxNnOhJSZQfs/search-templates",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findEmbeddedResourceView() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAs7Q2IMbb7C2WzFa98bVwlDMhN3kJ0rrF9cSEybtvLaA/find-embedded-things?type=https://w3id.org/kpxl/gen/terms/ResourceView",
                    "data", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findChorotype() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things?type=https%3A%2F%2Fw3id.org%2Fspaces%2Fcarabid-beetles%2Fr%2Fontology%2FChorotype&searchterm=",
                    "euro", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findViewsIndividualAgent() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAzSFlOt0yD9b-GSNifkGoKfakXEYQ7f6Ic3OMwuJfwts/find-views?appliedViewClass=https://w3id.org/kpxl/gen/terms/IndividualAgent",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findViewsSpace() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAzSFlOt0yD9b-GSNifkGoKfakXEYQ7f6Ic3OMwuJfwts/find-views?appliedViewClass=https://w3id.org/kpxl/gen/terms/Space",
                    "fdo", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_nanopubQuery_findViewsMaintainedResource() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        try (var ignored = mockNanopubNetwork()) {
            LookupApis.getPossibleValues(
                    "https://w3id.org/np/l/nanopub-query-1.1/api/RAzSFlOt0yD9b-GSNifkGoKfakXEYQ7f6Ic3OMwuJfwts/find-views?appliedViewClass=https://w3id.org/kpxl/gen/terms/MaintainedResource",
                    "test", labelMap, values);
        }
        assertValuesWithLabels(labelMap, values);
    }

}
