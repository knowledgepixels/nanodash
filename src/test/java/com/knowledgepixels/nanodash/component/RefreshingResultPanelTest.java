package com.knowledgepixels.nanodash.component;

import com.google.common.cache.Cache;
import com.knowledgepixels.nanodash.ApiCache;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        clearCacheField("failed");
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
        assertTrue(markup.contains("refresh-spinner"), markup);
        assertTrue(markup.contains("view-refreshing"), markup);
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
    @DisplayName("the poll timer stops once every panel has settled")
    void timerStopsWhenNothingIsWaiting() throws Exception {
        startPanel(response("alpha"));
        cacheAsCurrent(response("alpha"));

        runTimer();

        assertTrue(tester.getLastRenderedPage().getBehaviors(RefreshingResultPanel.RefreshPollTimer.class).isEmpty(),
                "the timer should have removed itself");
    }

}
