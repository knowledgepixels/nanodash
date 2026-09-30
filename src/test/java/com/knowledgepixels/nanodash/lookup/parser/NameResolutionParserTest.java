package com.knowledgepixels.nanodash.lookup.parser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The name resolution format maps identifiers to their label and description.
 */
class NameResolutionParserTest {

    private final NameResolutionParser parser = new NameResolutionParser();
    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    @Test
    void turnsCuriesIntoIdentifiersOrgUrisAndReadsLabelAndDescription() {
        parser.parse("{\"MONDO:0005148\":[\"type 2 diabetes\",\"a diabetes\"]}",
                "https://name-resolution-sri.renci.org/lookup?string=", labels, values);
        assertEquals(List.of("https://identifiers.org/MONDO:0005148"), values);
        assertEquals("type 2 diabetes - a diabetes", labels.get("https://identifiers.org/MONDO:0005148"));
    }

    @Test
    void keepsUrisAndShortensLongDescriptions() {
        parser.parse("{\"http://example.com/a\":[\"A\",\"" + "x".repeat(100) + "\"]}",
                "https://name-resolution-sri.renci.org/lookup?string=", labels, values);
        assertEquals(List.of("http://example.com/a"), values);
        assertEquals("A - " + "x".repeat(77) + "...", labels.get("http://example.com/a"));
    }

}
