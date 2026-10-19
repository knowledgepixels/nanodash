package com.knowledgepixels.nanodash.lookup.parser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The label of an OLS term is followed by its description when it has one, and stands alone
 * when it has none.
 */
class OlsParserTest {

    private static final String API = "https://www.ebi.ac.uk/ols/api/select?ontology=bto&q=";
    private static final String IRI = "http://purl.obolibrary.org/obo/BTO_0000831";

    private final OlsParser parser = new OlsParser();
    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    private void parse(String doc) {
        parser.parse("{\"response\":{\"docs\":[" + doc + "]}}", API, labels, values);
    }

    @Test
    void labelIsFollowedByTheFirstDescription() {
        parse("{\"iri\":\"" + IRI + "\",\"label\":\"liver\",\"description\":[\"A large organ.\",\"More.\"]}");
        assertEquals(List.of(IRI), values);
        assertEquals("liver - A large organ.", labels.get(IRI));
    }

    @Test
    void emptyDescriptionLeavesTheLabelAlone() {
        parse("{\"iri\":\"" + IRI + "\",\"label\":\"liver\",\"description\":[]}");
        assertEquals("liver", labels.get(IRI));
    }

    @Test
    void missingDescriptionLeavesTheLabelAlone() {
        parse("{\"iri\":\"" + IRI + "\",\"label\":\"liver\"}");
        assertEquals("liver", labels.get(IRI));
    }

}
