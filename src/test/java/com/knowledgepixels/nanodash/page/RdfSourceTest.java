package com.knowledgepixels.nanodash.page;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the JSON-LD a resource page embeds for its declaring nanopublications (issue #710).
 */
class RdfSourceTest {

    private static final String RESOURCE_IRI = "https://example.org/spaces/test-workshop";
    private static final String DUMP_URL = "https://nanodash.example.org/download-rdf?type=space&id=" + RESOURCE_IRI + "&format=trig";

    private static Nanopub declaration() throws Exception {
        return new NanopubImpl(new File("src/test/resources/np-space-declaration.trig"), RDFFormat.TRIG);
    }

    private static String jsonLd() throws Exception {
        return new RdfSource("space", RESOURCE_IRI, null, List.of(declaration())).toEmbeddedJsonLd(DUMP_URL);
    }

    /**
     * The document is about the resource itself, by its IRI, with the assertion's triples.
     */
    @Test
    void describesTheResourceByItsIri() throws Exception {
        String doc = jsonLd();
        assertTrue(doc.contains("\"@id\" : \"" + RESOURCE_IRI + "\"") || doc.contains("\"@id\": \"" + RESOURCE_IRI + "\""), doc);
        assertTrue(doc.contains("Test Workshop"), doc);
        assertTrue(doc.contains("2026-09-15T09:00:00+02:00"), doc);
    }

    /**
     * The nanopublication's own prefixes make up the context, without the ones that only
     * mean something inside the nanopublication.
     */
    @Test
    void keepsTheDeclaredPrefixesAsContext() throws Exception {
        String doc = jsonLd();
        assertTrue(doc.contains("\"@context\""), doc);
        assertTrue(doc.contains("\"gen\""), doc);
        assertFalse(doc.contains("\"sub\""), doc);
        assertFalse(doc.contains("\"this\""), doc);
    }

    /**
     * A pointer to the complete download lets a link-following tool get everything the
     * block leaves out.
     */
    @Test
    void pointsAtTheFullDownload() throws Exception {
        String doc = jsonLd();
        assertTrue(doc.contains("dataDump"), doc);
        assertTrue(doc.contains(DUMP_URL.replace("<", "\\u003c")), doc);
    }

    /**
     * A text value cannot end the script block the document sits in.
     */
    @Test
    void escapesAngleBracketsForTheScriptBlock() throws Exception {
        String doc = jsonLd();
        assertFalse(doc.contains("<"), doc);
        assertTrue(doc.contains("\\u003cscript>"), doc);
    }

    /**
     * Provenance and publication info stay out: the block is about the resource, not about
     * the nanopublication.
     */
    @Test
    void leavesOutTheOtherGraphs() throws Exception {
        String doc = jsonLd();
        assertFalse(doc.contains("wasAttributedTo"), doc);
        assertFalse(doc.contains("introduces"), doc);
    }

    /**
     * A resource whose declaration is not known has nothing to embed.
     */
    @Test
    void nothingToEmbedWithoutDeclarations() {
        assertNull(new RdfSource("space", RESOURCE_IRI, null, List.of()).toEmbeddedJsonLd(DUMP_URL));
    }

    /**
     * The download parameters name the resource, its context for a part, and the variant's
     * format and assertion switch.
     */
    @Test
    void buildsTheDownloadParameters() {
        RdfSource part = new RdfSource("part", "https://example.org/r/p1", "https://example.org/r", List.of());
        var turtle = part.downloadParameters(RdfNegotiation.negotiate("text/turtle"));
        assertEquals("part", turtle.get("type").toString());
        assertEquals("https://example.org/r/p1", turtle.get("id").toString());
        assertEquals("https://example.org/r", turtle.get("context").toString());
        assertEquals("turtle", turtle.get("format").toString());
        assertFalse(turtle.get("assertions").isNull());
        var trig = part.downloadParameters(RdfNegotiation.negotiate("application/trig"));
        assertEquals("trig", trig.get("format").toString());
        assertTrue(trig.get("assertions").isNull());
    }

}
