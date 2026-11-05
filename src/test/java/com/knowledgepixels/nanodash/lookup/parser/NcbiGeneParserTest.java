package com.knowledgepixels.nanodash.lookup.parser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A gene looked up by its NCBI Gene ID is given by its identifiers.org URI and labelled with
 * its symbol, description and organism; a search term that names no gene finds nothing.
 */
class NcbiGeneParserTest {

    private static final String API = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gene&retmode=json&id=";

    private static final String ALSB = """
            "948604":{"uid":"948604","name":"alsB","description":"D-allose ABC transporter periplasmic binding protein",
             "status":"","currentid":"",
             "organism":{"scientificname":"Escherichia coli str. K-12 substr. MG1655","commonname":"","taxid":511145}}""";

    private final NcbiGeneParser parser = new NcbiGeneParser();
    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    private void parse(String response) {
        parser.parse(response, API, labels, values);
    }

    @Test
    void geneIsLabelledWithSymbolDescriptionAndOrganism() {
        parse("{\"header\":{\"type\":\"esummary\",\"version\":\"0.3\"},\"result\":{\"uids\":[\"948604\"]," + ALSB + "}}");
        String value = "https://identifiers.org/ncbigene:948604";
        assertEquals(List.of(value), values);
        assertEquals("alsB - D-allose ABC transporter periplasmic binding protein (Escherichia coli str. K-12 substr. MG1655)",
                labels.get(value));
    }

    @Test
    void searchTermThatIsNoIdFindsNothing() {
        parse("""
                {"header":{"type":"esummary","version":"0.3"},"error":"Invalid uid test at position= 0","result":{"uids":[]}}""");
        assertTrue(values.isEmpty());
    }

    @Test
    void severalWordsThatAreNoIdsFindNothing() {
        // The E-utilities give one "error" field per word, all with the same key.
        parse("""
                {"header":{"type":"esummary","version":"0.3"},"error":"Invalid uid abc at position= 0",
                 "error":"Invalid uid def at position= 3","result":{"uids":[]}}""");
        assertTrue(values.isEmpty());
    }

    @Test
    void idThatNamesNoGeneIsLeftOut() {
        parse("{\"result\":{\"uids\":[\"999999999\",\"948604\"],"
              + "\"999999999\":{\"uid\":\"999999999\",\"error\":\"cannot get document summary\"}," + ALSB + "}}");
        assertEquals(List.of("https://identifiers.org/ncbigene:948604"), values);
    }

    @Test
    void geneWithoutDescriptionOrOrganismIsLabelledWithItsSymbol() {
        parse("{\"result\":{\"uids\":[\"1\"],\"1\":{\"uid\":\"1\",\"name\":\"A1BG\",\"description\":\"\"}}}");
        assertEquals("A1BG", labels.get("https://identifiers.org/ncbigene:1"));
    }

}
