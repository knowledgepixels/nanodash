package com.knowledgepixels.nanodash;

import com.github.openjson.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupApisTest {

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
        // Quoted phrase should not get the wildcard suffix
        String result = expandSearchTerm("\"covid virus\"");
        assertTrue(result.startsWith("( "));
        assertTrue(result.endsWith(" )"));
        assertFalse(result.endsWith("* )"), "Quoted phrase should not have wildcard: " + result);
    }

    // ---- getPossibleValues integration tests ----

    private void assertValuesWithLabels(Map<String, String> labelMap, List<String> values) {
        assertFalse(values.isEmpty(), "Expected at least one result");
        for (String v : values) {
            assertNotNull(labelMap.get(v), "Missing label for value: " + v);
        }
    }

    @Test
    @Disabled("EBI OLS v3 API (https://www.ebi.ac.uk/ols/api/) is currently unavailable (503)")
    void getPossibleValues_ebiOls() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://www.ebi.ac.uk/ols/api/select?q=", "heart", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("http")), "All URIs should be HTTP IRIs");
    }

    @Test
    void getPossibleValues_gbif() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://api.gbif.org/v1/species/suggest?q=", "homo", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://www.gbif.org/species/")),
                "All URIs should be GBIF species URIs");
    }

    @Test
    void getPossibleValues_catalogueOfLife() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://api.catalogueoflife.org/dataset/3LR/nameusage/search?q=", "canis", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://www.catalogueoflife.org/data/taxon/")),
                "All URIs should be Catalogue of Life taxon URIs");
    }

    @Test
    void getPossibleValues_vodex() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://vodex.petapico.org/nidx/query?rows=100&q=label:", "Carabidae", labelMap, values);
        assertValuesWithLabels(labelMap, values);
    }

    @Test
    void getPossibleValues_ror() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://api.ror.org/organizations?query=", "harvard", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://ror.org/")),
                "All URIs should be ROR organization URIs");
    }

    @Test
    void getPossibleValues_openaire() {
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues("https://api.openaire.eu/graph/v1/researchProducts?search=", "climate", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.startsWith("https://api.openaire.eu/graph/v1/researchProducts/")),
                "All URIs should be OpenAire research-product URIs");
    }

    @Test
    void getPossibleValues_wikidata() {
        // Wikidata uses the generic JSON fallback (concepturi + label fields)
        Map<String, String> labelMap = new HashMap<>();
        List<String> values = new ArrayList<>();
        LookupApis.getPossibleValues(
                "https://www.wikidata.org/w/api.php?action=wbsearchentities&language=en&format=json&limit=5&search=",
                "gene", labelMap, values);
        assertValuesWithLabels(labelMap, values);
        assertTrue(values.stream().allMatch(v -> v.contains("wikidata.org")),
                "All URIs should be Wikidata entity URIs");
    }

}
