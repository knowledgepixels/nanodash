package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.WicketApplication;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * What a network view puts on the page: the graph as JSON inside its container, the colour
 * legend when the query types its nodes, and the empty state when it finds nothing
 * (issue #792). Chiefly a check that the component and its markup still agree, which no
 * amount of compiling would tell us.
 */
class QueryResultNetworkTest {

    private static final QueryRef QUERY_REF = new QueryRef("RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE/get-network");

    private WicketTester tester;
    private MockedStatic<GrlcQuery> grlcQueryMockedStatic;
    private MockedStatic<NanodashLink> nanodashLinkMockedStatic;

    @BeforeEach
    void setUp() {
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
    }

    @AfterEach
    void tearDown() {
        nanodashLinkMockedStatic.close();
        grlcQueryMockedStatic.close();
        tester.destroy();
    }

    private static ViewDisplay viewDisplay() {
        View view = mock(View.class);
        ViewDisplay viewDisplay = mock(ViewDisplay.class);
        when(viewDisplay.getView()).thenReturn(view);
        when(viewDisplay.getTitle()).thenReturn("🕸 Test network");
        when(viewDisplay.getDisplayWidth()).thenReturn(12);
        return viewDisplay;
    }

    private static ApiResponse response(String[] header, String[]... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(header);
        for (String[] row : rows) response.add(row);
        return response;
    }

    private String render(ApiResponse response) {
        tester.startComponentInPage(new QueryResultNetwork("comp", QUERY_REF, response, viewDisplay()));
        return tester.getLastResponseAsString();
    }

    @Test
    void graphIsEmbeddedAsJsonInTheContainer() {
        String html = render(response(
                new String[]{"source", "source_label", "target", "target_label"},
                new String[]{"https://example.org/a", "Ay", "https://example.org/b", "Bee"}));

        assertTrue(html.contains("network-view-content"), html);
        assertTrue(html.contains("application/json"), html);
        assertTrue(html.contains("\"nodes\""), html);
        assertTrue(html.contains("\"edges\""), html);
        assertTrue(html.contains("Ay"), html);
        assertTrue(html.contains("/explore?id\\u003dhttps://example.org/a")
                || html.contains("/explore?id=https://example.org/a"), html);
        assertTrue(html.contains("🕸 Test network"), html);
        assertFalse(html.contains("(nothing found)"), html);
    }

    @Test
    void typedNodesGetALegend() {
        String html = render(response(
                new String[]{"source", "source_type", "target", "target_type"},
                new String[]{"https://example.org/s", "https://w3id.org/kpxl/gen/terms/Space",
                        "https://example.org/a", "https://w3id.org/kpxl/gen/terms/IndividualAgent"}));

        assertTrue(html.contains("network-view-legend"), html);
        assertTrue(html.contains("network-view-swatch"), html);
        assertTrue(html.contains("Space"), html);
        assertTrue(html.contains("IndividualAgent"), html);
    }

    @Test
    void untypedNodesGetNoLegend() {
        String html = render(response(
                new String[]{"source", "target"},
                new String[]{"https://example.org/a", "https://example.org/b"}));

        assertFalse(html.contains("network-view-swatch"), html);
    }

    @Test
    void nothingFoundShowsTheEmptyStateAndNoCanvas() {
        String html = render(response(new String[]{"source", "target"}));

        assertTrue(html.contains("(nothing found)"), html);
        assertFalse(html.contains("network-view-content"), html);
    }

    /**
     * The layout engine is referenced by a path inside its webjar, which nothing would
     * check at compile time: a wrong one serves a 404 and draws no network at all. The
     * reference resolves "current" to the version actually on the classpath, so this also
     * fails if the dependency is dropped or moved.
     */
    @Test
    void theLayoutEngineIsOnTheClasspathWhereTheComponentLooksForIt() {
        for (String path : List.of("vis-network/current/standalone/umd/vis-network.min.js",
                "vis-network/current/styles/vis-network.min.css")) {
            String resolved = new WebjarsJavaScriptResourceReference(path).getName();
            assertFalse(resolved.contains("current"), "version not resolved: " + resolved);
            // The reference resolves to a servable path ("/webjars/..."); on the classpath
            // the webjar serves that from under META-INF/resources.
            assertNotNull(QueryResultNetwork.class.getClassLoader()
                            .getResource("META-INF/resources" + resolved),
                    "not on the classpath: " + resolved);
        }
    }

    /**
     * The counterpart of the escaping test in NetworkGraphTest, one level up: what reaches
     * the page must not be able to close the script block it sits in.
     */
    @Test
    void aLabelLookingLikeMarkupDoesNotCloseTheScriptBlock() {
        String html = render(response(
                new String[]{"source", "source_label", "target"},
                new String[]{"https://example.org/a", "</script><img src=x onerror=alert(1)>",
                        "https://example.org/b"}));

        // One script block opened for the graph data, one closing tag for it.
        int opened = countOccurrences(html, "<script type=\"application/json\"");
        assertTrue(opened == 1, "expected one data block, found " + opened);
        assertFalse(html.contains("onerror=alert(1)"), html);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) count++;
        return count;
    }

    @Test
    void truncatedResultSaysSo() {
        List<String[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i <= com.knowledgepixels.nanodash.NetworkGraph.MAX_EDGES; i++) {
            rows.add(new String[]{"https://example.org/n" + i, "https://example.org/n" + (i + 1)});
        }
        String html = render(response(new String[]{"source", "target"}, rows.toArray(new String[0][])));

        assertTrue(html.contains("only the first"), html);
    }

}
