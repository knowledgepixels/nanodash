package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The two query-driven pre-fills of the publish form: the target-driven fill query (issue
 * #690), which takes the first row only, and the listing-driven values, which take every row.
 */
class PublishFormQueryFillTest {

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";

    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    private TemplateContext context() {
        Template template = mock(Template.class);
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    /** A response with the given columns and one row per value array. */
    private static ApiResponse response(String[] columns, String[]... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(columns);
        for (String[] row : rows) {
            ApiResponseEntry entry = new ApiResponseEntry();
            for (int i = 0; i < columns.length; i++) entry.add(columns[i], row[i]);
            response.add(entry);
        }
        return response;
    }

    // ---- fill query: first row only ----

    @Test
    void fillQueryFillsFromTheFirstRow() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("startDate:startDate location:location",
                response(new String[]{"startDate", "location"}, new String[]{"2026-10-01", "Vienna"}), ctx);
        assertEquals("2026-10-01", ctx.getParam("startDate"));
        assertEquals("Vienna", ctx.getParam("location"));
        assertFalse(ctx.isLocked("startDate"));
    }

    @Test
    void fillQueryIgnoresRowsAfterTheFirst() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("startDate:startDate",
                response(new String[]{"startDate"}, new String[]{"2026-10-01"}, new String[]{"2026-10-02"}), ctx);
        assertEquals("2026-10-01", ctx.getParam("startDate"));
        assertNull(ctx.getParam("startDate__1"), "a default for a single-valued field must not spill into a repetition");
    }

    @Test
    void fillQueryWithNoRowsFillsNothing() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("startDate:startDate", response(new String[]{"startDate"}), ctx);
        assertNull(ctx.getParam("startDate"));
        PublishForm.applyFillQueryValues("startDate:startDate", null, ctx);
        assertNull(ctx.getParam("startDate"));
    }

    @Test
    void fillQuerySkipsEmptyValuesAndLeavesThemUnlocked() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("startDate:!startDate location:!location",
                response(new String[]{"startDate", "location"}, new String[]{"", "Vienna"}), ctx);
        assertNull(ctx.getParam("startDate"));
        assertFalse(ctx.isLocked("startDate"), "no value, nothing to lock the user out of");
        assertEquals("Vienna", ctx.getParam("location"));
        assertTrue(ctx.isLocked("location"));
    }

    @Test
    void fillQueryHonoursTheLockMarker() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("event:!event",
                response(new String[]{"event"}, new String[]{"https://example.org/e"}), ctx);
        assertEquals("https://example.org/e", ctx.getParam("event"));
        assertTrue(ctx.isLocked("event"));
    }

    @Test
    void fillQueryIgnoresRawKeyTargets() {
        TemplateContext ctx = context();
        PublishForm.applyFillQueryValues("np:@derive-a startDate:startDate",
                response(new String[]{"np", "startDate"}, new String[]{"https://example.org/np", "2026-10-01"}), ctx);
        assertNull(ctx.getParam("derive-a"));
        assertNull(ctx.getParam("@derive-a"));
        assertEquals("2026-10-01", ctx.getParam("startDate"), "the other mappings still apply");
    }

    // ---- listing values: every row ----

    @Test
    void listingValuesFillEveryRowUnderRepetitionSuffixes() {
        TemplateContext ctx = context();
        PublishForm.applyQueryValues("np:nanopub",
                response(new String[]{"np"}, new String[]{"a"}, new String[]{"b"}, new String[]{"c"}), ctx);
        assertEquals("a", ctx.getParam("nanopub"));
        assertEquals("b", ctx.getParam("nanopub__1"));
        assertEquals("c", ctx.getParam("nanopub__2"));
    }

    /**
     * Regression: a result action with two mappings in one literal used to be split on
     * the first colon, giving a target of {@code "series organiser"} and filling nothing.
     */
    @Test
    void listingValuesApplyEveryMappingInTheLiteral() {
        TemplateContext ctx = context();
        PublishForm.applyQueryValues("series:series organiser:organiser",
                response(new String[]{"series", "organiser"}, new String[]{"S1", "O1"}, new String[]{"S2", "O2"}), ctx);
        assertEquals("S1", ctx.getParam("series"));
        assertEquals("S2", ctx.getParam("series__1"));
        assertEquals("O1", ctx.getParam("organiser"));
        assertEquals("O2", ctx.getParam("organiser__1"));
        assertNull(ctx.getParam("series organiser"));
    }

    @Test
    void listingValuesKeepTheBareNameShorthand() {
        TemplateContext ctx = context();
        PublishForm.applyQueryValues("thing", response(new String[]{"thing"}, new String[]{"x"}), ctx);
        assertEquals("x", ctx.getParam("thing"));
    }

    @Test
    void listingValuesLockEveryRepetitionOfALockedField() {
        TemplateContext ctx = context();
        PublishForm.applyQueryValues("np:!nanopub",
                response(new String[]{"np"}, new String[]{"a"}, new String[]{"b"}), ctx);
        assertTrue(ctx.isLocked("nanopub"));
        assertTrue(ctx.isLocked("nanopub__1"));
    }

    @Test
    void listingValuesWithNoResponseFillNothing() {
        TemplateContext ctx = context();
        PublishForm.applyQueryValues("np:nanopub", null, ctx);
        assertNull(ctx.getParam("nanopub"));
    }
}
