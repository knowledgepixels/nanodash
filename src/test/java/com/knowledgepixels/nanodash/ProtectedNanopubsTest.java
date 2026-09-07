package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtectedNanopubsTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static Nanopub nanopub(String npUri, boolean isProtected) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addAssertionStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        return creator.finalizeNanopub();
    }

    /**
     * Registers a minimal pubinfo template, so that its {@link Template} object can be handed to
     * the policy without publishing anything. The nanopub carrying it is protected or not.
     */
    private static Template template(String npUri, boolean isProtected) throws Exception {
        IRI st1 = vf.createIRI(npUri + "/st1");
        NanopubCreator creator = new NanopubCreator(npUri);
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.PUBINFO_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Protected nanopubs test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, NTEMPLATE.NANOPUB_PLACEHOLDER);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(st1, RDF.OBJECT, NTEMPLATE.NANOPUB_PLACEHOLDER);
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        if (isProtected) creator.addPubinfoStatement(RDF.TYPE, NPX.PROTECTED_NANOPUB);
        Template template = TemplateData.get().registerTemplate(creator.finalizeNanopub());
        assertNotNull(template);
        return template;
    }

    @Test
    void protectedMarkerIsRecognized() throws Exception {
        assertTrue(ProtectedNanopubs.isProtected(nanopub("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000001", true)));
        assertFalse(ProtectedNanopubs.isProtected(nanopub("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000002", false)));
        assertFalse(ProtectedNanopubs.isProtected(null));
    }

    @Test
    void aProtectedFillSourceForcesProtection() throws Exception {
        // A new version of, or a nanopublication derived from, a protected one repeats its
        // content, so publishing it unprotected would expose exactly what was protected.
        Nanopub source = nanopub("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000003", true);
        assertEquals("the nanopublication it is based on is protected",
                ProtectedNanopubs.getForcedReason(source, List.of()));
    }

    @Test
    void aProtectedTemplateForcesProtection() throws Exception {
        // The template is stored on the local instance only, so a public nanopublication made
        // with it would carry a nt:wasCreatedFromTemplate link nobody outside can follow.
        Template protectedTemplate = template("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000004", true);
        assertEquals("the template it is based on is protected",
                ProtectedNanopubs.getForcedReason(null, List.of(protectedTemplate)));
    }

    @Test
    void ordinarySourcesLeaveTheChoiceFree() throws Exception {
        Nanopub source = nanopub("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000005", false);
        Template plainTemplate = template("https://w3id.org/np/RAProtectedNanopubsTest0000000000000000000006", false);
        assertNull(ProtectedNanopubs.getForcedReason(source, List.of(plainTemplate)));
        assertNull(ProtectedNanopubs.getForcedReason(null, null));
    }

}
