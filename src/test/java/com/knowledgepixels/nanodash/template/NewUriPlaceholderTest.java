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
 * An identifier for a resource that does not exist yet carries no artifact code, so nothing
 * makes it unique and the publish form has to check it against the identifiers already in use
 * (#646). Which values those are is the template author's call, declared by tagging the
 * placeholder with {@link Template#NEW_URI_PLACEHOLDER}: a tagged placeholder is checked
 * however its value was formed, and an untagged one is never checked, even when its IRI is
 * built from a prefix and already exists.
 */
class NewUriPlaceholderTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String TARGET_NAMESPACE = "https://w3id.org/np/~~~ARTIFACTCODE~~~/";
    private static final String SPACE_PREFIX = "https://w3id.org/spaces/";
    private static final String TOPIC_PREFIX = "https://w3id.org/topics/";

    // As in "Defining an open-ended Space (with root definition)": a new resource named below
    // a fixed prefix, tagged so that the identifier gets checked.
    private static final IRI SPACE_FIELD = vf.createIRI(NP_URI + "/space");
    // Tagged, but with no prefix: the user types the identifier of the new resource in full.
    private static final IRI FULL_IRI_FIELD = vf.createIRI(NP_URI + "/fullIri");
    // Prefixed and introduced, but untagged: nothing is checked for it.
    private static final IRI TOPIC_FIELD = vf.createIRI(NP_URI + "/topic");
    // Tagged but minted under the nanopublication itself, which the artifact code makes unique.
    private static final IRI LOCAL_FIELD = vf.createIRI(NP_URI + "/local");

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
     * Builds a template with one placeholder per combination of tag and prefix that matters.
     */
    private TemplateContext spaceTemplateContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI stSpace = vf.createIRI(NP_URI + "/st1");
        IRI stFullIri = vf.createIRI(NP_URI + "/st2");
        IRI stTopic = vf.createIRI(NP_URI + "/st3");
        IRI stLocal = vf.createIRI(NP_URI + "/st4");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Defining a space"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stSpace);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stFullIri);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stTopic);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stLocal);
        creator.addAssertionStatement(stSpace, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stSpace, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(stSpace, RDF.OBJECT, vf.createIRI("https://w3id.org/kpxl/gen/terms/Space"));
        creator.addAssertionStatement(stFullIri, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stFullIri, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(stFullIri, RDF.OBJECT, FULL_IRI_FIELD);
        creator.addAssertionStatement(stTopic, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stTopic, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(stTopic, RDF.OBJECT, TOPIC_FIELD);
        creator.addAssertionStatement(stLocal, RDF.SUBJECT, SPACE_FIELD);
        creator.addAssertionStatement(stLocal, RDF.PREDICATE, RDFS.ISDEFINEDBY);
        creator.addAssertionStatement(stLocal, RDF.OBJECT, LOCAL_FIELD);

        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, Template.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(SPACE_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(SPACE_PREFIX));
        creator.addAssertionStatement(SPACE_FIELD, RDFS.LABEL, vf.createLiteral("Space identifier"));

        creator.addAssertionStatement(FULL_IRI_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(FULL_IRI_FIELD, RDF.TYPE, Template.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(FULL_IRI_FIELD, RDFS.LABEL, vf.createLiteral("identifier of the new record"));

        creator.addAssertionStatement(TOPIC_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(TOPIC_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(TOPIC_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(TOPIC_PREFIX));
        creator.addAssertionStatement(TOPIC_FIELD, RDFS.LABEL, vf.createLiteral("the topic"));

        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, Template.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDFS.LABEL, vf.createLiteral("short id of the record"));

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
    void aTaggedNameBelowAPrefixIsChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("my-space"));
        Value processed = context.processValue(SPACE_FIELD);
        assertEquals(SPACE_PREFIX + "my-space", processed.stringValue());
        assertTrue(context.getNewUriIris().contains(processed),
                "a tagged placeholder has to be checked for collisions");
    }

    // The tag says the value names something new, so how the IRI was arrived at is beside the
    // point: a typed-out IRI is checked exactly like a prefixed one.
    @Test
    void aTaggedIriTypedOutInFullIsChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(FULL_IRI_FIELD, Model.of("https://example.org/records/new-one"));
        Value processed = context.processValue(FULL_IRI_FIELD);
        assertEquals("https://example.org/records/new-one", processed.stringValue());
        assertTrue(context.getNewUriIris().contains(processed),
                "the tag decides, not whether a prefix was used");
    }

    // The point of making this opt-in: every template that does not ask for the check keeps
    // publishing as before, prefix or no prefix, existing identifier or not.
    @Test
    void anUntaggedPlaceholderIsNeverChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(TOPIC_FIELD, Model.of("cats"));
        Value processed = context.processValue(TOPIC_FIELD);
        assertEquals(TOPIC_PREFIX + "cats", processed.stringValue());
        assertFalse(context.getNewUriIris().contains(processed),
                "an untagged placeholder is left alone even when its IRI is built from a prefix");
        assertTrue(context.getIntroducedIris().contains(processed),
                "being an introduced resource is not by itself a reason to check");
    }

    // A local resource picks up this nanopublication's artifact code at signing time, so the
    // value seen here is not the one that gets published and there is nothing to look up.
    @Test
    void aTaggedResourceMintedUnderTheNanopubIsNotChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(LOCAL_FIELD, Model.of("record"));
        Value processed = context.processValue(LOCAL_FIELD);
        assertEquals(TARGET_NAMESPACE + "record", processed.stringValue());
        assertFalse(context.getNewUriIris().contains(processed),
                "an identifier carrying the nanopublication's artifact code is unique by construction");
    }

    // What the publish form does with all of the above: refuse the publication and name the
    // identifier that is taken.
    @Test
    void aTakenIdentifierIsReported() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("example/bar"));
        IRI newUri = (IRI) context.processValue(SPACE_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(newUri.stringValue())).thenReturn(true);
            assertEquals(newUri, PublishForm.findTakenNewUri(context));
        }
    }

    @Test
    void aFreeIdentifierLetsThePublicationThrough() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("nobody-took-this"));
        context.processValue(SPACE_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(anyString())).thenReturn(false);
            assertNull(PublishForm.findTakenNewUri(context));
        }
    }

    // An untagged placeholder must not even reach the query service: the check is off for it,
    // not merely tolerant of what it finds.
    @Test
    void anUntaggedPlaceholderIsNotLookedUpAtAll() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(TOPIC_FIELD, Model.of("cats"));
        context.processValue(TOPIC_FIELD);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenNewUri(context));
            q.verifyNoInteractions();
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
            assertNull(PublishForm.findTakenNewUri(context));
            q.verifyNoInteractions();
        }
    }

}
