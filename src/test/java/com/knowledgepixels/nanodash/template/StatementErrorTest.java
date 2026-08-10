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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Template#getStatementErrors()}: statements whose subject or predicate is a
 * literal, is a placeholder that can only be filled with a literal, or is missing altogether
 * cannot produce valid RDF and must be reported (see the "literals in subject/predicate
 * position" issue).
 */
class StatementErrorTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI THING = vf.createIRI(NP_URI + "/thing");
    private static final IRI NAME = vf.createIRI(NP_URI + "/name");

    /**
     * Builds a template with a single statement "thing rdfs:label name", where thing is a URI
     * placeholder and name a literal placeholder. Callers override statement parts on top.
     */
    private static NanopubCreator singleStatementCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Statement error test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(NAME, RDFS.LABEL, vf.createLiteral("the name"));
        return creator;
    }

    private static NanopubCreator wellFormedCreator() throws Exception {
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, NAME);
        return creator;
    }

    @Test
    void wellFormedStatementHasNoErrors() throws Exception {
        Template t = new Template(wellFormedCreator().finalizeNanopub());
        assertEquals(List.of(), t.getStatementErrors());
    }

    @Test
    void literalPlaceholderInSubjectPositionIsReported() throws Exception {
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(ST1, RDF.SUBJECT, NAME);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, THING);
        List<String> errors = new Template(creator.finalizeNanopub()).getStatementErrors();
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("\"the name\" (name)"), errors.get(0));
        assertTrue(errors.get(0).contains("subject position"), errors.get(0));
    }

    @Test
    void literalPlaceholderInPredicatePositionIsReported() throws Exception {
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, NAME);
        creator.addAssertionStatement(ST1, RDF.OBJECT, THING);
        List<String> errors = new Template(creator.finalizeNanopub()).getStatementErrors();
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("predicate position"), errors.get(0));
    }

    @Test
    void literalPlaceholderInObjectPositionIsFine() throws Exception {
        Template t = new Template(wellFormedCreator().finalizeNanopub());
        assertEquals(List.of(), t.getStatementErrors());
    }

    @Test
    void literalInSubjectPositionIsReportedAndKeptForRendering() throws Exception {
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(ST1, RDF.SUBJECT, vf.createLiteral("not a URI"));
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, NAME);
        Template t = new Template(creator.finalizeNanopub());
        List<String> errors = t.getStatementErrors();
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("\"not a URI\""), errors.get(0));
        assertTrue(errors.get(0).contains("subject position"), errors.get(0));
        // The literal is not a valid subject, so the IRI-typed accessor stays empty, but the
        // declared value is kept so the form can show the user what is wrong:
        assertNull(t.getSubject(ST1));
        assertEquals(vf.createLiteral("not a URI"), t.getDeclaredSubject(ST1));
    }

    @Test
    void undefinedStatementIsReported() throws Exception {
        // Statement referenced by nt:hasStatement but never given a subject/predicate/object:
        Template t = new Template(singleStatementCreator().finalizeNanopub());
        List<String> errors = t.getStatementErrors();
        assertEquals(2, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("no subject is defined"), errors.get(0));
        assertTrue(errors.get(1).contains("no predicate is defined"), errors.get(1));
    }

    @Test
    void errorNamesStatementByNumberAndLocalName() throws Exception {
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(ST1, RDF.SUBJECT, NAME);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, THING);
        List<String> errors = new Template(creator.finalizeNanopub()).getStatementErrors();
        assertTrue(errors.get(0).startsWith("Statement 1 (st1):"), errors.get(0));
    }

    @Test
    void groupMemberErrorIsNumberedWithinItsGroup() throws Exception {
        IRI group = vf.createIRI(NP_URI + "/group");
        IRI st2 = vf.createIRI(NP_URI + "/st2");
        NanopubCreator creator = singleStatementCreator();
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, group);
        creator.addAssertionStatement(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(group, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(group, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST1, RDF.OBJECT, NAME);
        creator.addAssertionStatement(st2, RDF.SUBJECT, NAME);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st2, RDF.OBJECT, THING);
        List<String> errors = new Template(creator.finalizeNanopub()).getStatementErrors();
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).startsWith("Statement 1.2 (st2):"), errors.get(0));
    }

}
