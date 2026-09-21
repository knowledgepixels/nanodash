package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Page sources in action mappings: a mapping whose source begins with {@code @} takes its
 * value from the page the view is on rather than from a result row. The case this exists
 * for is the {@code ♻ override...} action of a view showing one nanopub's content, which
 * has to name that nanopub — a result action has no row to read it from.
 */
class ViewActionMappingsPageSourceTest {

    private static final String NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPS1";
    private static final String VIEW = NP + "/view";
    private static final String VIEW_QUERY = "https://w3id.org/np/RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/get-presentation-details";
    private static final String VIEW_QUERY_ID = "RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/get-presentation-details";
    private static final String ACTION_TEMPLATE = "https://w3id.org/np/RAttttttttttttttttttttttttttttttttttttttttttt";
    private static final IRI ENTRY_ACTION = SimpleValueFactory.getInstance().createIRI(NP + "/entryOverrideAction");

    /** The nanopub the view is showing, and the template it was made with. */
    private static final String SOURCE_NP = "https://w3id.org/np/RAmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm";
    private static final String SOURCE_TEMPLATE = "https://w3id.org/np/RAssssssssssssssssssssssssssssssssssssssssssss";

    private static final String RESOURCE = "https://w3id.org/np/RAmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm/a-talk";
    private static final String CONTEXT = "https://w3id.org/spaces/example";

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
    }

    /**
     * Loads the fixture view with its query and action template stubbed out, and the
     * nanopub the view shows resolving to a nanopub made with {@code sourceTemplate} (null
     * for one that names no template).
     */
    private static View loadFixture(MockedStatic<Utils> utils, MockedStatic<GrlcQuery> grlc,
            MockedStatic<TemplateData> td, String sourceTemplate) throws Exception {
        Nanopub np = new NanopubImpl(new File("src/test/resources/np-page-source-view.trig"), RDFFormat.TRIG);
        utils.when(() -> Utils.getAsNanopub(NP)).thenReturn(np);
        utils.when(() -> Utils.getAsNanopub(SOURCE_NP)).thenReturn(mock(Nanopub.class));
        utils.when(() -> Utils.menuEntryIconBodyHtml(anyString())).thenCallRealMethod();
        GrlcQuery viewQuery = mock(GrlcQuery.class);
        when(viewQuery.getQueryId()).thenReturn(VIEW_QUERY_ID);
        grlc.when(() -> GrlcQuery.get(VIEW_QUERY)).thenReturn(viewQuery);
        Template template = mock(Template.class);
        when(template.getId()).thenReturn(ACTION_TEMPLATE);
        TemplateData templateData = mock(TemplateData.class);
        when(templateData.getTemplate(anyString())).thenReturn(template);
        when(templateData.getTemplateId(any())).thenReturn(sourceTemplate == null ? null : Values.iri(sourceTemplate));
        td.when(TemplateData::get).thenReturn(templateData);
        return View.get(VIEW, false);
    }

    /** The query ref a page builds for this view, with or without the source nanopub. */
    private static QueryRef queryRef(String sourceNp) {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("resource", RESOURCE);
        if (sourceNp != null) params.put("resourceNp", sourceNp);
        return new QueryRef(VIEW_QUERY_ID, params);
    }

    /** A result whose rows carry the given values in the two action columns. */
    private static ApiResponse rows(String... targetsAndTemplates) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{"property", "override_target", "override_template"});
        for (int i = 0; i < targetsAndTemplates.length; i += 2) {
            ApiResponseEntry row = new ApiResponseEntry();
            row.add("property", "a value");
            if (targetsAndTemplates[i] != null) row.add("override_target", targetsAndTemplates[i]);
            if (targetsAndTemplates[i + 1] != null) row.add("override_template", targetsAndTemplates[i + 1]);
            response.add(row);
        }
        return response;
    }

    private static QueryResult resultFor(View view, QueryRef queryRef) {
        return resultFor(view, queryRef, new ApiResponse());
    }

    private static QueryResult resultFor(View view, QueryRef queryRef, ApiResponse response) {
        return new QueryResult("r", queryRef, response, new ViewDisplay(view)) {
            @Override
            protected void populateComponent() {
            }
        };
    }

    private static PageParameters paramsOf(QueryResult result, String label) {
        for (QueryResult.MenuAction a : result.getMenuActions()) {
            if (a.label().equals(label)) return a.params();
        }
        return null;
    }

    private static QueryResult resultActions(MockedStatic<Utils> utils, MockedStatic<GrlcQuery> grlc,
            MockedStatic<TemplateData> td, String sourceNp, String sourceTemplate) throws Exception {
        return resultActions(utils, grlc, td, sourceNp, sourceTemplate, new ApiResponse());
    }

    private static QueryResult resultActions(MockedStatic<Utils> utils, MockedStatic<GrlcQuery> grlc,
            MockedStatic<TemplateData> td, String sourceNp, String sourceTemplate, ApiResponse response) throws Exception {
        View view = loadFixture(utils, grlc, td, sourceTemplate);
        QueryRef queryRef = queryRef(sourceNp);
        QueryResult result = resultFor(view, queryRef, response);
        ViewActionMappings.addResultActions(result, new ViewDisplay(view), queryRef, RESOURCE, CONTEXT, null, null);
        return result;
    }

    @Test
    void overrideActionOpensTheNanopubTheViewIsShowing() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, SOURCE_NP, SOURCE_TEMPLATE);

            PageParameters override = paramsOf(result, "♻ override...");
            assertNotNull(override);
            assertEquals(SOURCE_NP, override.get("override").toString());
            // The source's own template wins over the action's declared fallback, and stays
            // resolved forward to its latest version.
            assertEquals(SOURCE_TEMPLATE, override.get("template").toString());
            assertEquals("latest", override.get("template-version").toString());
        }
    }

    /**
     * A fill mode takes every field from the source nanopub, so the target field is not
     * passed on top of them — it could only overwrite a filled field of the same name.
     */
    @Test
    void fillModeActionCarriesNoTargetField() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, SOURCE_NP, SOURCE_TEMPLATE);

            assertTrue(paramsOf(result, "♻ override...").get("param_resource").isNull());
            assertTrue(paramsOf(result, "derive...").get("param_resource").isNull());
        }
    }

    @Test
    void actionIsHiddenWhenThePageHasNoSourceNanopub() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, SOURCE_TEMPLATE);

            assertNull(paramsOf(result, "♻ override..."));
            assertNull(paramsOf(result, "derive..."));
        }
    }

    /** "x:" is what ViewList fills in when the page has no nanopub: no source, no action. */
    @Test
    void sentinelSourceHidesTheAction() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, "x:", SOURCE_TEMPLATE);

            assertNull(paramsOf(result, "♻ override..."));
        }
    }

    /**
     * A source nanopub naming no template leaves the override form nothing to open, so the
     * action goes — while an action not asking for the template is unaffected.
     */
    @Test
    void missingSourceTemplateHidesOnlyTheActionThatNeedsIt() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, SOURCE_NP, null);

            assertNull(paramsOf(result, "♻ override..."));
            assertNotNull(paramsOf(result, "derive..."));
        }
    }

    /**
     * Page sources are applied to the link; only the ordinary column mappings are handed to
     * the form to apply against the query's rows.
     */
    @Test
    void columnMappingsStillGoToTheFormWithoutThePageSources() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, SOURCE_NP, SOURCE_TEMPLATE);

            PageParameters mixed = paramsOf(result, "derive...");
            assertEquals(SOURCE_NP, mixed.get("derive-a").toString());
            assertEquals("series:series", mixed.get("values-from-query-mapping").toString());
            assertEquals(queryRef(SOURCE_NP).getAsUrlString(), mixed.get("values-from-query").toString());
        }
    }

    /** A page source is not a result column, so there is no column of its name to hide. */
    @Test
    void pageSourcesAreNotMappingSourceColumns() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = loadFixture(utils, grlc, td, SOURCE_TEMPLATE);

            assertEquals(java.util.Set.of("series", "override_target", "override_template"),
                    view.getActionMappingSourceColumns());
        }
    }

    @Test
    void entryActionsResolvePageSourcesToo() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = loadFixture(utils, grlc, td, SOURCE_TEMPLATE);
            ApiResponseEntry row = mock(ApiResponseEntry.class);

            PageParameters params = new PageParameters();
            assertTrue(ViewActionMappings.applyEntryMappings(view, ENTRY_ACTION, row, params, queryRef(SOURCE_NP)));
            assertEquals(SOURCE_NP, params.get("override").toString());

            // Nothing to resolve it against: the action goes, rather than opening a form on
            // no source.
            PageParameters none = new PageParameters();
            assertFalse(ViewActionMappings.applyEntryMappings(view, ENTRY_ACTION, row, none, queryRef(null)));
        }
    }

    /**
     * The other way round: the query decides which nanopub the action acts on, returns it as
     * a column, and the action reads it back — no page-level resolution involved.
     */
    @Test
    void resultColumnsCarryTheActionTarget() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, null,
                    rows(SOURCE_NP, SOURCE_TEMPLATE, SOURCE_NP, SOURCE_TEMPLATE));

            PageParameters override = paramsOf(result, "♻ override result...");
            assertNotNull(override);
            assertEquals(SOURCE_NP, override.get("override").toString());
            assertEquals(SOURCE_TEMPLATE, override.get("template").toString());
            // It needs nothing from the page: the page-source action is gone here, this one is not.
            assertNull(paramsOf(result, "♻ override..."));
        }
    }

    /**
     * A column that differs from row to row is a property of a row, not of the view, so a
     * view-level action cannot speak for it.
     */
    @Test
    void aColumnThatVariesAcrossRowsHidesTheAction() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, null,
                    rows(SOURCE_NP, SOURCE_TEMPLATE, SOURCE_NP + "x", SOURCE_TEMPLATE));

            assertNull(paramsOf(result, "♻ override result..."));
        }
    }

    @Test
    void emptyResultsHideTheAction() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, null, new ApiResponse());

            assertNull(paramsOf(result, "♻ override result..."));
        }
    }

    /**
     * A column feeding an action is action data: it stays out of the rendered table, exactly
     * like an ordinary mapping source.
     */
    @Test
    void resultSourceColumnsAreHiddenFromTheTable() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = loadFixture(utils, grlc, td, SOURCE_TEMPLATE);

            assertEquals(java.util.Set.of("series", "override_target", "override_template"),
                    view.getActionMappingSourceColumns());
        }
    }

    /**
     * An action whose template comes from the result needs no declared one — which is also
     * what keeps it out of older Nanodash versions, whose only way to hide an action is a
     * missing template. Publishing such a view ahead of the code release is then harmless.
     */
    @Test
    void anActionCanTakeItsTemplateFromTheResultWithoutDeclaringOne() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, null,
                    rows(SOURCE_NP, SOURCE_TEMPLATE));

            PageParameters params = paramsOf(result, "♻ override templateless...");
            assertNotNull(params);
            assertEquals(SOURCE_TEMPLATE, params.get("template").toString());
            assertEquals(SOURCE_NP, params.get("override").toString());
        }
    }

    /** With neither a declared template nor one in the result, there is no form to open. */
    @Test
    void anActionWithNoTemplateAnywhereIsNotShown() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            QueryResult result = resultActions(utils, grlc, td, null, null,
                    rows(SOURCE_NP, SOURCE_TEMPLATE));

            assertNull(paramsOf(result, "broken..."));
        }
    }

}
