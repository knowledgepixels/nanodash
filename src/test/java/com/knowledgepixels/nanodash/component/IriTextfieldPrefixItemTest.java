package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
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
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;
import org.wicketstuff.select2.Select2Choice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Component-level tests for the picker that supplies the base of a
 * space-/namespace-dependent prefix when the navigation context determines none
 * (issue #571, docs/space-namespace-prefixes.md).
 */
public class IriTextfieldPrefixItemTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI RESOURCE = vf.createIRI(NP_URI + "/resource");
    private static final String SPACE_IRI = "https://w3id.org/space/foo/bar";

    private WicketTester tester;
    private MockedStatic<TemplateData> templateDataMockedStatic;
    private MockedStatic<NavigationContext> navigationContextMockedStatic;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        navigationContextMockedStatic = mockStatic(NavigationContext.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
        navigationContextMockedStatic.close();
    }

    /**
     * Builds a one-statement template whose object is a URI placeholder with the given
     * prefix, and returns an uninitialized context for it; call setParam before
     * initStatements.
     */
    private TemplateContext contextFor(String prefix) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Prefix component test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, vf.createIRI("http://example.com/hasPart"));
        creator.addAssertionStatement(st1, RDF.OBJECT, RESOURCE);
        creator.addAssertionStatement(RESOURCE, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(RESOURCE, NTEMPLATE.HAS_PREFIX, vf.createLiteral(prefix));
        creator.addAssertionStatement(RESOURCE, RDFS.LABEL, vf.createLiteral("resource"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    private void withSpaceContext(String contextId) {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(SPACE_IRI);
        when(space.getSpace()).thenReturn(space);
        navigationContextMockedStatic.when(() -> NavigationContext.resolve(contextId)).thenReturn(space);
    }

    @Test
    void prefixChoiceIsAddedWhenTheContextDeterminesNoBase() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.initStatements();
        assertEquals(2, context.getComponents().size(), "text field and prefix choice must both be registered");
        assertTrue(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(RESOURCE)));
        assertTrue(context.hasUnresolvedPrefix(RESOURCE));
    }

    @Test
    void noPrefixChoiceWhenTheContextDeterminesTheBase() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.setNavigationContextId(SPACE_IRI);
        context.initStatements();
        assertEquals(1, context.getComponents().size(), "a context-resolved prefix needs no picker");
        assertFalse(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(RESOURCE)));
        assertEquals(SPACE_IRI + "/r/", context.getPrefix(RESOURCE));
    }

    @Test
    void noPrefixChoiceForAStaticPrefix() throws Exception {
        TemplateContext context = contextFor("https://example.org/");
        context.initStatements();
        assertEquals(1, context.getComponents().size());
        assertFalse(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(RESOURCE)));
    }

    @Test
    void prefixParamPrefillsTheChoice() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.setParam("resource__prefix", "https://w3id.org/space/other");
        context.initStatements();
        assertEquals("https://w3id.org/space/other/r/", context.getPrefix(RESOURCE));
        assertFalse(context.hasUnresolvedPrefix(RESOURCE));
    }

    @Test
    void itemRendersWithThePicker() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.initStatements();
        tester.startComponentInPage(new IriTextfieldItem("value", "obj", RESOURCE, true, context));
        tester.assertComponent("value:prefixchoice", Select2Choice.class);
    }

    @Test
    void itemRendersWithoutThePicker() throws Exception {
        TemplateContext context = contextFor("https://example.org/");
        context.initStatements();
        tester.startComponentInPage(new IriTextfieldItem("value", "obj", RESOURCE, true, context));
        tester.assertInvisible("value:prefixchoice");
    }

    @Test
    void removeFromContextDeregistersBothComponents() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.initStatements();
        assertEquals(2, context.getComponents().size());
        IriTextfieldItem extra = new IriTextfieldItem("value", "obj", RESOURCE, true, context);
        assertEquals(4, context.getComponents().size());
        extra.removeFromContext();
        assertEquals(2, context.getComponents().size());
    }

}
