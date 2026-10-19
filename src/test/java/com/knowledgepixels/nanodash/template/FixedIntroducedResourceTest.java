package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for issue #749: a resource marked as introduced or embedded under a fixed IRI outside
 * the template's namespace would be declared by every nanopublication made from the template,
 * so {@link Template#getStatementErrors()} reports it.
 */
class FixedIntroducedResourceTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI ST2 = vf.createIRI(NP_URI + "/st2");
    private static final IRI FIXED = vf.createIRI("https://example.org/thing");
    private static final IRI MARKED = vf.createIRI("https://example.org/thing/~~ARTIFACTCODE~~");
    private static final IRI LOCAL = vf.createIRI(NP_URI + "/thing");
    private static final IRI OTHER = vf.createIRI("https://example.org/other");

    /**
     * Builds a template whose statements all use the given subject, marked with the given type.
     *
     * @param subject    the subject of each statement
     * @param marking    the type the subject is marked with
     * @param statements the number of statements
     * @return the creator, ready to add more to or finalize
     * @throws Exception if the nanopublication cannot be created
     */
    private static NanopubCreator creator(IRI subject, IRI marking, int statements) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Fixed introduced resource test template"));
        List<IRI> statementIris = List.of(ST1, ST2).subList(0, statements);
        for (IRI st : statementIris) {
            creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st);
            creator.addAssertionStatement(st, RDF.SUBJECT, subject);
            creator.addAssertionStatement(st, RDF.PREDICATE, RDFS.LABEL);
            creator.addAssertionStatement(st, RDF.OBJECT, vf.createLiteral("something"));
        }
        creator.addAssertionStatement(subject, RDF.TYPE, marking);
        return creator;
    }

    private static List<String> errors(NanopubCreator creator) throws Exception {
        return new Template(creator.finalizeNanopub()).getStatementErrors();
    }

    @Test
    void aFixedIntroducedIriIsReported() throws Exception {
        List<String> errors = errors(creator(FIXED, NTEMPLATE.INTRODUCED_RESOURCE, 1));
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("Statement 1 (st1): the subject thing is marked as introduced"), errors.get(0));
        assertTrue(errors.get(0).contains("every nanopublication would introduce the same resource"), errors.get(0));
    }

    @Test
    void aFixedEmbeddedIriIsReported() throws Exception {
        List<String> errors = errors(creator(FIXED, NTEMPLATE.EMBEDDED_RESOURCE, 1));
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("is marked as embedded"), errors.get(0));
        assertTrue(errors.get(0).contains("every nanopublication would embed the same resource"), errors.get(0));
    }

    @Test
    void aFixedIriIsReportedOnceHoweverOftenItIsUsed() throws Exception {
        List<String> errors = errors(creator(FIXED, NTEMPLATE.INTRODUCED_RESOURCE, 2));
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("Statement 1 (st1)"), errors.get(0));
    }

    @Test
    void aFixedIriInObjectPositionIsReported() throws Exception {
        NanopubCreator creator = creator(OTHER, NTEMPLATE.URI_PLACEHOLDER, 0);
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, OTHER);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(ST1, RDF.OBJECT, FIXED);
        creator.addAssertionStatement(FIXED, RDF.TYPE, NTEMPLATE.EMBEDDED_RESOURCE);
        List<String> errors = errors(creator);
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("the object thing is marked as embedded"), errors.get(0));
    }

    @Test
    void anIriWithTheArtifactCodeMarkerIsAccepted() throws Exception {
        assertEquals(List.of(), errors(creator(MARKED, NTEMPLATE.INTRODUCED_RESOURCE, 1)));
    }

    @Test
    void anIriUnderTheTemplateIsAccepted() throws Exception {
        assertEquals(List.of(), errors(creator(LOCAL, NTEMPLATE.INTRODUCED_RESOURCE, 1)));
    }

    @Test
    void aPlaceholderIsAccepted() throws Exception {
        NanopubCreator creator = creator(FIXED, NTEMPLATE.INTRODUCED_RESOURCE, 1);
        creator.addAssertionStatement(FIXED, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        assertEquals(List.of(), errors(creator));
    }

    @Test
    void aFixedIriThatIsNotIntroducedIsAccepted() throws Exception {
        NanopubCreator creator = creator(LOCAL, NTEMPLATE.INTRODUCED_RESOURCE, 0);
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, FIXED);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, vf.createLiteral("something"));
        assertEquals(List.of(), errors(creator));
    }

}
