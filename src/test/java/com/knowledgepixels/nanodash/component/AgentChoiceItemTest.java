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
 * Component-level tests for what an agent placeholder accepts: known users are suggested, but
 * any URI in an allowed scheme and any locally minted name can also be entered by hand
 * (issue #652).
 */
public class AgentChoiceItemTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI AGENT = vf.createIRI(NP_URI + "/agent");

    private WicketTester tester;
    private MockedStatic<TemplateData> templateDataMockedStatic;
    private MockedStatic<User> userMockedStatic;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        // The known users are looked up on every keystroke; keep the test off the network.
        userMockedStatic = mockStatic(User.class, CALLS_REAL_METHODS);
        userMockedStatic.when(User::getUserData).thenReturn(mock(UserData.class));
    }

    @AfterEach
    void tearDown() {
        userMockedStatic.close();
        templateDataMockedStatic.close();
    }

    /**
     * Builds a one-statement template whose object is an agent placeholder, and returns an
     * initialized context for it.
     */
    private TemplateContext agentContext() throws Exception {
        return agentContext(false);
    }

    /**
     * Builds a one-statement template whose object is an agent placeholder, optionally also typed
     * as an external URI placeholder, and returns an initialized context for it.
     */
    private TemplateContext agentContext(boolean external) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Agent component test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, vf.createIRI("http://example.com/hasAgent"));
        creator.addAssertionStatement(st1, RDF.OBJECT, AGENT);
        creator.addAssertionStatement(AGENT, RDF.TYPE, NTEMPLATE.AGENT_PLACEHOLDER);
        if (external) {
            creator.addAssertionStatement(AGENT, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        }
        creator.addAssertionStatement(AGENT, RDFS.LABEL, vf.createLiteral("agent"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @SuppressWarnings("unchecked")
    private List<String> suggestionsFor(TemplateContext context, String term) {
        Select2Choice<String> field = null;
        for (Component c : context.getComponents()) {
            if (c instanceof Select2Choice) field = (Select2Choice<String>) c;
        }
        assertTrue(field != null, "the agent placeholder must render a Select2 choice");
        Response<String> response = new Response<>();
        field.getProvider().query(term, 0, response);
        return response.getResults();
    }

    @SuppressWarnings("unchecked")
    private String displayValueFor(TemplateContext context, String choiceId) {
        for (Component c : context.getComponents()) {
            if (c instanceof Select2Choice) return ((Select2Choice<String>) c).getProvider().getDisplayValue(choiceId);
        }
        throw new AssertionError("the agent placeholder must render a Select2 choice");
    }

    /**
     * A name that has no identifier yet is shown as the local URI it will be minted into, so that
     * it doesn't read like an existing agent.
     */
    @Test
    void toBeMintedNameIsShownWithTheLocalPrefix() throws Exception {
        assertEquals("local:john-doe (mint locally)", displayValueFor(agentContext(), "john-doe"));
    }

    /**
     * A URI of an agent that is not a known user has no name to show, so it must be rendered once
     * rather than as "value (value)".
     */
    @Test
    void uriWithoutAKnownNameIsShownOnce() throws Exception {
        TemplateContext context = agentContext();
        assertEquals("https://example.com/agents/jd", displayValueFor(context, "https://example.com/agents/jd"));
        assertEquals("did:plc:z72i7hdynmk6r22z27h6tvur", displayValueFor(context, "did:plc:z72i7hdynmk6r22z27h6tvur"));
    }

    @Test
    void httpUriIsOffered() throws Exception {
        assertTrue(suggestionsFor(agentContext(), "https://example.com/agents/jd").contains("https://example.com/agents/jd"));
    }

    @Test
    void orcidIsOfferedAsUri() throws Exception {
        List<String> suggestions = suggestionsFor(agentContext(), "0000-0002-1267-0234");
        assertTrue(suggestions.contains("https://orcid.org/0000-0002-1267-0234"));
        // The bare ORCID itself would be a valid local name, but offering it next to the ORCID
        // URI would only be a confusing near-duplicate.
        assertFalse(suggestions.contains("0000-0002-1267-0234"));
    }

    @Test
    void uriInAnyAllowedSchemeIsOffered() throws Exception {
        assertTrue(suggestionsFor(agentContext(), "did:plc:z72i7hdynmk6r22z27h6tvur").contains("did:plc:z72i7hdynmk6r22z27h6tvur"));
    }

    @Test
    void plainNameIsOfferedAsLocallyMintedIdentifier() throws Exception {
        assertTrue(suggestionsFor(agentContext(), "john-doe").contains("john-doe"));
    }

    /**
     * An agent placeholder that is also an external URI placeholder refers to an agent that exists
     * outside this nanopublication, so no name to be minted is offered for it (issue #676).
     */
    @Test
    void externalAgentOffersNoPlainName() throws Exception {
        assertFalse(suggestionsFor(agentContext(true), "john-doe").contains("john-doe"));
    }

    /**
     * Only the minting is gone for an external agent placeholder: URIs and ORCIDs stay on offer.
     */
    @Test
    void externalAgentOffersUriAndOrcid() throws Exception {
        assertTrue(suggestionsFor(agentContext(true), "did:plc:z72i7hdynmk6r22z27h6tvur").contains("did:plc:z72i7hdynmk6r22z27h6tvur"));
        assertTrue(suggestionsFor(agentContext(true), "0000-0002-1267-0234").contains("https://orcid.org/0000-0002-1267-0234"));
    }

    /**
     * Nothing is minted for an external agent placeholder, so a bare word held for it is not
     * marked as a local identifier either (issue #676).
     */
    @Test
    void externalAgentNameIsNotShownAsMintedLocally() throws Exception {
        assertEquals("john-doe", displayValueFor(agentContext(true), "john-doe"));
    }

    @Test
    void termWithWhitespaceIsNotOffered() throws Exception {
        // A name with a space cannot be minted into a well-formed IRI, so the validator would
        // reject it; it must not be offered as a choice in the first place.
        assertTrue(suggestionsFor(agentContext(), "john doe").isEmpty());
    }

    /**
     * The counterpart of the suggestion: a locally minted name ends up as an IRI under the
     * namespace of the nanopublication being published.
     */
    @Test
    @SuppressWarnings("unchecked")
    void plainNameIsMintedUnderTheTargetNamespace() throws Exception {
        TemplateContext context = agentContext();
        ((org.apache.wicket.model.IModel<String>) context.getComponentModels().get(AGENT)).setObject("john-doe");
        assertEquals(vf.createIRI(Template.DEFAULT_TARGET_NAMESPACE + "john-doe"), context.processIri(AGENT));
    }

    @Test
    @SuppressWarnings("unchecked")
    void enteredUriIsKeptAsIs() throws Exception {
        TemplateContext context = agentContext();
        ((org.apache.wicket.model.IModel<String>) context.getComponentModels().get(AGENT)).setObject("did:plc:z72i7hdynmk6r22z27h6tvur");
        assertEquals(vf.createIRI("did:plc:z72i7hdynmk6r22z27h6tvur"), context.processIri(AGENT));
    }

}
