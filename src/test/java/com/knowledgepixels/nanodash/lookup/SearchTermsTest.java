package com.knowledgepixels.nanodash.lookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A search term is turned into a full-text query matching all of its words.
 */
class SearchTermsTest {

    @Test
    void singleWord() {
        assertEquals("( covid* )", SearchTerms.expand("covid"));
    }

    @Test
    void twoWords() {
        assertEquals("( covid AND virus* )", SearchTerms.expand("covid virus"));
    }

    @Test
    void extraWhitespace() {
        assertEquals("( covid* )", SearchTerms.expand("  covid  "));
    }

    @Test
    void quotedPhrase() {
        String result = SearchTerms.expand("\"covid virus\"");
        assertTrue(result.startsWith("( "));
        assertTrue(result.endsWith(" )"));
        assertFalse(result.endsWith("* )"), "Quoted phrase should not have wildcard: " + result);
    }

}
