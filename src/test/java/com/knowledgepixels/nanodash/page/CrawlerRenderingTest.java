package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.CrawlerRequest;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a crawler's request costs and what it gets (issue #804). Opening this application to
 * search engines is only safe because a crawled page queues no queries, so these are the
 * guarantees that have to keep holding.
 *
 * <p>One thing is not testable here: that a crawler's page is written straight into the
 * response instead of being buffered and redirected to. {@link WicketTester} always renders
 * in one pass, so both paths look the same to it; that one is checked against a running
 * server instead.</p>
 */
class CrawlerRenderingTest {

    private static final String GOOGLEBOT =
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
    private static final String BROWSER =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";

    /**
     * Records, while the request is still being served, what the application decided about
     * it. The decisions live on the request cycle, which is gone by the time the tester
     * hands back a response.
     */
    public static class RecordingPage extends PrivacyPage {

        static boolean crawler;
        static Boolean forcedRefresh;

        /**
         * @param parameters the page parameters
         */
        public RecordingPage(PageParameters parameters) {
            super(parameters);
            crawler = CrawlerRequest.isCrawler();
            forcedRefresh = RequestCycle.get().getMetaData(ApiCache.FORCE_REFRESH_ON_RELOAD);
        }

    }

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        tester.setFollowRedirects(false);
        RecordingPage.crawler = false;
        RecordingPage.forcedRefresh = null;
    }

    private void start(String userAgent, String cacheControl) {
        tester.getRequest().setHeader("User-Agent", userAgent);
        if (cacheControl != null) tester.getRequest().setHeader("Cache-Control", cacheControl);
        tester.startPage(RecordingPage.class);
    }

    /**
     * The identification itself: a crawler is recognised, a browser is not.
     */
    @Test
    void recognisesACrawler() {
        start(GOOGLEBOT, null);
        assertTrue(RecordingPage.crawler);
    }

    /**
     * And a browser is not one: a visitor taken for a crawler would be shown a page without
     * its not-yet-loaded sections and never see them arrive.
     */
    @Test
    void doesNotTakeABrowserForOne() {
        start(BROWSER, null);
        assertFalse(RecordingPage.crawler);
    }

    /**
     * Browsers send {@code Cache-Control: no-cache} when the reader hits reload, and that is
     * taken as a request to run the page's queries again. A crawler sends the same header
     * meaning something else entirely — do not serve me from your HTTP cache — and acting on
     * it is what turns a crawled page into a set of loading spinners and a fan-out across
     * the query fleet.
     */
    @Test
    void aCrawlersNoCacheHeaderDoesNotForceARefresh() {
        start(GOOGLEBOT, "no-cache");
        assertEquals(null, RecordingPage.forcedRefresh);
    }

    /**
     * The same header from a browser still means a reload.
     */
    @Test
    void aPersonsNoCacheHeaderStillForcesARefresh() {
        start(BROWSER, "no-cache");
        assertEquals(Boolean.TRUE, RecordingPage.forcedRefresh);
    }

    /**
     * The links on a crawled page carry no record of the route taken to them, so one target
     * is one URL instead of one per context and part a link might have picked up.
     */
    @Test
    void aCrawledPageLinksWithoutNavigationContext() {
        start(GOOGLEBOT, null);
        String document = tester.getLastResponse().getDocument();
        assertFalse(document.contains("context="), document);
        assertFalse(document.contains("part-label="), document);
    }

    /**
     * No page writes a session id into its URLs any more, for anyone: the container is told
     * to keep sessions in a cookie alone.
     */
    @Test
    void noUrlCarriesASessionId() {
        start(GOOGLEBOT, null);
        assertFalse(tester.getLastResponse().getDocument().toLowerCase().contains(";jsessionid="),
                tester.getLastResponse().getDocument());
    }

}
