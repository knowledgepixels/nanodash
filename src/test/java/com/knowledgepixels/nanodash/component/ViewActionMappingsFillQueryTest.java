package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.ViewFillQueryTest;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * The link a result action's button carries (issue #690): the fill-query parameters bound
 * to the target, and — fixed on the way — the listing-driven mappings passed whole.
 */
class ViewActionMappingsFillQueryTest {

    private static final String EVENT = "https://example.org/events/conf-2026";
    private static final QueryRef LISTING = new QueryRef("RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/list-presentations", "resource", EVENT);

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    /** The smallest concrete result component: buttons are all this test reads. */
    private static QueryResult resultFor(View view) {
        return new QueryResult("r", LISTING, new ApiResponse(), new ViewDisplay(view)) {
            @Override
            protected void populateComponent() {
            }
        };
    }

    private static PageParameters paramsOf(QueryResult result, String label) {
        for (QueryResult.MenuAction a : result.getMenuActions()) {
            if (a.label().equals(label)) return a.params();
        }
        return fail("no button labelled " + label + " among " + result.getMenuActions());
    }

    @Test
    void fillQueryIsBoundToTheTargetAndPassedWithItsMappings() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = ViewFillQueryTest.loadFixture(utils, grlc, td);
            QueryResult result = resultFor(view);

            ViewActionMappings.addResultActions(result, new ViewDisplay(view), LISTING, EVENT, EVENT, null, null);

            PageParameters add = paramsOf(result, "add presentation...");
            assertEquals(new QueryRef(ViewFillQueryTest.FILL_QUERY_ID, "event", EVENT).getAsUrlString(),
                    add.get("fill-query").toString(), "the fill query is bound to the target through its target field");
            assertEquals("startDate:startDate location:!location", add.get("fill-query-mapping").toString());
            assertEquals(EVENT, add.get("param_event").toString(), "the ordinary target pre-fill is unaffected");

            PageParameters plain = paramsOf(result, "add note...");
            assertTrue(plain.get("fill-query").isNull(), "an action without a fill query passes none");
            assertTrue(plain.get("fill-query-mapping").isNull());
        }
    }

    /**
     * No target, nothing to bind the fill query to: a resource-less page (the general
     * Spaces page, the standalone view-results page) gets the button without the fill.
     */
    @Test
    void noTargetMeansNoFillQuery() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = ViewFillQueryTest.loadFixture(utils, grlc, td);
            QueryResult result = resultFor(view);

            ViewActionMappings.addResultActions(result, new ViewDisplay(view), LISTING, null, null, null, null);

            PageParameters add = paramsOf(result, "add presentation...");
            assertTrue(add.get("fill-query").isNull());
            assertTrue(add.get("param_event").isNull());
        }
    }

    /**
     * A result action with several listing-driven mappings used to pass only the raw first
     * literal, which the form then split on its first colon into nonsense. All mappings
     * travel now, in the one literal the form knows how to split.
     */
    @Test
    void allListingMappingsArePassed() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = ViewFillQueryTest.loadFixture(utils, grlc, td);
            QueryResult result = resultFor(view);

            ViewActionMappings.addResultActions(result, new ViewDisplay(view), LISTING, EVENT, EVENT, null, null);

            PageParameters add = paramsOf(result, "add presentation...");
            assertEquals(LISTING.getAsUrlString(), add.get("values-from-query").toString());
            assertEquals("series:series organiser:organiser", add.get("values-from-query-mapping").toString());
            assertEquals(List.of("series:series", "organiser:organiser"),
                    View.parseMappingLiteral(add.get("values-from-query-mapping").toString()));
        }
    }
}
