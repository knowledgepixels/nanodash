package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the two template identity forms: legacy (template node = assertion
 * graph URI, ID = nanopub URI) and embedded (template node = embedded IRI in the
 * assertion, ID = that IRI).
 */
public class TemplateIdentityTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // A syntactically valid trusty URI shape (RA + 43 chars), so sub-IRIs strip correctly:
    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI TEMPLATE_NODE = vf.createIRI(NP_URI + "/template");
    private static final IRI KIND_IRI = vf.createIRI(NP_URI + "/test-template-kind");

    private static NanopubCreator newCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator;
    }

    /**
     * Adds the template body (label, one reified statement with a literal
     * placeholder) with the given IRI as the template node.
     */
    private static void addTemplateBody(NanopubCreator creator, IRI templateNode) throws Exception {
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI namePlaceholder = vf.createIRI(NP_URI + "/name");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Test template"));
        creator.addAssertionStatement(templateNode, DCTERMS.DESCRIPTION, vf.createLiteral("A template for testing."));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, namePlaceholder);
        creator.addAssertionStatement(namePlaceholder, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(namePlaceholder, RDFS.LABEL, vf.createLiteral("name"));
    }

    private static Nanopub legacyTemplateNanopub() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, creator.getAssertionUri());
        return creator.finalizeNanopub();
    }

    private static Nanopub embeddedTemplateNanopub() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, TEMPLATE_NODE);
        creator.addAssertionStatement(TEMPLATE_NODE, DCTERMS.IS_VERSION_OF, KIND_IRI);
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.EMBEDS, TEMPLATE_NODE));
        return creator.finalizeNanopub();
    }

    @Test
    void legacyTemplateParses() throws Exception {
        Template t = new Template(legacyTemplateNanopub());
        assertEquals(NP_URI, t.getId());
        assertTrue(t.hasId(NP_URI));
        assertFalse(t.hasId(TEMPLATE_NODE.stringValue()));
        assertEquals("Test template", t.getLabel());
        assertEquals(1, t.getStatementIris().size());
        assertFalse(t.isUnlisted());
        assertNull(t.getTemplateKindIri());
    }

    @Test
    void embeddedIdentityTemplateParses() throws Exception {
        Template t = new Template(embeddedTemplateNanopub());
        assertEquals(TEMPLATE_NODE.stringValue(), t.getId());
        assertTrue(t.hasId(TEMPLATE_NODE.stringValue()));
        assertTrue(t.hasId(NP_URI));
        assertEquals("Test template", t.getLabel());
        assertEquals("A template for testing.", t.getDescription());
        assertEquals(1, t.getStatementIris().size());
        assertFalse(t.isUnlisted());
        assertEquals(KIND_IRI, t.getTemplateKindIri());
    }

    @Test
    void unlistedEmbeddedTemplate() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, TEMPLATE_NODE);
        creator.addAssertionStatement(TEMPLATE_NODE, RDF.TYPE, NTEMPLATE.UNLISTED_TEMPLATE);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isUnlisted());
    }

    @Test
    void legacyTemplateKindParses() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, creator.getAssertionUri());
        creator.addAssertionStatement(creator.getAssertionUri(), DCTERMS.IS_VERSION_OF, KIND_IRI);
        Template t = new Template(creator.finalizeNanopub());
        assertEquals(NP_URI, t.getId());
        assertEquals(KIND_IRI, t.getTemplateKindIri());
    }

    @Test
    void legacyShapeTakesPrecedenceOverOtherTypedNodes() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, creator.getAssertionUri());
        creator.addAssertionStatement(TEMPLATE_NODE, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        Template t = new Template(creator.finalizeNanopub());
        assertEquals(NP_URI, t.getId());
    }

    @Test
    void multipleEmbeddedTemplateNodesAreMalformed() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, TEMPLATE_NODE);
        creator.addAssertionStatement(vf.createIRI(NP_URI + "/template2"), RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        Nanopub np = creator.finalizeNanopub();
        assertThrows(MalformedTemplateException.class, () -> new Template(np));
    }

    @Test
    void templateNodeOutsideNanopubNamespaceIsMalformed() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, vf.createIRI("http://example.com/external-template"));
        Nanopub np = creator.finalizeNanopub();
        assertThrows(MalformedTemplateException.class, () -> new Template(np));
    }

    @Test
    void embeddedProvenanceAndPubinfoTemplateNodesAreDiscovered() throws Exception {
        for (IRI type : new IRI[]{NTEMPLATE.PROVENANCE_TEMPLATE, NTEMPLATE.PUBINFO_TEMPLATE}) {
            NanopubCreator creator = newCreator();
            creator.addAssertionStatement(TEMPLATE_NODE, RDF.TYPE, type);
            creator.addAssertionStatement(TEMPLATE_NODE, RDFS.LABEL, vf.createLiteral("Test template"));
            Template t = new Template(creator.finalizeNanopub());
            assertEquals(TEMPLATE_NODE.stringValue(), t.getId());
        }
    }

    @Test
    void stripToNanopubId() {
        assertEquals(NP_URI, Utils.stripToNanopubId(TEMPLATE_NODE.stringValue()));
        assertEquals(NP_URI, Utils.stripToNanopubId(NP_URI + "#name"));
        assertEquals(NP_URI, Utils.stripToNanopubId(NP_URI));
    }

    @Test
    void registeredTemplateResolvesUnderBothIdForms() throws Exception {
        TemplateData td = TemplateData.get();
        Template t = td.registerTemplate(embeddedTemplateNanopub());
        assertEquals(TEMPLATE_NODE.stringValue(), t.getId());
        assertTrue(t == td.getTemplate(NP_URI));
        assertTrue(t == td.getTemplate(TEMPLATE_NODE.stringValue()));
    }

}
