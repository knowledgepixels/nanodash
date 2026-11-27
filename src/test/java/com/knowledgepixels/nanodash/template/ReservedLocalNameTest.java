package com.knowledgepixels.nanodash.template;

import org.nanopub.templates.Template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.Model;
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
import org.nanopub.NanopubUtils;
import org.nanopub.templates.TemplateFillException;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for issue #29: a nanopublication keeps a few local names for itself — its four graphs
 * and its signature element — and a resource minted under one of them is that part rather than
 * a resource of its own. Nothing can be corrected after publishing, so such a value is caught
 * while the form is still open: the filler of nanopub-java refuses it when it makes the
 * nanopublication from the form (#936).
 */
class ReservedLocalNameTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String TARGET_NAMESPACE = "https://w3id.org/np/~~~ARTIFACTCODE~~~/";
    private static final String TOPIC_PREFIX = "https://w3id.org/topics/";

    // Minted under the nanopublication itself: what the user types becomes its local name.
    private static final IRI LOCAL_FIELD = vf.createIRI(NP_URI + "/local");
    // Minted below a prefix of its own, so the same name collides with nothing.
    private static final IRI TOPIC_FIELD = vf.createIRI(NP_URI + "/topic");

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
     * A template with one locally minted placeholder and one minted below a fixed prefix.
     */
    private TemplateContext context() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI st2 = vf.createIRI(NP_URI + "/st2");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Naming something"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, LOCAL_FIELD);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, vf.createLiteral("something"));
        creator.addAssertionStatement(st2, RDF.SUBJECT, TOPIC_FIELD);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st2, RDF.OBJECT, vf.createLiteral("a topic"));

        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(LOCAL_FIELD, RDFS.LABEL, vf.createLiteral("short id"));

        creator.addAssertionStatement(TOPIC_FIELD, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(TOPIC_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(TOPIC_PREFIX));
        creator.addAssertionStatement(TOPIC_FIELD, RDFS.LABEL, vf.createLiteral("the topic"));

        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", TARGET_NAMESPACE);
        context.initStatements();
        return context;
    }

    // The form with the given values in its two fields.
    private TemplateContext context(String local, String topic) throws Exception {
        TemplateContext context = context();
        context.getComponentModels().put(LOCAL_FIELD, Model.of(local));
        context.getComponentModels().put(TOPIC_FIELD, Model.of(topic));
        return context;
    }

    // Why the filler refuses to make the nanopublication from the form; empty if it does not.
    private static List<String> problems(TemplateContext context) {
        try {
            TestFill.fill(context);
            return List.of();
        } catch (TemplateFillException ex) {
            return ex.getProblems();
        }
    }

    private static boolean isRefusedAsAPart(TemplateContext context, String name) {
        return problems(context).stream().anyMatch(p -> p.contains("'" + name + "' is what the nanopublication calls one of its own parts"));
    }

    // Whether the nanopublication the form makes has the given IRI in its assertion, the
    // namespace of the nanopublication written as "this:".
    private static boolean mentions(TemplateContext context, String iri) throws Exception {
        Nanopub np = TestFill.fill(context);
        String ns = np.getUri().stringValue();
        return np.getAssertion().stream().anyMatch(st -> st.getSubject().stringValue().replace(ns, "this:").equals(iri));
    }

    @Test
    void aNameTheNanopublicationUsesForItselfIsRefused() throws Exception {
        // The case from the issue: "...#assertion" would be the assertion graph, not a resource.
        assertTrue(isRefusedAsAPart(context("assertion", "a-topic"), "assertion"), "publishing this has to be refused");
    }

    @Test
    void everyPartOfANanopublicationCounts() throws Exception {
        for (String reserved : NanopubUtils.RESERVED_LOCAL_NAMES) {
            assertTrue(isRefusedAsAPart(context(reserved, "a-topic"), reserved), reserved + " is a part of a nanopublication");
        }
    }

    @Test
    void anOrdinaryNameIsLeftAlone() throws Exception {
        TemplateContext context = context("my-record", "a-topic");
        assertEquals(List.of(), problems(context));
        assertTrue(mentions(context, "this:my-record"));
    }

    @Test
    void theSameNameBelowAnotherPrefixCollidesWithNothing() throws Exception {
        // Only the nanopublication's own namespace has parts to collide with.
        TemplateContext context = context("my-record", "assertion");
        assertEquals(List.of(), problems(context));
        assertTrue(mentions(context, TOPIC_PREFIX + "assertion"));
    }

    @Test
    void aTemplateThatMintsSuchANameIsReportedAsInvalid() throws Exception {
        // Nothing the person filling the form can do about this one, so the template is
        // reported rather than filled in.
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI signatureName = vf.createIRI(NP_URI + "/sig");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Naming something 'sig'"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, signatureName);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, vf.createLiteral("something"));
        creator.addAssertionStatement(signatureName, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);

        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());
        assertTrue(template.getStatementErrors().stream().anyMatch(error -> error.contains("'sig'")),
                "the template's errors should name it: " + template.getStatementErrors());
    }

    @Test
    void aNameThatOnlyLooksLikeAPartIsLeftAlone() throws Exception {
        assertEquals(List.of(), problems(context("assertions", "a-topic")));
        assertEquals(List.of(), problems(context("Assertion", "a-topic")),
                "the parts are named in the case a nanopublication writes them");
    }

}
