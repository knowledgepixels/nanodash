package com.knowledgepixels.nanodash.component;

import com.google.common.cache.Cache;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Outdated results are shown right away with a spinner beside them; when the refresh lands,
 * the content is only swapped if the results actually changed (issue #599).
 */
class RefreshingResultPanelTest {

    private static final String QUERY_ID = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";

    private WicketTester tester;
    private QueryRef queryRef;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester();
        queryRef = new QueryRef(QUERY_ID);
        clearCacheField("cachedResponses");
        clearCacheField("lastRefresh");
        clearCacheField("refreshStart");
        clearCacheField("runAfter");
        clearCacheField("failures");
        clearCacheField("forcedRefresh");
    }

    @SuppressWarnings("unchecked")
    private static void clearCacheField(String fieldName) throws Exception {
        Field f = ApiCache.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object obj = f.get(null);
        if (obj instanceof Cache<?, ?> cache) {
            cache.invalidateAll();
        } else if (obj instanceof ConcurrentMap<?, ?> map) {
            map.clear();
        } else if (obj instanceof Set<?> set) {
            set.clear();
        }
    }

    /** Puts a response into the cache as freshly refreshed, so no API call is made. */
    @SuppressWarnings("unchecked")
    private void cacheAsCurrent(ApiResponse response) throws Exception {
        Field responses = ApiCache.class.getDeclaredField("cachedResponses");
        responses.setAccessible(true);
        ((Cache<String, ApiResponse>) responses.get(null)).put(queryRef.getAsUrlString(), response);
        Field lastRefresh = ApiCache.class.getDeclaredField("lastRefresh");
        lastRefresh.setAccessible(true);
        ((ConcurrentMap<String, Long>) lastRefresh.get(null)).put(queryRef.getAsUrlString(), System.currentTimeMillis());
    }

    private static ApiResponse response(String... labels) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"label"});
        for (String label : labels) {
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("label", label);
            response.add(entry);
        }
        return response;
    }

    private static final ApiResultRenderer RENDERER = (markupId, response) -> {
        StringBuilder sb = new StringBuilder();
        for (ApiResponseEntry entry : response.getData()) {
            sb.append(entry.get("label")).append(" ");
        }
        return new org.apache.wicket.markup.html.basic.Label(markupId, sb.toString().trim());
    };

    private RefreshingResultPanel startPanel(ApiResponse staleResponse) {
        tester.startComponentInPage(new RefreshingResultPanel("panel", queryRef, staleResponse, RENDERER));
        return (RefreshingResultPanel) tester.getComponentFromLastRenderedPage("panel");
    }

    private void runTimer() {
        List<RefreshingResultPanel.RefreshPollTimer> timers =
                tester.getLastRenderedPage().getBehaviors(RefreshingResultPanel.RefreshPollTimer.class);
        assertFalse(timers.isEmpty(), "the panel should have installed a poll timer on the page");
        tester.executeBehavior(timers.getFirst());
    }

    @Test
    @DisplayName("the outdated results are on the page from the first render, with the spinner beside them")
    void outdatedResultsAreShownImmediately() {
        startPanel(response("alpha", "beta"));

        String markup = tester.getLastResponseAsString();
        assertTrue(markup.contains("alpha beta"), markup);
        assertTrue(markup.contains("refresh-spinner"), markup);
        assertTrue(markup.contains("view-refreshing"), markup);
    }

    @Test
    @DisplayName("a refresh that changed nothing takes the spinner away without touching the content")
    void unchangedRefreshLeavesContentAlone() throws Exception {
        RefreshingResultPanel panel = startPanel(response("alpha", "beta"));
        Component shownContent = panel.get("content");

        // Same results, different object: what a re-run of an unchanged query returns.
        cacheAsCurrent(response("alpha", "beta"));
        runTimer();

        assertSame(shownContent, panel.get("content"), "the content component should not have been rebuilt");
        assertFalse(panel.get("spinner").isVisible(), "the spinner should be gone");
    }

    @Test
    @DisplayName("a refresh with changed results swaps the content in")
    void changedRefreshSwapsInTheNewResults() throws Exception {
        RefreshingResultPanel panel = startPanel(response("alpha", "beta"));
        Component shownContent = panel.get("content");

        cacheAsCurrent(response("alpha", "beta", "gamma"));
        runTimer();

        assertNotSame(shownContent, panel.get("content"));
        assertTrue(panel.get("content").getDefaultModelObjectAsString().contains("gamma"));
        assertFalse(panel.get("spinner").isVisible());
    }

    @Test
    @DisplayName("with nothing cached, the view's title is shown with the spinner beside it")
    void coldLoadShowsTitleWithSpinner() {
        tester.startComponentInPage(
                ApiResultComponent.create("panel", queryRef, null, "📌 Relevant resources", RENDERER));

        String markup = tester.getLastResponseAsString();
        assertTrue(markup.contains("Relevant resources"), markup);
        // Beside the title, in the title row, exactly as a loaded view shows it.
        assertTrue(markup.contains("paneltitlerow"), markup);
        assertTrue(markup.contains("refresh-spinner"), markup);
    }

    @Test
    @DisplayName("a view without a title falls back to the bare spinner, the same icon")
    void coldLoadWithoutTitleShowsBareSpinner() {
        tester.startComponentInPage(ApiResultComponent.create("panel", queryRef, null, null, RENDERER));

        String markup = tester.getLastResponseAsString();
        assertTrue(markup.contains("refresh-spinner"), markup);
        assertFalse(markup.contains("paneltitlerow"), markup);
    }

    @Test
    @DisplayName("a view still waiting for its first results does not hold the thread")
    void lazyLoadDoesNotBlockOnTheResponse() throws Exception {
        // A fetch is in flight elsewhere and nothing is cached yet: the state in which the
        // panel used to sit in a sleep loop, holding a request thread for up to a minute.
        Field refreshStart = ApiCache.class.getDeclaredField("refreshStart");
        refreshStart.setAccessible(true);
        ((ConcurrentMap<String, Long>) refreshStart.get(null)).put(queryRef.getAsUrlString(), System.currentTimeMillis());

        Component panel = ApiResultComponent.create("panel", queryRef, null, "A title", RENDERER);
        tester.startComponentInPage(panel);

        long start = System.currentTimeMillis();
        assertFalse(((ApiResultComponent) panel).isContentReady(), "nothing to show yet");
        Component built = ((ApiResultComponent) panel).getLazyLoadComponent("content");
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 1000, "must not wait for the response, but took " + elapsed + "ms");
        assertNotNull(built);
    }

    /** Records a failed attempt at the query, with the next one not due for a minute. */
    @SuppressWarnings("unchecked")
    private void backOff() throws Exception {
        Class<?> c = Class.forName("com.knowledgepixels.nanodash.ApiCache$FailureState");
        var ctor = c.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Field failures = ApiCache.class.getDeclaredField("failures");
        failures.setAccessible(true);
        ((Cache<String, Object>) failures.get(null)).put(queryRef.getAsUrlString(),
                ctor.newInstance(1, System.currentTimeMillis() + 60_000));
    }

    @Test
    @DisplayName("a view with nothing cached reports a failure at once while its query is backed off (issue #889)")
    void backedOffQueryIsReportedAsFailed() throws Exception {
        backOff();

        Component panel = ApiResultComponent.create("panel", queryRef, null, "A title", RENDERER);
        tester.startComponentInPage(panel);

        assertTrue(((ApiResultComponent) panel).isContentReady(), "there is nothing to wait for");
        Component built = ((ApiResultComponent) panel).getLazyLoadComponent("content");
        assertTrue(built.getDefaultModelObjectAsString().contains("API call failed"), built.getDefaultModelObjectAsString());
    }

    @Test
    @DisplayName("a refresh of a backed-off query keeps the outdated content and takes the spinner away (issue #889)")
    void backedOffRefreshSettlesOnTheOutdatedContent() throws Exception {
        backOff();
        RefreshingResultPanel panel = startPanel(response("alpha", "beta"));
        Component shownContent = panel.get("content");

        runTimer();

        assertSame(shownContent, panel.get("content"));
        assertFalse(panel.get("spinner").isVisible(), "the spinner should be gone");
    }

    // A real view, with the title row the spinner and the note go in.
    private static final ApiResultRenderer PARAGRAPHS = (markupId, response) ->
            new QueryResultPlainParagraph(markupId, new QueryRef(QUERY_ID), response, new ViewDisplay(10));

    private static ApiResponse paragraph(String title) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"title", "content"});
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("title", title);
        entry.add("content", "<p>Some prose.</p>");
        response.add(entry);
        return response;
    }

    // The view looks its query up for its title, which would otherwise mean fetching it.
    private static MockedStatic<GrlcQuery> mockQueryLookup() {
        GrlcQuery query = mock(GrlcQuery.class);
        when(query.getLabel()).thenReturn("Paragraphs");
        MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
        grlc.when(() -> GrlcQuery.get(any(QueryRef.class))).thenReturn(query);
        return grlc;
    }

    private RefreshingResultPanel startParagraphPanel(ApiResponse staleResponse) {
        tester.startComponentInPage(new RefreshingResultPanel("panel", queryRef, staleResponse, PARAGRAPHS));
        return (RefreshingResultPanel) tester.getComponentFromLastRenderedPage("panel");
    }

    @Test
    @DisplayName("results served while the query is failing say they are older than they look (issue #889)")
    void viewOfResultsFromBeforeAFailureCarriesTheNote() throws Exception {
        try (MockedStatic<GrlcQuery> grlc = mockQueryLookup()) {
            backOff();
            tester.startComponentInPage(PARAGRAPHS.render("view", paragraph("Old")));

            String markup = tester.getLastResponseAsString();
            assertTrue(markup.contains("class=\"refresh-failed\""), markup);
            assertTrue(markup.contains(QueryResult.REFRESH_FAILED_NOTE), markup);
            assertFalse(markup.contains("refresh-spinner"), markup);
        }
    }

    @Test
    @DisplayName("results of a query that is not failing carry no note")
    void viewOfCurrentResultsHasNoNote() throws Exception {
        try (MockedStatic<GrlcQuery> grlc = mockQueryLookup()) {
            tester.startComponentInPage(PARAGRAPHS.render("view", paragraph("Current")));

            String markup = tester.getLastResponseAsString();
            assertFalse(markup.contains("refresh-failed"), markup);
            assertFalse(markup.contains(QueryResult.REFRESH_FAILED_NOTE), markup);
        }
    }

    @Test
    @DisplayName("a refresh that fails swaps the spinner for the note (issue #889)")
    void failedRefreshLeavesTheNote() throws Exception {
        try (MockedStatic<GrlcQuery> grlc = mockQueryLookup()) {
            ApiResponse stale = paragraph("Old");
            cacheAsCurrent(stale);
            RefreshingResultPanel panel = startParagraphPanel(stale);
            Component shownContent = panel.get("content");
            String first = tester.getLastResponseAsString();
            assertTrue(first.contains("refresh-spinner"), first);
            assertFalse(first.contains("refresh-failed"), first);

            // The refresh failed: the cache goes on handing out what it had.
            backOff();
            runTimer();

            assertSame(shownContent, panel.get("content"));
            String update = tester.getLastResponseAsString();
            assertTrue(update.contains("class=\"refresh-failed\""), update);
            assertTrue(update.contains(QueryResult.REFRESH_FAILED_NOTE), update);
        }
    }

    @Test
    @DisplayName("a refresh that succeeds without changes clears a note left by an earlier failure")
    void successfulRefreshClearsTheNote() throws Exception {
        try (MockedStatic<GrlcQuery> grlc = mockQueryLookup()) {
            ApiResponse stale = paragraph("Old");
            cacheAsCurrent(stale);
            backOff();
            startParagraphPanel(stale);

            // The next attempt succeeded, with the same results.
            clearCacheField("failures");
            cacheAsCurrent(paragraph("Old"));
            runTimer();

            String update = tester.getLastResponseAsString();
            assertFalse(update.contains("refresh-failed"), update);
            assertFalse(update.contains(QueryResult.REFRESH_FAILED_NOTE), update);
        }
    }

    @Test
    @DisplayName("a refresh that times out also leaves the note")
    @SuppressWarnings("unchecked")
    void timedOutRefreshLeavesTheNote() throws Exception {
        try (MockedStatic<GrlcQuery> grlc = mockQueryLookup()) {
            // A fetch is still running elsewhere, and nothing current is cached.
            Field refreshStart = ApiCache.class.getDeclaredField("refreshStart");
            refreshStart.setAccessible(true);
            ((ConcurrentMap<String, Long>) refreshStart.get(null)).put(queryRef.getAsUrlString(), System.currentTimeMillis());
            RefreshingResultPanel panel = startParagraphPanel(paragraph("Old"));
            Field deadline = RefreshingResultPanel.class.getDeclaredField("deadline");
            deadline.setAccessible(true);
            deadline.setLong(panel, System.currentTimeMillis() - 1);

            runTimer();

            String update = tester.getLastResponseAsString();
            assertTrue(update.contains(QueryResult.REFRESH_FAILED_NOTE), update);
        }
    }

    @Test
    @DisplayName("the poll timer stops once every panel has settled")
    void timerStopsWhenNothingIsWaiting() throws Exception {
        startPanel(response("alpha"));
        cacheAsCurrent(response("alpha"));

        runTimer();

        assertTrue(tester.getLastRenderedPage().getBehaviors(RefreshingResultPanel.RefreshPollTimer.class).isEmpty(),
                "the timer should have removed itself");
    }

}
