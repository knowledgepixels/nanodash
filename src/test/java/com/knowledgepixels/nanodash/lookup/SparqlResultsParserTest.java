package com.knowledgepixels.nanodash.lookup;

import com.github.openjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values and labels are read from the thing and label bindings of SPARQL JSON results.
 */
class SparqlResultsParserTest {

    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    @Test
    void singleResult() {
        SparqlResultsParser.parse(new JSONObject("{\"results\":{\"bindings\":[" +
                "{\"thing\":{\"value\":\"https://example.org/thing1\"},\"label\":{\"value\":\"Thing 1\"}}" +
                "]}}"), labels, values);
        assertEquals(List.of("https://example.org/thing1"), values);
        assertEquals("Thing 1", labels.get("https://example.org/thing1"));
    }

    @Test
    void multipleResults() {
        SparqlResultsParser.parse(new JSONObject("{\"results\":{\"bindings\":[" +
                "{\"thing\":{\"value\":\"https://example.org/thing1\"},\"label\":{\"value\":\"Thing 1\"}}," +
                "{\"thing\":{\"value\":\"https://example.org/thing2\"},\"label\":{\"value\":\"Thing 2\"}}" +
                "]}}"), labels, values);
        assertEquals(2, values.size());
        assertEquals("Thing 1", labels.get("https://example.org/thing1"));
        assertEquals("Thing 2", labels.get("https://example.org/thing2"));
    }

    @Test
    void emptyResults() {
        SparqlResultsParser.parse(new JSONObject("{\"results\":{\"bindings\":[]}}"), labels, values);
        assertTrue(values.isEmpty());
        assertTrue(labels.isEmpty());
    }

}
