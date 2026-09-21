package com.knowledgepixels.nanodash.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests which representation an {@code Accept} header selects (issue #710).
 */
class RdfNegotiationTest {

    /**
     * Browsers and command-line clients that accept anything get the page, not RDF.
     */
    @Test
    void htmlWinsForBrowsersAndWildcards() {
        assertNull(RdfNegotiation.negotiate(null));
        assertNull(RdfNegotiation.negotiate(""));
        assertNull(RdfNegotiation.negotiate("*/*"));
        assertNull(RdfNegotiation.negotiate("text/html"));
        assertNull(RdfNegotiation.negotiate("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"));
        assertNull(RdfNegotiation.negotiate("image/png"));
        assertNull(RdfNegotiation.negotiate("application/json"));
    }

    /**
     * Graph-aware formats carry whole nanopublications, so they are not restricted to the
     * assertions.
     */
    @Test
    void graphFormatsCarryFullNanopubs() {
        RdfNegotiation.Variant trig = RdfNegotiation.negotiate("application/trig");
        assertEquals("trig", trig.format());
        assertFalse(trig.assertionsOnly());
        assertEquals("nq", RdfNegotiation.negotiate("application/n-quads").format());
        assertEquals("jsonld", RdfNegotiation.negotiate("application/ld+json").format());
        assertEquals("trix", RdfNegotiation.negotiate("application/trix").format());
    }

    /**
     * Triple-only formats cannot hold the named graphs, so they carry the assertions.
     */
    @Test
    void tripleFormatsCarryAssertionsOnly() {
        RdfNegotiation.Variant turtle = RdfNegotiation.negotiate("text/turtle");
        assertEquals("turtle", turtle.format());
        assertTrue(turtle.assertionsOnly());
        assertTrue(RdfNegotiation.negotiate("application/n-triples").assertionsOnly());
        assertTrue(RdfNegotiation.negotiate("application/rdf+xml").assertionsOnly());
    }

    /**
     * A Linked Data client's weighted header, with HTML allowed but not preferred, gets its
     * preferred RDF format.
     */
    @Test
    void honoursQualityWeights() {
        assertEquals("turtle", RdfNegotiation.negotiate("text/turtle;q=0.9,application/trig;q=0.8,text/html;q=0.1").format());
        assertEquals("trig", RdfNegotiation.negotiate("application/trig,text/html;q=0.1").format());
        assertNull(RdfNegotiation.negotiate("text/html,text/turtle;q=0.5"));
    }

    /**
     * A malformed header is no reason to fail the page.
     */
    @Test
    void malformedHeaderGetsHtml() {
        assertNull(RdfNegotiation.negotiate(";;;q=x"));
    }

}
