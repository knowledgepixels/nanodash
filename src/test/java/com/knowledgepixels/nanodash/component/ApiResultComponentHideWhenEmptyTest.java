package com.knowledgepixels.nanodash.component;

import com.google.common.cache.Cache;
import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * A view display may declare that it has nothing to say when its query finds nothing, and
 * then leaves the page instead of showing a titled box with "(nothing found)" in it — which
 * is what an Explore page about a bare URI used to be four of. It may also override that note
 * for the one view left speaking for the whole page.
 */
class ApiResultComponentHideWhenEmptyTest {

    private static final QueryRef QUERY_REF = new QueryRef("RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE/get-network");

    private static final String TITLE = "🕸 Test network";

    private WicketTester tester;
    private MockedStatic<GrlcQuery> grlcQueryMockedStatic;
    private MockedStatic<NanodashLink> nanodashLinkMockedStatic;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        // The query definition and the page an IRI leads to are both looked up over the
        // network; neither is what these tests are about.
        GrlcQuery query = mock(GrlcQuery.class);
        when(query.getLabel()).thenReturn("Get network");
        grlcQueryMockedStatic = mockStatic(GrlcQuery.class);
        grlcQueryMockedStatic.when(() -> GrlcQuery.get(any(QueryRef.class))).thenReturn(query);
        nanodashLinkMockedStatic = mockStatic(NanodashLink.class, CALLS_REAL_METHODS);
        nanodashLinkMockedStatic.when(() -> NanodashLink.getPageUrl(anyString(), any()))
                .thenAnswer(inv -> "/explore?id=" + inv.getArgument(0, String.class));
        clearCacheField("cachedResponses");
        clearCacheField("lastRefresh");
        clearCacheField("forcedRefresh");
    }

    @AfterEach
    void tearDown() {
        nanodashLinkMockedStatic.close();
        grlcQueryMockedStatic.close();
        tester.destroy();
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

    /** Leaves a response in the cache without marking it current, as a stale entry. */
    @SuppressWarnings("unchecked")
    private static void cacheAsStale(ApiResponse response) throws Exception {
        Field responses = ApiCache.class.getDeclaredField("cachedResponses");
        responses.setAccessible(true);
        ((Cache<String, ApiResponse>) responses.get(null)).put(QUERY_REF.getAsUrlString(), response);
    }

    private static ViewDisplay viewDisplay() {
        View view = mock(View.class);
        when(view.getTitle()).thenReturn(TITLE);
        return new ViewDisplay(view);
    }

    private static ApiResponse response(String[]... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"source", "target"});
        for (String[] row : rows) response.add(row);
        return response;
    }

    private static ApiResponse empty() {
        return response();
    }

    private static ApiResponse oneEdge() {
        return response(new String[]{"https://example.org/a", "https://example.org/b"});
    }

    private Component create(ApiResponse cached, ViewDisplay viewDisplay, boolean hideWhenEmpty) {
        return ApiResultComponent.create("comp", QUERY_REF, cached, viewDisplay.getTitle(),
                (id, r) -> new QueryResultNetwork(id, QUERY_REF, r, viewDisplay), hideWhenEmpty);
    }

    @Test
    @DisplayName("without the option an empty result still shows the view and its note")
    void theEmptyNoteIsTheDefault() {
        Component comp = create(empty(), viewDisplay(), false);
        tester.startComponentInPage(comp);

        String html = tester.getLastResponseAsString();
        assertTrue(comp.isVisible());
        assertTrue(html.contains("(nothing found)"), html);
        assertTrue(html.contains(TITLE), html);
    }

    @Test
    @DisplayName("with the option an empty result takes the whole panel off the page")
    void anEmptyViewLeavesThePage() {
        Component comp = create(empty(), viewDisplay().hiddenWhenEmpty(), true);
        tester.startComponentInPage(comp);

        String html = tester.getLastResponseAsString();
        assertFalse(comp.isVisible());
        assertFalse(html.contains("(nothing found)"), html);
        assertFalse(html.contains(TITLE), html);
    }

    @Test
    @DisplayName("the option only hides an empty view, never one with results")
    void resultsAreShownAsBefore() {
        Component comp = create(oneEdge(), viewDisplay().hiddenWhenEmpty(), true);
        tester.startComponentInPage(comp);

        String html = tester.getLastResponseAsString();
        assertTrue(comp.isVisible());
        assertTrue(html.contains(TITLE), html);
        assertTrue(html.contains("network-view-content"), html);
    }

    @Test
    @DisplayName("an outdated empty result waits for its refresh instead of being shown and then hidden")
    void aStaleEmptyResultWaitsForItsRefresh() throws Exception {
        cacheAsStale(empty());

        assertInstanceOf(ApiResultComponent.class, create(null, viewDisplay().hiddenWhenEmpty(), true));
    }

    @Test
    @DisplayName("an outdated result with rows is still shown while it refreshes")
    void aStaleResultWithRowsIsStillShown() throws Exception {
        cacheAsStale(oneEdge());

        assertInstanceOf(RefreshingResultPanel.class, create(null, viewDisplay().hiddenWhenEmpty(), true));
    }

    @Test
    @DisplayName("an outdated empty result is shown as before when the option is off")
    void aStaleEmptyResultIsShownWithoutTheOption() throws Exception {
        cacheAsStale(empty());

        assertInstanceOf(RefreshingResultPanel.class, create(null, viewDisplay(), false));
    }

    @Test
    @DisplayName("a view that is the only one left on its page can speak for the page")
    void theEmptyNoteCanBeOverridden() {
        String note = "No nanopublication mentions this yet.";
        Component comp = create(empty(), viewDisplay().withEmptyMessage(note), false);
        tester.startComponentInPage(comp);

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains(note), html);
        assertFalse(html.contains("(nothing found)"), html);
    }

    @Test
    @DisplayName("the overridden note does not turn up in a view that found something")
    void theOverriddenNoteStaysOutOfANonEmptyView() {
        String note = "No nanopublication mentions this yet.";
        Component comp = create(oneEdge(), viewDisplay().withEmptyMessage(note), false);
        tester.startComponentInPage(comp);

        String html = tester.getLastResponseAsString();
        assertFalse(html.contains(note), html);
    }

}
