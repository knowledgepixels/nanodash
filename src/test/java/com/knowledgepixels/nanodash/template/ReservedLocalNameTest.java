package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for issue #29: a nanopublication keeps a few local names for itself — its four graphs
 * and its signature element — and a resource minted under one of them is that part rather than
 * a resource of its own. Nothing can be corrected after publishing, so such a value is caught
 * while the form is still open.
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

    @Test
    void aNameTheNanopublicationUsesForItselfIsRecorded() throws Exception {
        // The case from the issue: "...#assertion" would be the assertion graph, not a resource.
        TemplateContext context = context();
        context.getComponentModels().put(LOCAL_FIELD, Model.of("assertion"));
        Value processed = context.processValue(LOCAL_FIELD);
        assertEquals(TARGET_NAMESPACE + "assertion", processed.stringValue());
        assertTrue(context.getReservedIris().contains(processed), "publishing this has to be refused");
    }

    @Test
    void everyPartOfANanopublicationCounts() throws Exception {
        for (String reserved : TemplateContext.RESERVED_LOCAL_NAMES) {
            TemplateContext context = context();
            context.getComponentModels().put(LOCAL_FIELD, Model.of(reserved));
            Value processed = context.processValue(LOCAL_FIELD);
            assertTrue(context.getReservedIris().contains(processed), reserved + " is a part of a nanopublication");
        }
    }

    @Test
    void anOrdinaryNameIsLeftAlone() throws Exception {
        TemplateContext context = context();
        context.getComponentModels().put(LOCAL_FIELD, Model.of("my-record"));
        Value processed = context.processValue(LOCAL_FIELD);
        assertEquals(TARGET_NAMESPACE + "my-record", processed.stringValue());
        assertTrue(context.getReservedIris().isEmpty());
    }

    @Test
    void theSameNameBelowAnotherPrefixCollidesWithNothing() throws Exception {
        // Only the nanopublication's own namespace has parts to collide with.
        TemplateContext context = context();
        context.getComponentModels().put(TOPIC_FIELD, Model.of("assertion"));
        Value processed = context.processValue(TOPIC_FIELD);
        assertEquals(TOPIC_PREFIX + "assertion", processed.stringValue());
        assertTrue(context.getReservedIris().isEmpty());
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
        TemplateContext context = context();
        context.getComponentModels().put(LOCAL_FIELD, Model.of("assertions"));
        assertFalse(context.isReservedIri((IRI) context.processValue(LOCAL_FIELD)));
        context.getComponentModels().put(LOCAL_FIELD, Model.of("Assertion"));
        assertFalse(context.isReservedIri((IRI) context.processValue(LOCAL_FIELD)),
                "the parts are named in the case a nanopublication writes them");
    }

}
