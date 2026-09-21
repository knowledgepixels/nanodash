package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubImpl;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests how a page about a resource serves that resource's RDF (issue #710): by content
 * negotiation for clients that ask for it, and by links and an embedded block for the rest.
 */
class NanodashPageRdfTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        tester.setFollowRedirects(false);
        RdfSourceTestPage.declaration = new NanopubImpl(new File("src/test/resources/np-space-declaration.trig"), RDFFormat.TRIG);
    }

    private void start(String accept) {
        if (accept != null) tester.getRequest().setHeader("Accept", accept);
        tester.startPage(RdfSourceTestPage.class);
    }

    /**
     * A client asking for Turtle is sent to the assertions-only Turtle download, with a
     * 303 rather than a 302, as the download is another resource than the page.
     */
    @Test
    void redirectsTurtleRequestsToTheAssertionsDownload() {
        start("text/turtle");
        assertEquals(303, tester.getLastResponse().getStatus());
        String location = tester.getLastResponse().getHeader("Location");
        assertNotNull(location);
        assertTrue(location.startsWith("http"), location);
        assertTrue(location.contains("/download-rdf?"), location);
        assertTrue(location.contains("type=space"), location);
        assertTrue(location.contains("format=turtle"), location);
        assertTrue(location.contains("assertions"), location);
        assertFalse(location.toLowerCase().contains(";jsessionid="), location);
    }

    /**
     * A client asking for TriG gets the complete nanopublications.
     */
    @Test
    void redirectsTrigRequestsToTheFullDownload() {
        start("application/trig");
        assertEquals(303, tester.getLastResponse().getStatus());
        String location = tester.getLastResponse().getHeader("Location");
        assertTrue(location.contains("format=trig"), location);
        assertFalse(location.contains("assertions"), location);
    }

    /**
     * Both answers vary on the Accept header, so that a cache never hands the RDF
     * redirect to a browser or the page to a Linked Data client.
     */
    @Test
    void marksBothAnswersAsVaryingOnAccept() throws Exception {
        start("text/turtle");
        assertEquals("Accept", tester.getLastResponse().getHeader("Vary"));
        setUp();
        start("text/html");
        assertEquals(200, tester.getLastResponse().getStatus());
        assertEquals("Accept", tester.getLastResponse().getHeader("Vary"));
    }

    /**
     * A browser gets the page, which links every RDF representation for tools that
     * read HTML.
     */
    @Test
    void rendersAlternateLinksForEveryFormat() {
        start("text/html,*/*;q=0.8");
        assertEquals(200, tester.getLastResponse().getStatus());
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("<link rel=\"alternate\" href=\"http"), document);
        assertTrue(document.contains("type=\"application/trig\""), document);
        assertTrue(document.contains("type=\"text/turtle\""), document);
        assertTrue(document.contains("type=\"application/ld+json\""), document);
        assertTrue(document.contains("type=\"application/n-quads\""), document);
    }

    /**
     * The page embeds the resource's declaring assertion as JSON-LD, about the resource
     * IRI and not the page URL, with a pointer to the complete download.
     */
    @Test
    void embedsTheDeclarationAsJsonLd() {
        start(null);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("<script type=\"application/ld+json\">"), document);
        assertTrue(document.contains(RdfSourceTestPage.RESOURCE_IRI), document);
        assertTrue(document.contains("dataDump"), document);
        assertTrue(document.contains("Test Workshop"), document);
        assertTrue(document.contains("\\u003cscript>"), document);
    }

    /**
     * Without a known declaration there is no block, but the links still lead to the
     * download.
     */
    @Test
    void leavesOutTheBlockWithoutADeclaration() {
        RdfSourceTestPage.declaration = null;
        start(null);
        String document = tester.getLastResponse().getDocument();
        assertFalse(document.contains("application/ld+json\">"), document);
        assertTrue(document.contains("<link rel=\"alternate\""), document);
    }

}
