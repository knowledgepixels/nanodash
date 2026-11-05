package com.knowledgepixels.nanodash.lookup.parser;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A paper found in Europe PMC is given by its PMC ID, or by its PubMed ID if it has none, and
 * is labelled with its title in plain text, followed by its first author, journal and year.
 */
class EuropePmcParserTest {

    private static final String API = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?format=json&query=";

    private final EuropePmcParser parser = new EuropePmcParser();
    private final Map<String, String> labels = new HashMap<>();
    private final List<String> values = new ArrayList<>();

    private void parse(String... results) {
        parser.parse("{\"version\":\"6.9\",\"hitCount\":" + results.length
                     + ",\"resultList\":{\"result\":[" + String.join(",", results) + "]}}", API, labels, values);
    }

    @Test
    void paperInPmcIsGivenByItsPmcId() {
        parse("""
                {"id":"21516234","source":"MED","pmid":"21516234","pmcid":"PMC3079231",
                 "title":"Conformational studies of the robust 2-Cys peroxiredoxin Salmonella typhimurium AhpC.",
                 "authorString":"Nirudodhi S, Parsonage D, Karplus PA, Poole LB, Maier CS.",
                 "journalTitle":"Int J Mass Spectrom","pubYear":"2011"}""");
        String value = "https://identifiers.org/pmc:PMC3079231";
        assertEquals(List.of(value), values);
        assertEquals("Conformational studies of the robust 2-Cys peroxiredoxin Salmonella typhimurium AhpC"
                     + " - Nirudodhi S et al., Int J Mass Spectrom, 2011", labels.get(value));
    }

    @Test
    void paperNotInPmcIsGivenByItsPubMedId() {
        parse("""
                {"id":"42918114","source":"MED","pmid":"42918114","title":"Peroxiredoxin 6 silencing.",
                 "authorString":"Smith J.","journalTitle":"J Proteome Res","pubYear":"2026"}""");
        String value = "https://identifiers.org/pubmed:42918114";
        assertEquals(List.of(value), values);
        assertEquals("Peroxiredoxin 6 silencing - Smith J, J Proteome Res, 2026", labels.get(value));
    }

    @Test
    void paperWithNeitherIdIsLeftOut() {
        parse("""
                {"id":"PPR1345335","source":"PPR","doi":"10.64898/2026.10.16.759411","title":"A preprint.",
                 "bookOrReportDetails":{"publisher":"bioRxiv","yearOfPublication":2026}}""",
                """
                {"id":"42918114","source":"MED","pmid":"42918114","title":"A paper."}""");
        assertEquals(List.of("https://identifiers.org/pubmed:42918114"), values);
    }

    @Test
    void preprintIsCitedWithItsServer() {
        parse("""
                {"id":"PPR1","source":"PPR","pmid":"40000001","title":"A preprint.",
                 "authorString":"Glesener H, Dorn N.","bookOrReportDetails":{"publisher":"bioRxiv"},"pubYear":"2026"}""");
        assertEquals("A preprint - Glesener H et al., bioRxiv, 2026", labels.get("https://identifiers.org/pubmed:40000001"));
    }

    @Test
    void escapedMarkupInTitleBecomesPlainText() {
        parse("""
                {"pmid":"1","title":"Degradation of aflatoxin B&lt;sub&gt;1&lt;/sub&gt; by &lt;i&gt;Bacillus&lt;/i&gt; laccase &amp; p &lt; 0.05."}""");
        assertEquals("Degradation of aflatoxin B1 by Bacillus laccase & p < 0.05", labels.get("https://identifiers.org/pubmed:1"));
    }

    @Test
    void dashInTitleIsNotTakenForTheDescription() {
        parse("""
                {"pmid":"1","title":"Peroxiredoxins - a review.","authorString":"Smith J.","pubYear":"2020"}""");
        assertEquals("Peroxiredoxins -- a review - Smith J, 2020", labels.get("https://identifiers.org/pubmed:1"));
    }

    @Test
    void titleEndingInAQuestionMarkKeepsIt() {
        parse("""
                {"pmid":"1","title":"Is it?"}""");
        assertEquals("Is it?", labels.get("https://identifiers.org/pubmed:1"));
    }

    @Test
    void malformedIdsAreNotTurnedIntoUris() {
        parse("""
                {"pmcid":"not an id","pmid":"also not","title":"Odd."}""");
        assertTrue(values.isEmpty());
    }

    @Test
    void noHitsGiveNoValues() {
        parse();
        assertTrue(values.isEmpty());
    }

}
