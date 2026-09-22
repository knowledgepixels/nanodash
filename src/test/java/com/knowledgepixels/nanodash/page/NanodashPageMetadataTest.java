package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    /**
     * Only a site marks its body: everywhere else the stylesheet's outbound-link rules stay off.
     */
    @Test
    void doesNotMarkTheBodyAsASite() {
        String document = renderedPage();
        assertFalse(document.matches("(?s).*<body[^>]*class=.*"), document);
    }

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

    /**
     * An error page describing itself with the given text, standing in for a page whose
     * description comes from a nanopublication.
     */
    public static class DescribedPage extends ErrorPage {

        static String description;

        /**
         * Creates the page with {@link #description} as its meta description.
         *
         * @param parameters the page parameters
         */
        public DescribedPage(PageParameters parameters) {
            super(parameters);
            setMetaDescription(description);
        }

    }

    /**
     * A description that ends the attribute it is written into cannot put markup into the
     * head; it stays text in every tag it appears in.
     */
    @Test
    void aDescriptionCannotBreakOutOfItsAttribute() {
        DescribedPage.description = "Q\"><b id=\"injected\">x</b>' & more";
        tester.startPage(DescribedPage.class);
        String document = tester.getLastResponse().getDocument();
        assertFalse(document.contains("<b id="), document);
        assertTrue(document.contains("<meta name=\"description\" content=\"Q&quot;&gt;x&#039; &amp; more\" />"), document);
        assertTrue(document.contains("property=\"og:description\" content=\"Q&quot;&gt;x&#039; &amp; more\""), document);
        assertTrue(document.contains("<meta name=\"twitter:description\" content=\"Q&quot;&gt;x&#039; &amp; more\" />"), document);
    }

    /**
     * An HTML description, as spaces and presentations often have, shows as its text in
     * search results and link previews, not as markup.
     */
    @Test
    void anHtmlDescriptionIsShownAsText() {
        DescribedPage.description = "<span>This year\u2019s theme, <strong>\"Interoperable Europe\"</strong>, comes at a key moment.</span>";
        tester.startPage(DescribedPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("<meta name=\"description\" content=\"This year\u2019s theme, &quot;Interoperable Europe&quot;, comes at a key moment.\" />"), document);
        assertFalse(document.contains("content=\"<span>"), document);
    }

    @Test
    void headTagEscapesEveryAttributeValue() {
        assertEquals("<link rel=\"a&quot;b\" href=\"https://x.org/?a=1&amp;b=&lt;2&gt;\" type=\"t&#039;\" />\n",
                NanodashPage.headTag("link", "rel", "a\"b", "href", "https://x.org/?a=1&b=<2>", "type", "t'").getString().toString());
        assertEquals("<meta name=\"description\" content=\"\" />\n",
                NanodashPage.headTag("meta", "name", "description", "content", null).getString().toString());
    }

    @Test
    void headTagRefusesAnAttributeWithoutValue() {
        assertThrows(IllegalArgumentException.class, () -> NanodashPage.headTag("meta", "name"));
    }

    @Test
    void plainTextKeepsBlocksApartAndDecodesEntities() {
        assertEquals("One two & three",
                NanodashPage.toMetaDescription("<p>One</p><p>two &amp;</p><ul><li>three</li></ul>"));
        assertEquals("a < b", NanodashPage.toMetaDescription("a &lt; b"));
        assertEquals("Line one Line two", NanodashPage.toMetaDescription("Line one<br/>Line two"));
    }

    @Test
    void plainTextDropsScripts() {
        assertEquals("Before after", NanodashPage.toMetaDescription("Before <script>alert(1)</script>after"));
    }

    @Test
    void metaDescriptionIsCutAfterTheMarkupIsRemoved() {
        String text = "word ".repeat(80).strip();
        String description = NanodashPage.toMetaDescription("<span>" + text + "</span>");
        assertTrue(description.endsWith("\u2026"), description);
        assertTrue(description.length() <= 301, description);
        assertFalse(description.contains("<"), description);
    }

    @Test
    void noMetaDescriptionWhenNothingIsLeft() {
        assertNull(NanodashPage.toMetaDescription(null));
        assertNull(NanodashPage.toMetaDescription("   "));
        assertNull(NanodashPage.toMetaDescription("<span> </span><br/>"));
    }

}
