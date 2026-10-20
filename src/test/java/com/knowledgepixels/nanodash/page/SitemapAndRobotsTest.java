package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two files a search engine reads before anything else (issue #804).
 */
class SitemapAndRobotsTest {

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
    }

    private String body(Class<? extends WebPage> page) {
        tester.startPage(page);
        return tester.getLastResponse().getDocument();
    }

    /**
     * A sitemap that does not parse is dropped by a search engine without a word, so this is
     * the assertion the rest rest on.
     */
    @Test
    void theSitemapIsWellFormedXml() throws Exception {
        assertFalse(locations(body(SitemapPage.class)).isEmpty(), "the sitemap lists nothing");
    }

    /**
     * Every URL must be absolute — a sitemap has nothing to resolve a relative one against —
     * must be listed once, and must be the page's canonical URL: listing a second way to
     * reach the same page works against the canonical tags rather than with them.
     */
    @Test
    void everyUrlIsAbsoluteCanonicalAndListedOnce() throws Exception {
        List<String> urls = locations(body(SitemapPage.class));
        Set<String> seen = new HashSet<>();
        for (String url : urls) {
            assertTrue(url.startsWith("http://") || url.startsWith("https://"), url + " is not absolute");
            assertFalse(url.toLowerCase().contains(";jsessionid="), url);
            assertFalse(url.contains("context="), url + " carries a navigation parameter");
            assertTrue(seen.add(url), url + " is listed twice");
        }
        assertTrue(urls.size() <= SitemapPage.MAX_URLS, "past the one-file limit: " + urls.size());
    }

    /**
     * The home page is the one URL that must be there whatever else this instance holds.
     */
    @Test
    void theSitemapListsTheHomePage() throws Exception {
        List<String> urls = locations(body(SitemapPage.class));
        assertTrue(urls.stream().anyMatch(url -> url.endsWith("/") || url.endsWith("/servlet")),
                "no home page among " + urls);
    }

    /**
     * Nanopublication landing pages are left out on purpose: there are tens of millions of
     * them and each costs a fetch from the registry to render. They are found by crawling
     * the pages that are listed, not by being enumerated here.
     */
    @Test
    void theSitemapDoesNotListNanopublicationPages() throws Exception {
        for (String url : locations(body(SitemapPage.class))) {
            assertFalse(url.contains("/explore"), url);
        }
    }

    /**
     * Without this line the sitemap is only found by someone submitting it by hand.
     */
    @Test
    void robotsTxtNamesTheSitemap() {
        assertTrue(RobotsPage.build("https://nanodash.example.org/sitemap.xml")
                .contains("\nSitemap: https://nanodash.example.org/sitemap.xml\n"));
    }

    /**
     * An instance that does not know its own address would otherwise name a sitemap at
     * whatever host the request happened to arrive on, which behind a reverse proxy is how
     * the proxy reached the container — of no use to a crawler.
     */
    @Test
    void robotsTxtNamesNoSitemapWithoutAConfiguredAddress() {
        assertFalse(RobotsPage.build(null).contains("Sitemap:"));
    }

    /**
     * What is kept from crawlers is the expensive and the session-bound. The content pages —
     * and nanopublication landing pages above all, which is what issue #168 asks for — stay
     * reachable.
     */
    @Test
    void robotsTxtClosesTheExpensivePathsAndOpensTheRest() {
        String robots = body(RobotsPage.class);
        for (String closed : List.of("/publish", "/query", "/download-rdf", "/calendar.ics", "/wicket/")) {
            assertTrue(robots.contains("Disallow: " + closed + "\n"), closed + " should be closed:\n" + robots);
        }
        for (String open : List.of("/explore", "/space", "/user", "/resource", "/part")) {
            assertFalse(robots.contains("Disallow: " + open + "\n"), open + " should stay open:\n" + robots);
        }
        assertTrue(robots.contains("Allow: /\n"), robots);
    }

    /**
     * Every path named here has to be one that exists, or the file quietly protects nothing.
     */
    @Test
    void everyDisallowedPathIsAMountedPath() {
        for (String path : RobotsPage.DISALLOWED) {
            assertTrue(path.startsWith("/"), path);
        }
        assertTrue(RobotsPage.DISALLOWED.contains(PublishPage.MOUNT_PATH), "the publish form should be closed");
        assertTrue(RobotsPage.DISALLOWED.contains(DownloadRdfPage.MOUNT_PATH), "RDF downloads should be closed");
        assertTrue(RobotsPage.DISALLOWED.contains(CalendarFeedPage.MOUNT_PATH), "calendar feeds should be closed");
        assertFalse(RobotsPage.DISALLOWED.contains(ExplorePage.MOUNT_PATH), "landing pages must stay open (#168)");
        assertFalse(RobotsPage.DISALLOWED.contains(SpacePage.MOUNT_PATH), "space pages must stay open");
    }

    /**
     * These two are names read from this file, never user agents anything sends, so opening
     * the site to crawlers would silently opt it into AI training unless they are answered
     * here — and answered before the group that opens it.
     */
    @Test
    void robotsTxtOptsOutOfAiTrainingFirst() {
        String robots = body(RobotsPage.class);
        int googleExtended = robots.indexOf("User-agent: Google-Extended");
        int appleExtended = robots.indexOf("User-agent: Applebot-Extended");
        int everyone = robots.indexOf("User-agent: *");
        assertTrue(googleExtended >= 0 && appleExtended >= 0, robots);
        assertTrue(googleExtended < everyone && appleExtended < everyone, robots);
    }

    private static List<String> locations(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        assertEquals("urlset", document.getDocumentElement().getLocalName());
        assertEquals("http://www.sitemaps.org/schemas/sitemap/0.9", document.getDocumentElement().getNamespaceURI());
        NodeList nodes = document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) urls.add(nodes.item(i).getTextContent());
        return urls;
    }

}
