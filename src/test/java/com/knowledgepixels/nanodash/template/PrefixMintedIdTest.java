package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * An identifier minted under a prefix the template supplies carries no artifact code, so
 * nothing makes it unique and the publish form has to check it against the identifiers
 * already in use (#646). {@link TemplateContext#getPrefixMintedIris()} is what tells those
 * apart from the identifiers that need no checking: one the user typed out in full (naming an
 * existing thing rather than minting a new one), one minted under the new nanopublication's
 * own namespace (unique by construction), and one derived from the text itself (meant to be
 * arrived at more than once).
 */
class PrefixMintedIdTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String TARGET_NAMESPACE = "https://w3id.org/np/~~~ARTIFACTCODE~~~/";
    private static final String SPACE_PREFIX = "https://w3id.org/spaces/";
    private static final String AIDA_PREFIX = "http://purl.org/aida/";

    // As in "Defining an open-ended Space (with root definition)": an introduced resource
    // named below a fixed prefix.
    private static final IRI SPACE_FIELD = vf.createIRI(NP_URI + "/space");
    // As in "Introducing a user": an introduced resource the user types out in full.
    private static final IRI AGENT_FIELD = vf.createIRI(NP_URI + "/agent");
    // An introduced resource minted under the nanopublication itself.
    private static final IRI LOCAL_FIELD = vf.createIRI(NP_URI + "/local");
    // As in the AIDA sentence templates: an introduced resource whose IRI is derived from
    // the text itself.
    private static final IRI AIDA_FIELD = vf.createIRI(NP_URI + "/aida");

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
     * Builds a template that introduces four resources, one per way of arriving at an IRI.
     */
    private TemplateContext spaceTemplateContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI stSpace = vf.createIRI(NP_URI + "/st1");
        IRI stAgent = vf.createIRI(NP_URI + "/st2");
        IRI stLocal = vf.createIRI(NP_URI + "/st3");
        IRI stAida = vf.createIRI(NP_URI + "/st4");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Defining a space"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stSpace);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stAgent);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stLocal);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stAida);
        creator.addAssertionStatement(stSpace, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stSpace, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(stSpace, RDF.OBJECT, vf.createIRI("https://w3id.org/kpxl/gen/terms/Space"));
        creator.addAssertionStatement(stAgent, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stAgent, RDF.PREDICATE, vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin"));
        creator.addAssertionStatement(stAgent, RDF.OBJECT, AGENT_FIELD);
        creator.addAssertionStatement(stLocal, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stLocal, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(stLocal, RDF.OBJECT, LOCAL_FIELD);
        creator.addAssertionStatement(stAida, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stAida, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(stAida, RDF.OBJECT, AIDA_FIELD);
        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(SPACE_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(SPACE_PREFIX));
        creator.addAssertionStatement(SPACE_FIELD, RDFS.LABEL, vf.createLiteral("Space identifier"));
        creator.addAssertionStatement(AGENT_FIELD, RDF.TYPE, NTEMPLATE.AGENT_PLACEHOLDER);
        creator.addAssertionStatement(AGENT_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(AGENT_FIELD, RDFS.LABEL, vf.createLiteral("an admin of the space"));
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(LOCAL_FIELD, RDFS.LABEL, vf.createLiteral("short id of the record"));
        creator.addAssertionStatement(AIDA_FIELD, RDF.TYPE, NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER);
        creator.addAssertionStatement(AIDA_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(AIDA_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(AIDA_PREFIX));
        creator.addAssertionStatement(AIDA_FIELD, RDFS.LABEL, vf.createLiteral("the AIDA sentence"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", TARGET_NAMESPACE);
        context.initStatements();
        return context;
    }

    // The headline case: the user types "my-space" and the template's prefix turns it into a
    // full IRI that nothing else guarantees to be free.
    @Test
    void aNameTypedBelowAPrefixIsMinted() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("my-space"));
        Value processed = context.processValue(SPACE_FIELD);
        assertEquals(SPACE_PREFIX + "my-space", processed.stringValue());
        assertTrue(context.getPrefixMintedIris().contains(processed),
                "an IRI formed from the template's prefix has to be checked for collisions");
        assertTrue(context.getIntroducedIris().contains(processed));
    }

    // "Defining an open-ended Space with existing URI" exists precisely so that an existing
    // IRI can be used, so a fully typed-out IRI is a reference, not a mint.
    @Test
    void anIriTypedOutInFullIsNotMinted() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("https://example.org/spaces/existing"));
        Value processed = context.processValue(SPACE_FIELD);
        assertEquals("https://example.org/spaces/existing", processed.stringValue());
        assertFalse(context.getPrefixMintedIris().contains(processed),
                "naming an existing resource is not minting a new identifier");
        assertTrue(context.getIntroducedIris().contains(processed));
    }

    // The user-introduction case: the ORCID is an introduced resource that is meant to be
    // introduced again whenever a new key is declared, so it must not be checked.
    @Test
    void anAgentIriWithoutAPrefixIsNotMinted() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(AGENT_FIELD, Model.of("https://orcid.org/0000-0002-1267-0234"));
        Value processed = context.processValue(AGENT_FIELD);
        assertEquals("https://orcid.org/0000-0002-1267-0234", processed.stringValue());
        assertFalse(context.getPrefixMintedIris().contains(processed),
                "a placeholder without a prefix mints nothing");
        assertTrue(context.getIntroducedIris().contains(processed));
    }

    // An AIDA sentence IRI is the sentence itself: two people writing the same sentence are
    // meant to arrive at the same IRI, so finding it already published is agreement rather
    // than a collision.
    @Test
    void anAutoEscapedIriIsNotMinted() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(AIDA_FIELD, Model.of("The cat sat on the mat."));
        Value processed = context.processValue(AIDA_FIELD);
        assertEquals(AIDA_PREFIX + "The+cat+sat+on+the+mat.", processed.stringValue());
        assertFalse(context.getPrefixMintedIris().contains(processed),
                "an IRI derived from the text itself is meant to be arrived at more than once");
        assertTrue(context.getIntroducedIris().contains(processed));
    }

    // A local resource picks up this nanopublication's artifact code at signing time, which
    // is what makes it unique; there is nothing to check.
    @Test
    void aResourceMintedUnderTheNanopubIsNotChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(LOCAL_FIELD, Model.of("record"));
        Value processed = context.processValue(LOCAL_FIELD);
        assertEquals(TARGET_NAMESPACE + "record", processed.stringValue());
        assertFalse(context.getPrefixMintedIris().contains(processed),
                "an identifier carrying the nanopublication's artifact code is unique by construction");
        assertTrue(context.getIntroducedIris().contains(processed));
    }

    // What the publish form does with all of the above: refuse the publication and name the
    // identifier that is taken.
    @Test
    void aTakenIdentifierIsReported() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("example/bar"));
        IRI minted = (IRI) context.processValue(SPACE_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(minted.stringValue())).thenReturn(true);
            assertEquals(minted, PublishForm.findTakenMintedId(context));
        }
    }

    @Test
    void aFreeIdentifierLetsThePublicationThrough() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("nobody-took-this"));
        context.processValue(SPACE_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(anyString())).thenReturn(false);
            assertNull(PublishForm.findTakenMintedId(context));
        }
    }

    // Superseding and overriding keep the source's identifier on purpose (docs/fill-modes.md),
    // so the identifier being in use is exactly what is expected there.
    @Test
    void supersedingKeepsTheIdentifierWithoutAsking() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.setFillMode(FillMode.SUPERSEDE);
        context.getComponentModels().put(SPACE_FIELD, Model.of("example/bar"));
        context.processValue(SPACE_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenMintedId(context));
            q.verifyNoInteractions();
        }
    }

}
