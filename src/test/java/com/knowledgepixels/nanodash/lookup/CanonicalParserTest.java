package com.knowledgepixels.nanodash.lookup;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An API without a parser of its own is read by the canonical rules.
 */
class CanonicalParserTest {

    private final CanonicalParser parser = new CanonicalParser();
    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    @Test
    void readsValueLabelAndDescriptionFromTheResultFields() {
        parser.parse("{\"results\":[{\"uri\":\"http://example.com/apple\",\"label\":\"Apple\",\"description\":\"A fruit\"}]}",
                "https://example.org/api?q=", labels, values);
        assertEquals(List.of("http://example.com/apple"), values);
        assertEquals("Apple - A fruit", labels.get("http://example.com/apple"));
    }

    @Test
    void prefersTheFirstOfTheAlternativeFieldNames() {
        parser.parse("{\"collection\":[{\"@id\":\"http://example.com/a\",\"uri\":\"http://example.com/other\"," +
                "\"prefLabel\":\"Preferred\",\"label\":\"Other\",\"definition\":\"Defined\"}]}",
                "https://example.org/api?q=", labels, values);
        assertEquals(List.of("http://example.com/a"), values);
        assertEquals("Preferred - Defined", labels.get("http://example.com/a"));
    }

    @Test
    void keepsTheDashSeparatingLabelAndDescriptionUnambiguous() {
        parser.parse("{\"search\":[{\"concepturi\":\"http://example.com/a\",\"label\":\"A - B\"}]}",
                "https://example.org/api?q=", labels, values);
        assertEquals("A -- B", labels.get("http://example.com/a"));
    }

    @Test
    void readsAtMostTenResults() {
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            if (i > 0) results.append(',');
            results.append("{\"uri\":\"http://example.com/").append(i).append("\"}");
        }
        parser.parse("{\"results\":[" + results + "]}", "https://example.org/api?q=", labels, values);
        assertEquals(10, values.size());
    }

    @Test
    void anAnswerWithoutValueFieldsGivesNoValues() {
        parser.parse("{\"MONDO:0005148\":[\"type 2 diabetes\"],\"search\":[\"something\"]}",
                "https://example.org/api?q=", labels, values);
        assertTrue(values.isEmpty());
    }

}
