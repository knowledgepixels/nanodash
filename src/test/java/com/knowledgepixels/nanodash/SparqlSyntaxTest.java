package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparqlSyntaxTest {

    private static final String VALID = "prefix dct: <http://purl.org/dc/terms/>\n"
            + "select ?thing where { ?thing dct:isPartOf ?_ontology_iri }";

    @Test
    void validQueryHasNothingToReport() {
        assertNull(SparqlSyntax.checkQuery(VALID));
    }

    // Placeholders are ordinary SPARQL variables, so a query template parses like any query.
    @Test
    void placeholdersAndValuesBlocksAreValid() {
        assertNull(SparqlSyntax.checkQuery(
                "select ?t where { values ?_kind_multi_iri { } ?t a ?_kind_multi_iri . ?t ?p ?__optional }"));
    }

    @Test
    void blankInputIsNotAnError() {
        assertNull(SparqlSyntax.checkQuery(null));
        assertNull(SparqlSyntax.checkQuery(""));
        assertNull(SparqlSyntax.checkQuery("   \n  "));
    }

    // The case from #284: a character that can't be seen, reported by the parser as a bare
    // number. Naming it is the only way the author can find it.
    @Test
    void nonBreakingSpaceIsNamed() {
        // A non-breaking space stands where a plain space belongs.
        String problem = SparqlSyntax.checkQuery("select ?thing where { ?thing ?p\u00A0?o }");
        assertNotNull(problem);
        assertTrue(problem.contains("U+00A0"), problem);
        assertTrue(problem.contains("NO-BREAK SPACE"), problem);
    }

    @Test
    void typographicQuoteIsNamed() {
        String problem = SparqlSyntax.checkQuery("select ?thing where { ?thing ?p “hello” }");
        assertNotNull(problem);
        assertTrue(problem.contains("U+201C"), problem);
        assertTrue(problem.contains("QUOTATION MARK"), problem);
    }

    // An ordinary syntax error has nothing invisible about it: the parser's own report is
    // passed on, without the list of grammar rules it was expecting.
    @Test
    void plainSyntaxErrorIsReportedWithoutNamingACharacter() {
        String problem = SparqlSyntax.checkQuery("select ?thing where { ?thing");
        assertNotNull(problem);
        assertTrue(problem.startsWith("This is not valid SPARQL."), problem);
        assertTrue(problem.contains("line 1"), problem);
        assertTrue(problem.lines().count() == 1, "the expected-tokens dump must be left out: " + problem);
    }

    @Test
    void summarizeKeepsTheFirstLineAndEndsIt() {
        assertNull(SparqlSyntax.summarize(null));
        assertNull(SparqlSyntax.summarize("   "));
        assertTrue(SparqlSyntax.summarize("Encountered \"<EOF>\" at line 1, column 5.\nWas expecting:\n \"(\"")
                .equals("Encountered \"<EOF>\" at line 1, column 5."));
        // Lexical errors trail off into a dangling comma that would run into what follows.
        assertTrue(SparqlSyntax.summarize("Lexical error at line 2, column 12.  Encountered: '160' (160),")
                .endsWith("(160)."));
    }

    @Test
    void asciiCharactersAreNotNamedBecauseTheParserAlreadyShowsThem() {
        assertNull(SparqlSyntax.explainOffendingCharacter("Lexical error at line 1, column 3.  Encountered: '64' (64),"));
        assertNull(SparqlSyntax.explainOffendingCharacter("Encountered \"<EOF>\" at line 1, column 5."));
        assertNull(SparqlSyntax.explainOffendingCharacter(null));
    }

}
