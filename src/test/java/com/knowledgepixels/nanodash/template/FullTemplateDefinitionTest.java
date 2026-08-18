package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Template#hasFullTemplateDefinition(Nanopub)}, which decides
 * whether a nanopublication can be offered as a form to fill in ("use this
 * template..."). Nanopublications that only refer to a template - in particular
 * the registration of a template kind as a maintained resource - must not
 * qualify, even though their type set contains {@code nt:AssertionTemplate}
 * (see issue #597).
 */
public class FullTemplateDefinitionTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    // A syntactically valid trusty URI shape (RA + 43 chars), so sub-IRIs strip correctly:
    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String OTHER_NP_URI = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210_-ZyXwV";
    private static final IRI TEMPLATE_NODE = vf.createIRI(NP_URI + "/template");
    private static final IRI KIND_IRI = vf.createIRI(OTHER_NP_URI + "/templateKind");
    private static final IRI SPACE = vf.createIRI("https://w3id.org/spaces/knowledgepixels/nanodash");

    private static NanopubCreator newCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator;
    }

    private static void addTemplateBody(NanopubCreator creator, IRI templateNode) throws Exception {
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        IRI namePlaceholder = vf.createIRI(NP_URI + "/name");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, namePlaceholder);
        creator.addAssertionStatement(namePlaceholder, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(namePlaceholder, RDFS.LABEL, vf.createLiteral("name"));
    }

    /**
     * The shape of a template-kind registration: the kind IRI, which lives in the
     * namespace of the nanopub that minted it, is typed as an assertion template and
     * declared a maintained resource of a space. There is no template body here.
     */
    private static Nanopub kindRegistrationNanopub() throws Exception {
        NanopubCreator creator = newCreator();
        creator.addAssertionStatement(KIND_IRI, RDF.TYPE, KPXL_TERMS.MAINTAINED_RESOURCE);
        creator.addAssertionStatement(KIND_IRI, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(KIND_IRI, RDFS.LABEL, vf.createLiteral("Commenting on Something Template"));
        creator.addAssertionStatement(KIND_IRI, DCTERMS.DESCRIPTION, vf.createLiteral("Template for commenting on something."));
        creator.addAssertionStatement(KIND_IRI, KPXL_TERMS.IS_MAINTAINED_BY, SPACE);
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, KIND_IRI));
        return creator.finalizeNanopub();
    }

    @Test
    void legacyTemplateIsFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, creator.getAssertionUri());
        assertTrue(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void embeddedIdentityTemplateIsFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, TEMPLATE_NODE);
        creator.addAssertionStatement(TEMPLATE_NODE, DCTERMS.IS_VERSION_OF, KIND_IRI);
        creator.addAssertionStatement(TEMPLATE_NODE, KPXL_TERMS.GOVERNED_BY, SPACE);
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.EMBEDS, TEMPLATE_NODE));
        assertTrue(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void kindRegistrationIsNotFullDefinition() throws Exception {
        assertFalse(Template.hasFullTemplateDefinition(kindRegistrationNanopub()));
    }

    /**
     * The type-based check alone is what made the button appear on kind registrations:
     * the type of the introduced resource counts as a type of the nanopub itself.
     */
    @Test
    void kindRegistrationStillCountsAsAssertionTemplateByType() throws Exception {
        assertTrue(Utils.isNanopubOfClass(kindRegistrationNanopub(), NTEMPLATE.ASSERTION_TEMPLATE));
    }

    @Test
    void templateNodeWithoutStatementsIsNotFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        creator.addAssertionStatement(TEMPLATE_NODE, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(TEMPLATE_NODE, RDFS.LABEL, vf.createLiteral("Test template"));
        assertFalse(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void multipleTemplateNodesAreNotFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        addTemplateBody(creator, TEMPLATE_NODE);
        creator.addAssertionStatement(vf.createIRI(NP_URI + "/template2"), RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        assertFalse(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void plainNanopubIsNotFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        creator.addAssertionStatement(vf.createIRI("http://example.com/thing"), RDFS.LABEL, vf.createLiteral("A thing"));
        assertFalse(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void shaclTemplateIsFullDefinition() throws Exception {
        NanopubCreator creator = newCreator();
        IRI shape = vf.createIRI(NP_URI + "/shape");
        creator.addAssertionStatement(shape, RDF.TYPE, SHACL.NODE_SHAPE);
        creator.addAssertionStatement(shape, SHACL.TARGET_CLASS, vf.createIRI("http://example.com/Thing"));
        assertTrue(Template.hasFullTemplateDefinition(creator.finalizeNanopub()));
    }

    @Test
    void nullIsNotFullDefinition() {
        assertFalse(Template.hasFullTemplateDefinition(null));
    }

}
