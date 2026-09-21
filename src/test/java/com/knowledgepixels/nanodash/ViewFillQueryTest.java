package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Parsing of an action's fill query (issue #690): the query run against the target
 * resource when the form opens, its target placeholder, and its mappings.
 */
public class ViewFillQueryTest {

    static final String NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFQ1";
    static final String VIEW = NP + "/view";
    static final IRI ADD = SimpleValueFactory.getInstance().createIRI(NP + "/addAction");
    static final IRI PLAIN = SimpleValueFactory.getInstance().createIRI(NP + "/plainAction");
    static final String VIEW_QUERY = "https://w3id.org/np/RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/list-presentations";
    static final String FILL_QUERY = "https://w3id.org/np/RAfffffffffffffffffffffffffffffffffffffffffff/get-event-defaults";
    public static final String FILL_QUERY_ID = "RAfffffffffffffffffffffffffffffffffffffffffff/get-event-defaults";

    /**
     * Loads the fixture view with its two queries and the action template stubbed out, so
     * that nothing is fetched. Callers close the returned mocks.
     */
    public static View loadFixture(MockedStatic<Utils> utils, MockedStatic<GrlcQuery> grlc, MockedStatic<TemplateData> td) throws Exception {
        Nanopub np = new NanopubImpl(new File("src/test/resources/np-fill-query-view.trig"), RDFFormat.TRIG);
        utils.when(() -> Utils.getAsNanopub(NP)).thenReturn(np);
        GrlcQuery viewQuery = mock(GrlcQuery.class);
        when(viewQuery.getQueryId()).thenReturn("RAqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq/list-presentations");
        GrlcQuery fillQuery = mock(GrlcQuery.class);
        when(fillQuery.getQueryId()).thenReturn(FILL_QUERY_ID);
        grlc.when(() -> GrlcQuery.get(VIEW_QUERY)).thenReturn(viewQuery);
        grlc.when(() -> GrlcQuery.get(FILL_QUERY)).thenReturn(fillQuery);
        Template template = mock(Template.class);
        when(template.getId()).thenReturn("https://w3id.org/np/RAttttttttttttttttttttttttttttttttttttttttttt");
        TemplateData templateData = mock(TemplateData.class);
        when(templateData.getTemplate(anyString())).thenReturn(template);
        td.when(TemplateData::get).thenReturn(templateData);
        return View.get(VIEW, false);
    }

    @Test
    void fillQueryIsParsedPerAction() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = loadFixture(utils, grlc, td);

            assertNotNull(view.getFillQueryForAction(ADD));
            assertEquals(FILL_QUERY_ID, view.getFillQueryForAction(ADD).getQueryId());
            assertEquals("event", view.getFillQueryTargetFieldForAction(ADD));
            assertEquals(List.of("startDate:startDate", "location:!location"), view.getFillQueryMappings(ADD));

            assertNull(view.getFillQueryForAction(PLAIN));
            assertNull(view.getFillQueryTargetFieldForAction(PLAIN));
            assertEquals(List.of(), view.getFillQueryMappings(PLAIN));
        }
    }

    /**
     * The fill query's columns belong to a different query than the view's, so they must
     * not be hidden from the view's own result table the way mapping-source columns are.
     */
    @Test
    void fillQueryColumnsAreNotMappingSourceColumns() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<GrlcQuery> grlc = mockStatic(GrlcQuery.class);
             MockedStatic<TemplateData> td = mockStatic(TemplateData.class)) {
            View view = loadFixture(utils, grlc, td);
            assertEquals(Set.of("series", "organiser"), view.getActionMappingSourceColumns());
        }
    }

    @Test
    void actionMappingParsesTheThreeTargetForms() {
        View.ActionMapping plain = View.ActionMapping.parse("col:field");
        assertEquals("col", plain.column());
        assertEquals("field", plain.key());
        assertFalse(plain.rawKey());
        assertFalse(plain.locked());

        View.ActionMapping locked = View.ActionMapping.parse("local_pubkey:!public-key__.1");
        assertEquals("local_pubkey", locked.column());
        assertEquals("public-key__.1", locked.key());
        assertFalse(locked.rawKey());
        assertTrue(locked.locked());

        View.ActionMapping raw = View.ActionMapping.parse("derive_target:@derive-a");
        assertEquals("derive_target", raw.column());
        assertEquals("derive-a", raw.key());
        assertTrue(raw.rawKey());
        assertFalse(raw.locked(), "a raw key is not a form field, so it cannot be locked");

        assertNull(View.ActionMapping.parse("nocolon"));
    }
}
