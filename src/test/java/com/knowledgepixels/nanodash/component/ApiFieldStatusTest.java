package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LookupApis;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.domain.UserData;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import com.knowledgepixels.nanodash.template.ValueFiller;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for issue #763 in a form that defines a template: the field in which the API of a
 * placeholder is entered, as the object of {@code nt:possibleValuesFromApi}, says whether the
 * API entered can be reached.
 */
class ApiFieldStatusTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI PLACEHOLDER = vf.createIRI(NP_URI + "/placeholder");
    private static final IRI API = vf.createIRI(NP_URI + "/api");
    private static final String UNREACHABLE = "http://localhost:1/lookup?q=";

    private MockedStatic<TemplateData> templateDataMockedStatic;
    private MockedStatic<User> userMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        userMockedStatic = mockStatic(User.class, CALLS_REAL_METHODS);
        userMockedStatic.when(User::getUserData).thenReturn(mock(UserData.class));
        LookupApis.forgetReachability(List.of(UNREACHABLE));
    }

    @AfterEach
    void tearDown() {
        LookupApis.forgetReachability(List.of(UNREACHABLE));
        userMockedStatic.close();
        templateDataMockedStatic.close();
    }

    /**
     * Builds a template with one statement "placeholder ‹predicate› api", where api is a literal
     * placeholder, and returns an initialized context for it.
     */
    private TemplateContext context(IRI predicate) throws Exception {
        mockTemplate(predicate);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    /**
     * Builds the template of {@link #context(IRI)} and has {@link TemplateData} return it.
     */
    private void mockTemplate(IRI predicate) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("API field test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, PLACEHOLDER);
        creator.addAssertionStatement(st1, RDF.PREDICATE, predicate);
        creator.addAssertionStatement(st1, RDF.OBJECT, API);
        creator.addAssertionStatement(PLACEHOLDER, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(PLACEHOLDER, RDFS.LABEL, vf.createLiteral("placeholder"));
        creator.addAssertionStatement(API, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(API, RDFS.LABEL, vf.createLiteral("URL prefix for API lookup"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
    }

    /**
     * Builds a nanopub made with the template of {@link #context(IRI)} that gives the API, and
     * returns a context that shows it read-only, as when it is explored.
     */
    private TemplateContext readOnlyContext(String api) throws Exception {
        mockTemplate(NTEMPLATE.POSSIBLE_VALUES_FROM_API);
        String npUri = "https://w3id.org/np/RAzYxWvUtSrQpOnMlKjIhGfEdCbA9876543210_-ZyXwV";
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addAssertionStatement(vf.createIRI("https://example.org/field"), NTEMPLATE.POSSIBLE_VALUES_FROM_API, vf.createLiteral(api));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        Nanopub np = creator.finalizeNanopub();

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", np);
        context.initStatements();
        new ValueFiller(np, ContextType.ASSERTION, false).fill(context);
        return context;
    }

    private static List<LookupApiStatus> statusLines(TemplateContext context) {
        return context.getComponents().stream()
                .filter(LookupApiStatus.class::isInstance).map(LookupApiStatus.class::cast).toList();
    }

    /**
     * Has a status line make itself ready to be shown, as it does before each rendering.
     */
    private static void reconfigure(LookupApiStatus line) {
        line.detach();
        line.configure();
    }

    @Test
    void theFieldOfAnApiHasAStatusLineSharingItsModel() throws Exception {
        TemplateContext context = context(NTEMPLATE.POSSIBLE_VALUES_FROM_API);
        List<LookupApiStatus> lines = statusLines(context);
        assertEquals(1, lines.size());
        assertSame(context.getComponentModels().get(API), lines.get(0).getDefaultModel());
    }

    @Test
    void anOrdinaryLiteralFieldHasNoStatusLine() throws Exception {
        assertTrue(statusLines(context(RDFS.SEEALSO)).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aValueTheFormFillsInIsChecked() throws Exception {
        TemplateContext context = context(NTEMPLATE.POSSIBLE_VALUES_FROM_API);
        LookupApiStatus line = statusLines(context).get(0);
        ((IModel<String>) context.getComponentModels().get(API)).setObject(UNREACHABLE);
        reconfigure(line);
        assertTrue(line.isVisible());
        assertInstanceOf(AjaxLazyLoadPanel.class, line.get(LookupApiStatus.CHECK_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theStatusLineChecksTheApiEntered() throws Exception {
        TemplateContext context = context(NTEMPLATE.POSSIBLE_VALUES_FROM_API);
        LookupApiStatus line = statusLines(context).get(0);
        assertEquals(List.of(), line.getApis());

        ((IModel<String>) context.getComponentModels().get(API)).setObject("  " + UNREACHABLE + " ");
        reconfigure(line);
        assertEquals(List.of(UNREACHABLE), line.getApis());
        assertInstanceOf(AjaxLazyLoadPanel.class, line.get(LookupApiStatus.CHECK_ID), "a new URL is checked after the form is shown");

        LookupApis.checkReachability(List.of(UNREACHABLE));
        ((IModel<String>) context.getComponentModels().get(API)).setObject("");
        reconfigure(line);
        assertFalse(line.isVisible(), "an emptied field has no status");
        ((IModel<String>) context.getComponentModels().get(API)).setObject(UNREACHABLE);
        reconfigure(line);
        Component outcome = line.get(LookupApiStatus.CHECK_ID);
        assertEquals("Outcome", outcome.getClass().getSimpleName(), "a URL checked lately is shown at once");
    }

    /**
     * A nanopub shown read-only, as when a template is explored, holds a literal in quotation
     * marks; the API checked is the URL inside them, not the quoted text, which cannot be reached.
     */
    @Test
    void aReadOnlyFieldChecksTheUrlWithoutItsQuotationMarks() throws Exception {
        // The predicate is shown with the label of the space or resource it names, if any; those
        // are loaded over the network, which the test does without.
        try (MockedStatic<SpaceRepository> spaces = mockStatic(SpaceRepository.class);
             MockedStatic<MaintainedResourceRepository> resources = mockStatic(MaintainedResourceRepository.class)) {
            spaces.when(SpaceRepository::get).thenReturn(mock(SpaceRepository.class));
            resources.when(MaintainedResourceRepository::get).thenReturn(mock(MaintainedResourceRepository.class));
            TemplateContext context = readOnlyContext(UNREACHABLE);
            assertTrue(context.isReadOnly());
            assertEquals("\"" + UNREACHABLE + "\"", context.getComponentModels().get(API).getObject());
            List<LookupApiStatus> lines = statusLines(context);
            assertEquals(1, lines.size());
            LookupApiStatus line = lines.get(0);
            reconfigure(line);
            assertEquals(List.of(UNREACHABLE), line.getApis());
        }
    }

}
