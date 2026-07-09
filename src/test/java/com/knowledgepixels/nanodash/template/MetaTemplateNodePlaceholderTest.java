package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the fill mechanics that an updated template-creation meta-template
 * relies on (docs/template-identity-and-governance.md): the defined template's
 * node expressed as a local URI placeholder (instead of the nt:ASSERTION
 * sentinel), with a default local name, must (a) pre-fill on create, (b) unify
 * to "assertion" when filling from a legacy-shaped template (keeping its shape
 * on round-trip), and (c) unify to the embedded node's local name when filling
 * from a template with embedded identity.
 */
public class MetaTemplateNodePlaceholderTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String META_URI = "https://w3id.org/np/RAMetaMetaMetaMetaMetaMetaMetaMetaMetaMet01";
    private static final String LEGACY_URI = "https://w3id.org/np/RALegacyLegacyLegacyLegacyLegacyLegacyLeg02";
    private static final String NEWSTYLE_URI = "https://w3id.org/np/RANewstyleNewstyleNewstyleNewstyleNewsty03";

    private static final IRI NODE_PLACEHOLDER = vf.createIRI(META_URI + "/templateNode");
    private static final IRI LABEL_PLACEHOLDER = vf.createIRI(META_URI + "/tlabel");
    private static final IRI KIND_PLACEHOLDER = vf.createIRI(META_URI + "/templateKind");

    private TemplateContext context;

    @BeforeEach
    void setUp() throws Exception {
        new WicketTester(new WicketApplication());
        // Mini meta-template: the defined template's node is a local URI placeholder
        // with default local name "template" (the proposed new shape), instead of
        // the nt:ASSERTION sentinel (the current shape).
        NanopubCreator creator = new NanopubCreator(META_URI);
        IRI assertionUri = creator.getAssertionUri();
        IRI st1 = vf.createIRI(META_URI + "/st1");
        IRI st2 = vf.createIRI(META_URI + "/st2");
        creator.addAssertionStatement(assertionUri, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(assertionUri, RDFS.LABEL, vf.createLiteral("Mini meta-template"));
        creator.addAssertionStatement(assertionUri, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(assertionUri, NTEMPLATE.HAS_STATEMENT, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, NODE_PLACEHOLDER);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(st1, RDF.OBJECT, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(st2, RDF.SUBJECT, NODE_PLACEHOLDER);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st2, RDF.OBJECT, LABEL_PLACEHOLDER);
        creator.addAssertionStatement(NODE_PLACEHOLDER, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(NODE_PLACEHOLDER, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(NODE_PLACEHOLDER, RDFS.LABEL, vf.createLiteral("local name of the template node"));
        creator.addAssertionStatement(NODE_PLACEHOLDER, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createIRI(META_URI + "/template"));
        creator.addAssertionStatement(LABEL_PLACEHOLDER, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(LABEL_PLACEHOLDER, RDFS.LABEL, vf.createLiteral("label of the template"));
        // Optional kind statement with an introduced-resource placeholder, mirroring
        // the real meta-template's kind mechanism (dct:isVersionOf a stable kind IRI):
        IRI st3 = vf.createIRI(META_URI + "/st3");
        creator.addAssertionStatement(assertionUri, NTEMPLATE.HAS_STATEMENT, st3);
        creator.addAssertionStatement(st3, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(st3, RDF.SUBJECT, NODE_PLACEHOLDER);
        creator.addAssertionStatement(st3, RDF.PREDICATE, DCTERMS.IS_VERSION_OF);
        creator.addAssertionStatement(st3, RDF.OBJECT, KIND_PLACEHOLDER);
        creator.addAssertionStatement(KIND_PLACEHOLDER, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(KIND_PLACEHOLDER, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(KIND_PLACEHOLDER, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(KIND_PLACEHOLDER, RDFS.LABEL, vf.createLiteral("stable kind identifier"));
        creator.addProvenanceStatement(vf.createStatement(assertionUri, RDFS.SEEALSO, assertionUri));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        Template meta = TemplateData.get().registerTemplate(creator.finalizeNanopub());
        assertNotNull(meta);

        context = new TemplateContext(ContextType.ASSERTION, META_URI, "statement",
                "https://example.org/np/~~~ARTIFACTCODE~~~/");
    }

    private static Nanopub templateNanopub(String npUri, IRI templateNode, String label) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        IRI node = (templateNode == null ? creator.getAssertionUri() : templateNode);
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral(label));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        if (templateNode == null) {
            creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        } else {
            creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.EMBEDS, templateNode));
        }
        return creator.finalizeNanopub();
    }

    private String modelValue(IRI placeholderIri) {
        IModel<String> m = (IModel<String>) context.getComponentModels().get(placeholderIri);
        assertNotNull(m, "component model for " + placeholderIri + " should exist; present keys: "
                + context.getComponentModels().keySet());
        return m.getObject();
    }

    /**
     * Whether the kind placeholder is backed by an editable form field: editable
     * items (e.g. IriTextfieldItem) register their field in the context's component
     * list, while ReadonlyItem registers only a model.
     */
    private boolean kindHasEditableField() {
        IModel<?> m = context.getComponentModels().get(KIND_PLACEHOLDER);
        if (m == null) return false;
        for (org.apache.wicket.Component c : context.getComponents()) {
            if (c.getDefaultModel() == m) return true;
        }
        return false;
    }

    @Test
    void createModePreFillsDefaultLocalName() {
        context.initStatements();
        context.finalizeStatements();
        assertEquals("template", modelValue(NODE_PLACEHOLDER));
    }

    @Test
    void fillFromLegacyTemplateUnifiesNodeToAssertion() throws Exception {
        context.setFillMode(FillMode.SUPERSEDE);
        context.initStatements();
        Nanopub legacy = templateNanopub(LEGACY_URI, null, "My old template");
        context.setFillSource(legacy);
        ValueFiller filler = new ValueFiller(legacy, ContextType.ASSERTION, true, FillMode.SUPERSEDE);
        filler.fill(context);
        context.finalizeStatements();
        assertEquals("assertion", modelValue(NODE_PLACEHOLDER));
        assertEquals("My old template", modelValue(LABEL_PLACEHOLDER));
        assertEquals(0, filler.getUnusedStatements().size(),
                "all statements of the legacy template should unify: " + filler.getUnusedStatements());
    }

    @Test
    void fillFromEmbeddedIdentityTemplateUnifiesNodeToLocalName() throws Exception {
        context.setFillMode(FillMode.SUPERSEDE);
        context.initStatements();
        Nanopub newStyle = templateNanopub(NEWSTYLE_URI, vf.createIRI(NEWSTYLE_URI + "/my-nice-template"), "My new template");
        context.setFillSource(newStyle);
        ValueFiller filler = new ValueFiller(newStyle, ContextType.ASSERTION, true, FillMode.SUPERSEDE);
        filler.fill(context);
        context.finalizeStatements();
        assertEquals("my-nice-template", modelValue(NODE_PLACEHOLDER));
        assertEquals("My new template", modelValue(LABEL_PLACEHOLDER));
        assertEquals(0, filler.getUnusedStatements().size(),
                "all statements of the new-style template should unify: " + filler.getUnusedStatements());
    }

    // Regression for issue #549: an introduced-resource placeholder in an optional
    // statement must stay fillable when the superseded nanopub doesn't introduce
    // anything -- otherwise a kind can never be ADDED to an existing template.
    // (The fill source is set before initStatements, as PublishForm does.)
    @Test
    void kindFieldStaysFillableWhenSourceIntroducesNothing() throws Exception {
        context.setFillMode(FillMode.SUPERSEDE);
        Nanopub legacy = templateNanopub(LEGACY_URI, null, "My old template");
        context.setFillSource(legacy);
        context.initStatements();
        ValueFiller filler = new ValueFiller(legacy, ContextType.ASSERTION, true, FillMode.SUPERSEDE);
        filler.fill(context);
        context.finalizeStatements();
        assertTrue(kindHasEditableField(),
                "the kind field must be editable when the superseded nanopub introduces nothing");
        assertEquals("assertion", modelValue(NODE_PLACEHOLDER));
    }

    @Test
    void kindFieldPinnedWhenSourceIntroducesKind() throws Exception {
        context.setFillMode(FillMode.SUPERSEDE);
        IRI node = vf.createIRI(NEWSTYLE_URI + "/my-nice-template");
        IRI kind = vf.createIRI(NEWSTYLE_URI + "/my-nice-template-kind");
        NanopubCreator creator = new NanopubCreator(NEWSTYLE_URI);
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("My governed template"));
        creator.addAssertionStatement(node, DCTERMS.IS_VERSION_OF, kind);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.EMBEDS, node));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, kind));
        Nanopub governed = creator.finalizeNanopub();
        context.setFillSource(governed);
        context.initStatements();
        ValueFiller filler = new ValueFiller(governed, ContextType.ASSERTION, true, FillMode.SUPERSEDE);
        filler.fill(context);
        context.finalizeStatements();
        assertFalse(kindHasEditableField(),
                "the kind field must be pinned read-only when the superseded nanopub introduces it");
        assertEquals(kind.stringValue(), modelValue(KIND_PLACEHOLDER),
                "the pinned kind must keep the original full IRI");
        assertEquals(0, filler.getUnusedStatements().size(),
                "all statements should unify: " + filler.getUnusedStatements());
    }

}
