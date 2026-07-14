package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Publish-path tests for member-level optionality inside grouped statements
 * (docs/optional-statements-in-groups.md): an empty optional member is dropped
 * from the created nanopub while the rest of the group is emitted; empty
 * required members still fail.
 */
public class OptionalGroupMemberPublishTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI GROUP = vf.createIRI(NP_URI + "/group");
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI ST2 = vf.createIRI(NP_URI + "/st2");
    private static final IRI THING = vf.createIRI(NP_URI + "/thing");
    private static final IRI NAME = vf.createIRI(NP_URI + "/name");
    private static final IRI SOME_CLASS = vf.createIRI("http://example.com/SomeClass");

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

    /**
     * Builds a template with one group: st1 (required: thing a SomeClass) and st2
     * (optional: thing rdfs:label name), and registers it with the mocked
     * TemplateData under its nanopub URI.
     */
    private TemplateContext mixedGroupContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Group test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, GROUP);
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST2);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(ST1, RDF.OBJECT, SOME_CLASS);
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST2, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST2, RDF.OBJECT, NAME);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("thing"));
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(NAME, RDFS.LABEL, vf.createLiteral("name"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
        context.initStatements();
        return context;
    }

    @SuppressWarnings("unchecked")
    private void setModel(TemplateContext context, IRI placeholder, String value) {
        ((org.apache.wicket.model.IModel<Object>) context.getComponentModels().get(placeholder)).setObject(value);
    }

    private Nanopub publish(TemplateContext context) throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/result/");
        context.propagateStatements(creator);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static boolean hasAssertionTriple(Nanopub np, Value subj, Value pred, Value obj) {
        for (Statement st : np.getAssertion()) {
            if (st.getSubject().equals(subj) && st.getPredicate().equals(pred) && st.getObject().equals(obj)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAssertionTripleWithPredicate(Nanopub np, Value pred) {
        for (Statement st : np.getAssertion()) {
            if (st.getPredicate().equals(pred)) return true;
        }
        return false;
    }

    @Test
    void emptyOptionalMemberIsDropped() throws Exception {
        TemplateContext context = mixedGroupContext();
        setModel(context, THING, "http://example.com/thing1");
        // NAME stays empty
        Nanopub np = publish(context);
        assertTrue(hasAssertionTriple(np, vf.createIRI("http://example.com/thing1"), RDF.TYPE, SOME_CLASS));
        assertFalse(hasAssertionTripleWithPredicate(np, RDFS.LABEL), "empty optional member must not be emitted");
    }

    @Test
    void filledOptionalMemberIsEmitted() throws Exception {
        TemplateContext context = mixedGroupContext();
        setModel(context, THING, "http://example.com/thing1");
        setModel(context, NAME, "some label");
        Nanopub np = publish(context);
        assertTrue(hasAssertionTriple(np, vf.createIRI("http://example.com/thing1"), RDF.TYPE, SOME_CLASS));
        assertTrue(hasAssertionTriple(np, vf.createIRI("http://example.com/thing1"), RDFS.LABEL, vf.createLiteral("some label")));
    }

    @Test
    void emptyRequiredMemberStillFails() throws Exception {
        TemplateContext context = mixedGroupContext();
        // THING stays empty, so the required member st1 cannot be resolved
        setModel(context, NAME, "some label");
        assertThrows(MalformedNanopubException.class, () -> publish(context));
    }

}
