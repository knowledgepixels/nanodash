package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.domain.UserData;
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
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Component-level tests for what a guided choice placeholder accepts: it suggests values but does
 * not limit them, so a plain name for a resource that has no identifier yet can be entered too
 * (issue #652).
 */
public class GuidedChoiceItemTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI THING = vf.createIRI(NP_URI + "/thing");

    private MockedStatic<TemplateData> templateDataMockedStatic;
    private MockedStatic<User> userMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        // Building the form looks users up; keep the test off the network.
        userMockedStatic = mockStatic(User.class, CALLS_REAL_METHODS);
        userMockedStatic.when(User::getUserData).thenReturn(mock(UserData.class));
    }

    @AfterEach
    void tearDown() {
        userMockedStatic.close();
        templateDataMockedStatic.close();
    }

    /**
     * Builds a one-statement template whose object is a guided choice placeholder with the given
     * prefix (none if null), and returns an initialized context for it.
     */
    private TemplateContext guidedContext(String prefix) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Guided choice component test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, vf.createIRI("http://example.com/hasThing"));
        creator.addAssertionStatement(st1, RDF.OBJECT, THING);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.GUIDED_CHOICE_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("thing"));
        if (prefix != null) {
            creator.addAssertionStatement(THING, NTEMPLATE.HAS_PREFIX, vf.createLiteral(prefix));
        }
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @SuppressWarnings("unchecked")
    private Select2Choice<String> fieldOf(TemplateContext context) {
        for (Component c : context.getComponents()) {
            if (c instanceof Select2Choice) return (Select2Choice<String>) c;
        }
        throw new AssertionError("the guided choice placeholder must render a Select2 choice");
    }

    private List<String> suggestionsFor(TemplateContext context, String term) {
        Response<String> response = new Response<>();
        fieldOf(context).getProvider().query(term, 0, response);
        return response.getResults();
    }

    @Test
    void plainNameIsOffered() throws Exception {
        assertTrue(suggestionsFor(guidedContext(null), "john").contains("john"));
    }

    @Test
    void uriIsOffered() throws Exception {
        assertTrue(suggestionsFor(guidedContext(null), "https://example.com/thing").contains("https://example.com/thing"));
    }

    @Test
    void termWithWhitespaceIsNotOffered() throws Exception {
        // A name with a space cannot be turned into a well-formed IRI, so the validator would
        // reject it; it must not be offered as a choice in the first place.
        assertTrue(suggestionsFor(guidedContext(null), "john doe").isEmpty());
    }

    /**
     * Without a prefix, the nanopublication mints the name under its own namespace, and the field
     * says so.
     */
    @Test
    void plainNameIsShownAsMintedLocally() throws Exception {
        assertEquals("local:john (mint locally)", fieldOf(guidedContext(null)).getProvider().getDisplayValue("john"));
    }

    /**
     * With a prefix, the name is minted under that prefix instead, which the field shows next to
     * the value -- so it is not a local identifier and must not be marked as one.
     */
    @Test
    void plainNameUnderAPrefixIsNotMintedLocally() throws Exception {
        TemplateContext context = guidedContext("https://example.org/");
        assertTrue(suggestionsFor(context, "john").contains("john"));
        assertEquals("john", fieldOf(context).getProvider().getDisplayValue("john"));
    }

    /**
     * A restricted choice is restricted: it must keep offering only what the template allows.
     */
    @Test
    void restrictedChoiceOffersNoPlainName() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Restricted choice component test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, vf.createIRI("http://example.com/hasThing"));
        creator.addAssertionStatement(st1, RDF.OBJECT, THING);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.RESTRICTED_CHOICE_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("thing"));
        creator.addAssertionStatement(THING, NTEMPLATE.POSSIBLE_VALUE, vf.createIRI("https://example.com/allowed-thing"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();

        List<String> suggestions = suggestionsFor(context, "john");
        assertFalse(suggestions.contains("john"), "a restricted choice must not offer a made-up name");
        assertTrue(suggestions.isEmpty());
        assertEquals("john", fieldOf(context).getProvider().getDisplayValue("john"));
    }

}
