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
 * Tests for the nt:TransientTemplate flag (issue #606): a pubinfo template so flagged
 * has its filled content bound to the one nanopub it was published with, and the
 * publish form discards that content instead of carrying it over when the nanopub is
 * superseded, overridden, derived from, or used as a fill source.
 */
public class TransientTemplateTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI NOTE = vf.createIRI(NP_URI + "/note");

    private static NanopubCreator templateCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.PUBINFO_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Transient template test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, vf.createIRI(NP_URI + "/nanopub"));
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, NOTE);
        creator.addAssertionStatement(NOTE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        return creator;
    }

    @Test
    void flaggedTemplateIsTransient() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(creator.getAssertionUri(), RDF.TYPE, Template.TRANSIENT_TEMPLATE);
        Template t = new Template(creator.finalizeNanopub());
        assertTrue(t.isTransient());
    }

    @Test
    void unflaggedTemplateIsNotTransient() throws Exception {
        Template t = new Template(templateCreator().finalizeNanopub());
        assertFalse(t.isTransient());
    }

}
