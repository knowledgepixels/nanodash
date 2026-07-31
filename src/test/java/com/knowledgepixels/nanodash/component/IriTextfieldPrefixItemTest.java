package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import org.apache.wicket.Component;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    private static final IRI OTHER = vf.createIRI(NP_URI + "/other");
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
        creator.addAssertionStatement(RESOURCE, NTEMPLATE.HAS_PREFIX_LABEL, vf.createLiteral("Class"));
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
        assertTrue(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(DynamicPrefix.SPACE_TOKEN)));
        assertTrue(context.hasUnresolvedPrefix(RESOURCE));
    }

    @Test
    void noPrefixChoiceWhenTheContextDeterminesTheBase() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.setNavigationContextId(SPACE_IRI);
        context.initStatements();
        assertEquals(1, context.getComponents().size(), "a context-resolved prefix needs no picker");
        assertFalse(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(DynamicPrefix.SPACE_TOKEN)));
        assertEquals(SPACE_IRI + "/r/", context.getPrefix(RESOURCE));
    }

    @Test
    void noPrefixChoiceForAStaticPrefix() throws Exception {
        TemplateContext context = contextFor("https://example.org/");
        context.initStatements();
        assertEquals(1, context.getComponents().size());
        assertFalse(context.getComponentModels().containsKey(TemplateContext.getPrefixModelKey(DynamicPrefix.SPACE_TOKEN)));
    }

    @Test
    void prefixParamPrefillsTheChoice() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.setParam("resource__prefix", "https://w3id.org/space/other");
        context.initStatements();
        assertEquals("https://w3id.org/space/other/r/", context.getPrefix(RESOURCE));
        assertFalse(context.hasUnresolvedPrefix(RESOURCE));
    }

    /**
     * Builds a template with two URI placeholders carrying the given prefixes, and returns
     * an uninitialized context for it.
     */
    private TemplateContext contextForTwo(String prefixA, String prefixB) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Two dynamic prefixes"));
        IRI subject = vf.createIRI("http://example.com/subject");
        int i = 0;
        for (IRI placeholder : new IRI[]{RESOURCE, OTHER}) {
            IRI st = vf.createIRI(NP_URI + "/st" + (++i));
            creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st);
            creator.addAssertionStatement(st, RDF.SUBJECT, subject);
            creator.addAssertionStatement(st, RDF.PREDICATE, vf.createIRI("http://example.com/p" + i));
            creator.addAssertionStatement(st, RDF.OBJECT, placeholder);
            creator.addAssertionStatement(placeholder, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
            creator.addAssertionStatement(placeholder, NTEMPLATE.HAS_PREFIX, vf.createLiteral(i == 1 ? prefixA : prefixB));
            creator.addAssertionStatement(placeholder, RDFS.LABEL, vf.createLiteral("placeholder " + i));
        }
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    @Test
    void pickersOnTheSameTokenShareOneModel() throws Exception {
        // Different suffixes, same token: one shared base, each field keeping its own suffix.
        TemplateContext context = contextForTwo("~~SPACE~~/r/", "~~SPACE~~/");
        context.initStatements();
        List<Select2Choice<?>> pickers = pickersIn(context);
        assertEquals(2, pickers.size(), "both fields must get a picker");
        assertSame(pickers.get(0).getDefaultModel(), pickers.get(1).getDefaultModel(),
                "pickers on the same token must share one model, so the shared-model AJAX refresh reaches both");

        // Choosing in one picker resolves the prefix of both fields.
        pickers.get(0).setDefaultModelObject("https://w3id.org/space/other");
        assertEquals("https://w3id.org/space/other/r/", context.getPrefix(RESOURCE));
        assertEquals("https://w3id.org/space/other/", context.getPrefix(OTHER));
        assertFalse(context.hasUnresolvedPrefix(RESOURCE));
        assertFalse(context.hasUnresolvedPrefix(OTHER));
    }

    @Test
    void pickersOnDifferentTokensKeepSeparateModels() throws Exception {
        // A space and a maintained-resource namespace are different things picked from
        // different lists, so they must not share a value.
        TemplateContext context = contextForTwo("~~SPACE~~/r/", "~~NAMESPACE~~");
        context.initStatements();
        List<Select2Choice<?>> pickers = pickersIn(context);
        assertEquals(2, pickers.size());
        assertNotSame(pickers.get(0).getDefaultModel(), pickers.get(1).getDefaultModel());

        pickers.get(0).setDefaultModelObject("https://w3id.org/space/other");
        assertEquals("https://w3id.org/space/other/r/", context.getPrefix(RESOURCE));
        assertNull(context.getPrefix(OTHER), "the namespace picker must be unaffected");
        assertTrue(context.hasUnresolvedPrefix(OTHER));
    }

    private static List<Select2Choice<?>> pickersIn(TemplateContext context) {
        List<Select2Choice<?>> pickers = new ArrayList<>();
        for (Component c : context.getComponents()) {
            if (c instanceof Select2Choice<?> choice) pickers.add(choice);
        }
        return pickers;
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

    /**
     * The picker already names what is being chosen, so the template's static prefix label
     * would only duplicate it; it is shown only when no picker is.
     */
    @Test
    void prefixLabelIsHiddenWhenThePickerIsShown() throws Exception {
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.initStatements();
        tester.startComponentInPage(new IriTextfieldItem("value", "obj", RESOURCE, true, context));
        tester.assertComponent("value:prefixchoice", Select2Choice.class);
        tester.assertInvisible("value:prefix");
    }

    @Test
    void prefixLabelIsShownWhenTheContextResolvesTheBase() throws Exception {
        withSpaceContext(SPACE_IRI);
        TemplateContext context = contextFor("~~SPACE~~/r/");
        context.setNavigationContextId(SPACE_IRI);
        context.initStatements();
        tester.startComponentInPage(new IriTextfieldItem("value", "obj", RESOURCE, true, context));
        tester.assertInvisible("value:prefixchoice");
        tester.assertVisible("value:prefix");
        tester.assertLabel("value:prefix", "Class");
    }

    @Test
    void prefixLabelIsShownForAStaticPrefix() throws Exception {
        TemplateContext context = contextFor("https://example.org/");
        context.initStatements();
        tester.startComponentInPage(new IriTextfieldItem("value", "obj", RESOURCE, true, context));
        tester.assertVisible("value:prefix");
        tester.assertLabel("value:prefix", "Class");
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
