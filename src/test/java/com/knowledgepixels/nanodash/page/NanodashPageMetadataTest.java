package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the search engine and link preview metadata that every Nanodash page renders
 * into its head (issue #704).
 */
class NanodashPageMetadataTest {

    private WicketTester tester;

    /**
     * Starts a tester against the real application, so that the metadata is rendered the
     * way a served page renders it.
     */
    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private String renderedPage() {
        tester.startPage(ErrorPage.class);
        return tester.getLastResponse().getDocument();
    }

    /**
     * A page without a subject of its own describes Nanodash, so that a search result for
     * it says what Nanodash is instead of nothing at all.
     */
    @Test
    void rendersTheSiteDescription() {
        String document = renderedPage();
        assertTrue(document.contains("<meta name=\"description\" content=\"Nanodash is a web client to browse and publish nanopublications"), document);
    }

    /**
     * Link previews read Open Graph and Twitter card tags rather than the description.
     */
    @Test
    void rendersOpenGraphAndTwitterCardTags() {
        String document = renderedPage();
        assertTrue(document.contains("property=\"og:site_name\" content=\"Nanodash\""), document);
        assertTrue(document.contains("property=\"og:type\" content=\"website\""), document);
        assertTrue(document.contains("property=\"og:title\""), document);
        assertTrue(document.contains("property=\"og:description\""), document);
        assertTrue(document.contains("property=\"og:url\" content=\"http"), document);
        assertTrue(document.contains("<meta name=\"twitter:card\" content=\"summary\""), document);
    }

    /**
     * The canonical URL is absolute and free of the visitor's session id, which would
     * otherwise make every crawl of a page a different URL.
     */
    @Test
    void rendersAnAbsoluteCanonicalUrlWithoutSessionId() {
        String document = renderedPage();
        assertTrue(document.contains("<link rel=\"canonical\" href=\"http"), document);
        assertFalse(document.toLowerCase().contains(";jsessionid="), document);
    }

    /**
     * A page's own title carries into the link preview, so that a shared link names what
     * it points at.
     */
    @Test
    void takesTheTitleFromThePage() {
        tester.startPage(ErrorPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("property=\"og:title\" content=\"Something went wrong"), document);
    }

}
