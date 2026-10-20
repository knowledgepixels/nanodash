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

import java.util.List;

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
        assertTrue(document.contains("<meta name=\"twitter:card\" content=\"summary"), document);
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

    /**
     * A page's canonical URL names what the page is about, not the route the visitor took to
     * it: without this the same subject is a different URL for every space, part or label a
     * link happened to carry (issue #804).
     */
    @Test
    void theCanonicalUrlDropsNavigationParameters() {
        PageParameters parameters = new PageParameters()
                .set("id", "https://example.org/thing")
                .set("context", "https://example.org/spaces/somewhere")
                .set("part", "https://example.org/parts/p1")
                .set("part-label", "P1")
                .set("label", "Thing")
                .set("forward-to-part", "true")
                .set("just-published", "https://example.org/np/RA123");
        tester.startPage(DescribedPage.class, parameters);
        String document = tester.getLastResponse().getDocument();
        String canonical = valueOf(document, "<link rel=\"canonical\" href=\"");
        assertTrue(canonical.contains("id=https://example.org/thing"), canonical);
        for (String dropped : List.of("context=", "part=", "part-label=", "label=", "forward-to-part=", "just-published=")) {
            assertFalse(canonical.contains(dropped), dropped + " should be gone from " + canonical);
        }
        // og:url says the same thing to a link preview as canonical says to a search engine.
        assertTrue(document.contains("property=\"og:url\" content=\"" + canonical.replace("&", "&amp;") + "\""), document);
    }

    /**
     * A page that is not about a subject of its own says so, rather than being left to
     * compete in search results with the pages that are.
     */
    @Test
    void aPageWithoutASubjectAsksNotToBeIndexed() {
        tester.startPage(ListPage.class, new PageParameters().set("type", "https://example.org/Type"));
        assertTrue(tester.getLastResponse().getDocument().contains("<meta name=\"robots\" content=\"noindex, follow\""),
                tester.getLastResponse().getDocument());
    }

    /**
     * Content pages carry no directive at all, which is how a crawler is left to index them.
     */
    @Test
    void aContentPageCarriesNoRobotsDirective() {
        tester.startPage(PrivacyPage.class);
        assertFalse(tester.getLastResponse().getDocument().contains("name=\"robots\""),
                tester.getLastResponse().getDocument());
    }

    /**
     * The tabs of a resource page are the same resource seen from a different side. Indexing
     * them would put three near-copies of every space, user and resource into the index.
     */
    @Test
    void aTabOtherThanTheMainOneIsNotIndexed() {
        tester.startPage(PrivacyPage.class, new PageParameters().set("tab", "about"));
        assertTrue(tester.getLastResponse().getDocument().contains("<meta name=\"robots\" content=\"noindex, follow\""),
                tester.getLastResponse().getDocument());
        tester.startPage(PrivacyPage.class, new PageParameters().set("tab", "content"));
        assertFalse(tester.getLastResponse().getDocument().contains("name=\"robots\""),
                tester.getLastResponse().getDocument());
    }

    /**
     * A page that has a picture offers it to link previews, which otherwise show a bare line
     * of text.
     */
    @Test
    void offersAPictureToLinkPreviews() {
        PicturedPage.imageUrl = "https://nanodash.example.org/images/social-card.png";
        PicturedPage.wide = true;
        tester.startPage(PicturedPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("property=\"og:image\" content=\"https://nanodash.example.org/images/social-card.png\""), document);
        assertTrue(document.contains("<meta name=\"twitter:image\" content=\"https://nanodash.example.org/images/social-card.png\""), document);
        assertTrue(document.contains("<meta name=\"twitter:card\" content=\"summary_large_image\""), document);
    }

    /**
     * A profile picture is a small square: the wide card would crop or stretch it, so a page
     * showing one asks for the small card instead.
     */
    @Test
    void asksForTheSmallCardWhereThePictureIsASquare() {
        PicturedPage.imageUrl = "https://example.org/profile.png";
        PicturedPage.wide = false;
        tester.startPage(PicturedPage.class);
        String document = tester.getLastResponse().getDocument();
        assertTrue(document.contains("property=\"og:image\" content=\"https://example.org/profile.png\""), document);
        assertTrue(document.contains("<meta name=\"twitter:card\" content=\"summary\""), document);
    }

    /**
     * A page offering the given picture, standing in for one whose subject has a picture of
     * its own. The website URL cannot be configured from a test, so the real default picture
     * is checked against a running instance instead.
     */
    public static class PicturedPage extends ErrorPage {

        static String imageUrl;
        static boolean wide;

        /**
         * @param parameters the page parameters
         */
        public PicturedPage(PageParameters parameters) {
            super(parameters);
        }

        @Override
        protected String getMetaImageUrl() {
            return imageUrl;
        }

        @Override
        protected boolean hasWideMetaImage() {
            return wide;
        }

    }

    /**
     * A profile picture may be an SVG carried in the page itself. That is no use to the
     * service rendering a preview card, which fetches the picture over HTTP and mostly
     * cannot draw SVG, so such a picture is left out rather than advertised.
     */
    @Test
    void doesNotOfferAnInlineOrSvgPictureToLinkPreviews() {
        assertNull(NanodashPage.sharableImageUrl("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="));
        assertNull(NanodashPage.sharableImageUrl("https://example.org/picture.svg"));
        assertNull(NanodashPage.sharableImageUrl(""));
        assertNull(NanodashPage.sharableImageUrl(null));
        assertEquals("https://example.org/picture.png", NanodashPage.sharableImageUrl("https://example.org/picture.png"));
    }

    /**
     * A page with no picture to offer — an instance that has not been told its own address
     * cannot build a URL a preview service could fetch — says nothing rather than something
     * wrong, and asks for the card shape that does not expect a picture.
     */
    @Test
    void offersNoPictureWhereThereIsNone() {
        PicturedPage.imageUrl = null;
        PicturedPage.wide = false;
        tester.startPage(PicturedPage.class);
        String document = tester.getLastResponse().getDocument();
        assertFalse(document.contains("og:image"), document);
        assertFalse(document.contains("twitter:image"), document);
        assertTrue(document.contains("<meta name=\"twitter:card\" content=\"summary\" />"), document);
    }

    /**
     * Every page says what language it is in.
     */
    @Test
    void declaresThePageLanguage() {
        assertTrue(renderedPage().contains("<html lang=\"en\""), renderedPage());
    }

    private static String valueOf(String document, String prefix) {
        int start = document.indexOf(prefix);
        assertTrue(start >= 0, "not found: " + prefix);
        start += prefix.length();
        return document.substring(start, document.indexOf('"', start));
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
