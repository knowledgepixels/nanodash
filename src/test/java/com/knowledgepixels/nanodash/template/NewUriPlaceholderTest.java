package com.knowledgepixels.nanodash.template;

import org.nanopub.templates.Template;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.NewUriCheck;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * An identifier for a resource that does not exist yet carries no artifact code, so nothing
 * makes it unique and the publish form has to check it against the identifiers already in use
 * (#646). Which values those are is the template author's call, declared by tagging the
 * placeholder with {@link org.nanopub.vocabulary.NTEMPLATE#NEW_URI_PLACEHOLDER}: a tagged placeholder is checked
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
        creator.addAssertionStatement(SPACE_FIELD, RDF.TYPE, NTEMPLATE.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(SPACE_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(SPACE_PREFIX));
        creator.addAssertionStatement(SPACE_FIELD, RDFS.LABEL, vf.createLiteral("Space identifier"));

        creator.addAssertionStatement(FULL_IRI_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(FULL_IRI_FIELD, RDF.TYPE, NTEMPLATE.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(FULL_IRI_FIELD, RDFS.LABEL, vf.createLiteral("identifier of the new record"));

        creator.addAssertionStatement(TOPIC_FIELD, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(TOPIC_FIELD, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(TOPIC_FIELD, NTEMPLATE.HAS_PREFIX, vf.createLiteral(TOPIC_PREFIX));
        creator.addAssertionStatement(TOPIC_FIELD, RDFS.LABEL, vf.createLiteral("the topic"));

        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(LOCAL_FIELD, RDF.TYPE, NTEMPLATE.NEW_URI_PLACEHOLDER);
        creator.addAssertionStatement(LOCAL_FIELD, RDFS.LABEL, vf.createLiteral("short id of the record"));

        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", TARGET_NAMESPACE);
        context.initStatements();
        return context;
    }

    // The form with a value in each field, the given one in the given field.
    private TemplateContext filledContext(IRI field, String value) throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.getComponentModels().put(SPACE_FIELD, Model.of("a-space"));
        context.getComponentModels().put(FULL_IRI_FIELD, Model.of("https://example.org/records/a-record"));
        context.getComponentModels().put(TOPIC_FIELD, Model.of("a-topic"));
        context.getComponentModels().put(LOCAL_FIELD, Model.of("a-record"));
        context.getComponentModels().put(field, Model.of(value));
        return context;
    }

    // The headline case: the user types "my-space" and the template's prefix turns it into a
    // full IRI that nothing else guarantees to be free.
    @Test
    void aTaggedNameBelowAPrefixIsChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertEquals(vf.createIRI(SPACE_PREFIX + "my-space"), context.getNewUriIri(SPACE_FIELD, "my-space"),
                "a tagged placeholder has to be checked for collisions");
    }

    // The tag says the value names something new, so how the IRI was arrived at is beside the
    // point: a typed-out IRI is checked exactly like a prefixed one.
    @Test
    void aTaggedIriTypedOutInFullIsChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertEquals(vf.createIRI("https://example.org/records/new-one"), context.getNewUriIri(FULL_IRI_FIELD, "https://example.org/records/new-one"),
                "the tag decides, not whether a prefix was used");
    }

    // The point of making this opt-in: every template that does not ask for the check keeps
    // publishing as before, prefix or no prefix, existing identifier or not.
    @Test
    void anUntaggedPlaceholderIsNeverChecked() throws Exception {
        TemplateContext context = filledContext(TOPIC_FIELD, "cats");
        assertNull(context.getNewUriIri(TOPIC_FIELD, "cats"),
                "an untagged placeholder is left alone even when its IRI is built from a prefix");
        assertTrue(NanopubUtils.getIntroducedIriIds(TestFill.fill(context)).contains(TOPIC_PREFIX + "cats"),
                "being an introduced resource is not by itself a reason to check");
    }

    // A local resource picks up this nanopublication's artifact code at signing time, so the
    // value seen here is not the one that gets published and there is nothing to look up.
    @Test
    void aTaggedResourceMintedUnderTheNanopubIsNotChecked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertNull(context.getNewUriIri(LOCAL_FIELD, "record"),
                "an identifier carrying the nanopublication's artifact code is unique by construction");
    }

    // What the publish form does with all of the above: refuse the publication and name the
    // identifier that is taken.
    @Test
    void aTakenIdentifierIsReported() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "example/bar");
        Nanopub np = TestFill.fill(context);
        IRI newUri = vf.createIRI(SPACE_PREFIX + "example/bar");
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(newUri.stringValue())).thenReturn(true);
            assertEquals(newUri, PublishForm.findTakenNewUri(context, np));
        }
    }

    @Test
    void aFreeIdentifierLetsThePublicationThrough() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "nobody-took-this");
        Nanopub np = TestFill.fill(context);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(anyString())).thenReturn(false);
            assertNull(PublishForm.findTakenNewUri(context, np));
            // Both tagged fields, and nothing else:
            q.verify(() -> QueryApiAccess.isUriIntroduced(anyString()), times(2));
        }
    }

    // An untagged placeholder must not even reach the query service: the check is off for it,
    // not merely tolerant of what it finds.
    @Test
    void anUntaggedPlaceholderIsNotLookedUpAtAll() throws Exception {
        TemplateContext context = filledContext(TOPIC_FIELD, "cats");
        Nanopub np = TestFill.fill(context);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenNewUri(context, np));
            q.verify(() -> QueryApiAccess.isUriIntroduced(TOPIC_PREFIX + "cats"), never());
        }
    }

    // A value that does not make it into the nanopublication is not looked up either.
    @Test
    void anIdentifierLeftOutOfTheNanopublicationIsNotLookedUp() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "example/bar");
        Nanopub np = TestFill.fill(filledContext(SPACE_FIELD, "example/other"));
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenNewUri(context, np));
            q.verify(() -> QueryApiAccess.isUriIntroduced(SPACE_PREFIX + "example/bar"), never());
        }
    }

    // A new version carries the source's resource over (docs/fill-modes.md), so finding that
    // identifier in use is the expected answer rather than a collision.
    @Test
    void supersedingKeepsTheSourcesIdentifierWithoutAsking() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "example/bar");
        context.setFillMode(FillMode.SUPERSEDE);
        context.setFillSource(sourceUsing(SPACE_PREFIX + "example/bar"));
        Nanopub np = TestFill.fill(context);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenNewUri(context, np));
            q.verify(() -> QueryApiAccess.isUriIntroduced(SPACE_PREFIX + "example/bar"), never());
        }
    }

    // Nothing re-mints a prefix-minted identifier for the new version, so editing the name
    // while superseding defines a resource the source never had, which can collide like any
    // other. Exempting the whole fill mode used to let this through unchecked.
    @Test
    void supersedingUnderANewNameIsCheckedLikeAnyOther() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "example/renamed");
        context.setFillMode(FillMode.SUPERSEDE);
        context.setFillSource(sourceUsing(SPACE_PREFIX + "example/bar"));
        Nanopub np = TestFill.fill(context);
        IRI renamed = vf.createIRI(SPACE_PREFIX + "example/renamed");
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.isUriIntroduced(renamed.stringValue())).thenReturn(true);
            assertEquals(renamed, PublishForm.findTakenNewUri(context, np));
        }
    }

    // With no source to compare against there is nothing to say whether the identifier is the
    // one being carried over, so publishing is left exactly as it was.
    @Test
    void supersedingWithoutAKnownSourceAsksNothing() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "example/bar");
        context.setFillMode(FillMode.SUPERSEDE);
        Nanopub np = TestFill.fill(context);
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertNull(PublishForm.findTakenNewUri(context, np));
            q.verifyNoInteractions();
        }
    }

    // The live check while typing (#713) must look up exactly what publishing would: the same
    // identifier, formed the same way, for the same placeholders.
    @Test
    void theLiveCheckFormsTheSameIdentifierAsPublishing() throws Exception {
        TemplateContext context = filledContext(SPACE_FIELD, "my-space");
        IRI checked = context.getNewUriIri(SPACE_FIELD, "my-space");
        assertNotNull(checked);
        assertTrue(TestFill.fill(context).getAssertion().stream().anyMatch(st -> st.getSubject().equals(checked)),
                "the nanopublication the form makes has the identifier the live check looks up");
    }

    @Test
    void theLiveCheckTakesAFullIriAsTyped() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertEquals(vf.createIRI("https://example.org/records/42"),
                context.getNewUriIri(FULL_IRI_FIELD, "https://example.org/records/42"));
    }

    @Test
    void theLiveCheckHasNothingToLookUpForAnUntaggedField() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertNull(context.getNewUriIri(TOPIC_FIELD, "my-topic"));
    }

    @Test
    void theLiveCheckHasNothingToLookUpForAResourceMintedUnderTheNanopub() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertNull(context.getNewUriIri(LOCAL_FIELD, "record"));
    }

    @Test
    void theLiveCheckHasNothingToLookUpForAnEmptyField() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertNull(context.getNewUriIri(SPACE_FIELD, ""));
        assertNull(context.getNewUriIri(SPACE_FIELD, null));
    }

    @Test
    void theLiveCheckHasNothingToLookUpForAValueThatFormsNoIri() throws Exception {
        TemplateContext context = spaceTemplateContext();
        assertNull(context.getNewUriIri(SPACE_FIELD, "not an identifier"));
    }

    @Test
    void theLiveCheckReportsATakenIdentifier() throws Exception {
        TemplateContext context = spaceTemplateContext();
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.checkUriIntroduced(SPACE_PREFIX + "my-space")).thenReturn(Optional.of(true));
            assertEquals(NewUriCheck.Result.TAKEN, NewUriCheck.check(context, SPACE_FIELD, "my-space"));
        }
    }

    @Test
    void theLiveCheckReportsAFreeIdentifier() throws Exception {
        TemplateContext context = spaceTemplateContext();
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.checkUriIntroduced(anyString())).thenReturn(Optional.of(false));
            assertEquals(NewUriCheck.Result.FREE, NewUriCheck.check(context, SPACE_FIELD, "my-space"));
        }
    }

    // An unreachable query service answers neither "taken" nor "free": the field must show
    // nothing rather than a false all-clear.
    @Test
    void theLiveCheckKnowsNothingWhenTheQueryServiceCannotBeAsked() throws Exception {
        TemplateContext context = spaceTemplateContext();
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.checkUriIntroduced(anyString())).thenReturn(Optional.empty());
            assertEquals(NewUriCheck.Result.UNKNOWN, NewUriCheck.check(context, SPACE_FIELD, "my-space"));
        }
    }

    @Test
    void theLiveCheckDoesNotAskForAnUntaggedField() throws Exception {
        TemplateContext context = spaceTemplateContext();
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertEquals(NewUriCheck.Result.NOT_CHECKED, NewUriCheck.check(context, TOPIC_FIELD, "my-topic"));
            q.verifyNoInteractions();
        }
    }

    @Test
    void theLiveCheckKeepsTheSourcesIdentifierWhenSuperseding() throws Exception {
        TemplateContext context = spaceTemplateContext();
        context.setFillMode(FillMode.SUPERSEDE);
        context.setFillSource(sourceUsing(SPACE_PREFIX + "example/bar"));
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertEquals(NewUriCheck.Result.NOT_CHECKED, NewUriCheck.check(context, SPACE_FIELD, "example/bar"));
            q.verifyNoInteractions();
        }
    }

    /**
     * Builds the nanopublication a supersede would be filled from: one that already describes
     * the resource under the given identifier.
     */
    private Nanopub sourceUsing(String resourceIri) throws Exception {
        NanopubCreator creator = new NanopubCreator("https://w3id.org/np/RASourceVersion0123456789-_AbCdEfGhIjKlMnOpQ");
        IRI resource = vf.createIRI(resourceIri);
        creator.addAssertionStatement(resource, RDF.TYPE, vf.createIRI("https://w3id.org/kpxl/gen/terms/Space"));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

}
