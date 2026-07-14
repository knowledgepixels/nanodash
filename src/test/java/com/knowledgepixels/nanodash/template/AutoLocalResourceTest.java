package com.knowledgepixels.nanodash.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the automatic Local Resource treatment of template-local IRIs (issue
 * #551): a local IRI used in a statement position without any identity type is
 * treated as if it were tagged nt:LocalResource, so produced nanopubs mint it
 * under their own namespace instead of copying it verbatim.
 */
public class AutoLocalResourceTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI LOCAL_SUBJ = vf.createIRI(NP_URI + "/localthing");
    private static final IRI LOCAL_PRED = vf.createIRI(NP_URI + "/haslocalrelation");
    private static final IRI LOCAL_OBJ = vf.createIRI(NP_URI + "/localvalue");
    private static final IRI EXTERNAL_OBJ = vf.createIRI("http://example.com/SomeClass");

    private static NanopubCreator templateCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Auto local resource test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, LOCAL_SUBJ);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, LOCAL_PRED);
        creator.addAssertionStatement(ST1, RDF.OBJECT, LOCAL_OBJ);
        return creator;
    }

    @Test
    void untypedLocalIrisBecomeLocalResourcesInAllPositions() throws Exception {
        Template t = new Template(templateCreator().finalizeNanopub());
        assertTrue(t.isLocalResource(LOCAL_SUBJ), "untyped local subject must become a Local Resource");
        assertTrue(t.isLocalResource(LOCAL_PRED), "untyped local predicate must become a Local Resource");
        assertTrue(t.isLocalResource(LOCAL_OBJ), "untyped local object must become a Local Resource");
    }

    @Test
    void externalIrisStayUntouched() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(ST1, RDF.OBJECT, EXTERNAL_OBJ);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLocalResource(EXTERNAL_OBJ));
    }

    @Test
    void placeholdersStayUntouched() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(LOCAL_SUBJ, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLocalResource(LOCAL_SUBJ), "a placeholder must not be auto-tagged as Local Resource");
        assertTrue(t.isPlaceholder(LOCAL_SUBJ));
    }

    @Test
    void introducedResourcesStayUntouched() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(LOCAL_SUBJ, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLocalResource(LOCAL_SUBJ), "an introduced resource must not be auto-tagged as Local Resource");
        assertTrue(t.isIntroducedResource(LOCAL_SUBJ));
    }

    @Test
    void explicitLocalResourceTagStillWorks() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(LOCAL_SUBJ, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isLocalResource(LOCAL_SUBJ));
    }

    @Test
    void localIrisInDeadStatementsStayUntouched() throws Exception {
        // A statement definition not referenced via nt:hasStatement is dead; its
        // local IRIs are never published and must not be tagged.
        NanopubCreator creator = templateCreator();
        IRI deadStatement = vf.createIRI(NP_URI + "/deadst");
        IRI deadLocal = vf.createIRI(NP_URI + "/deadlocal");
        creator.addAssertionStatement(deadStatement, RDF.SUBJECT, deadLocal);
        creator.addAssertionStatement(deadStatement, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(deadStatement, RDF.OBJECT, EXTERNAL_OBJ);
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLocalResource(deadLocal), "local IRIs in unreferenced statement definitions must not be tagged");
    }

    @Test
    void templateNanopubUriItselfStaysUntouched() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(ST1, RDF.OBJECT, vf.createIRI(NP_URI));
        Template t = new Template(creator.finalizeNanopub());
        assertFalse(t.isLocalResource(vf.createIRI(NP_URI)), "the template nanopub URI is a global reference, not a local IRI");
    }

    @Test
    void groupMemberStatementsAreCovered() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI group = vf.createIRI(NP_URI + "/group");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Group test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, group);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(group, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, LOCAL_SUBJ);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(ST1, RDF.OBJECT, EXTERNAL_OBJ);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isLocalResource(LOCAL_SUBJ), "local IRIs inside grouped statements must be tagged too");
    }

}
