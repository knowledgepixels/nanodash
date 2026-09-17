package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.knowledgepixels.nanodash.utils.TestUtils.vf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for how {@link ResourcePartPage} finds the parent part it shows in its breadcrumb
 * (issue #718).
 */
class ResourcePartPageParentTest {

    private static final String CONTEXT_ID = "https://example.com/spaces/docs";
    private static final IRI PARAGRAPH = vf.createIRI("https://example.com/np/RAparagraph/approval");
    private static final IRI TOPIC = vf.createIRI("https://example.com/np/RAtopic/person");
    private static final IRI OTHER_TOPIC = vf.createIRI("https://example.com/np/RAtopic/space");
    private static final IRI ROUNDS = vf.createIRI("https://example.com/np/RArounds/paragraph");
    private static final IRI TRUST_TOPIC = vf.createIRI("http://purl.org/nanopub/x/approvesOf");
    private static final IRI SCHEMA_ABOUT = vf.createIRI("https://schema.org/about");
    private static final IRI SCHEMA_ABOUT_HTTP = vf.createIRI("http://schema.org/about");
    private static final IRI SCHEMA_TITLE = vf.createIRI("https://schema.org/title");

    /**
     * Builds a nanopublication whose assertion holds the given statements, each given as
     * subject, predicate and object.
     *
     * @param triples the assertion statements, three values each
     * @return the finalized nanopublication
     * @throws MalformedNanopubException        if the nanopublication is malformed
     * @throws NanopubAlreadyFinalizedException if the creator was already finalized
     */
    private static Nanopub nanopubWith(Object... triples) throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        TestUtils.fillProvenanceGraph(creator);
        TestUtils.fillPubInfoGraph(creator);
        for (int i = 0; i < triples.length; i += 3) {
            IRI subject = (IRI) triples[i];
            IRI predicate = (IRI) triples[i + 1];
            Object object = triples[i + 2];
            if (object instanceof IRI objectIri) {
                creator.addAssertionStatement(vf.createStatement(subject, predicate, objectIri));
            } else {
                creator.addAssertionStatement(vf.createStatement(subject, predicate, vf.createLiteral((String) object)));
            }
        }
        return creator.finalizeNanopub();
    }

    /**
     * Page parameters of a part page, reached from another part as
     * {@link NavigationContext#withPart} leaves them.
     *
     * @param partId      the part page's own id
     * @param parentId    the part the page was reached from, or null
     * @param parentLabel the label handed along with it, or null
     * @return the page parameters
     */
    private static PageParameters partPageParams(String partId, String parentId, String parentLabel) {
        PageParameters params = new PageParameters().set("id", partId).set(NavigationContext.CONTEXT_PARAM, CONTEXT_ID);
        if (parentId != null) params.set(NavigationContext.PART_PARAM, parentId);
        if (parentLabel != null) params.set(NavigationContext.PART_LABEL_PARAM, parentLabel);
        return params;
    }

    /**
     * A resolver that treats exactly the given definitions as parts of the context.
     *
     * @param definitions the defining nanopublication of each part, by part id
     * @return the resolver
     */
    private static Function<String, Nanopub> partsOfContext(Map<IRI, Nanopub> definitions) {
        Map<String, Nanopub> byId = new HashMap<>();
        definitions.forEach((part, np) -> byId.put(part.stringValue(), np));
        return byId::get;
    }

    /**
     * The ids of the given page references, in order.
     *
     * @param refs the page references
     * @return their {@code id} parameters
     */
    private static List<String> ids(List<NanodashPageRef> refs) {
        return refs.stream().map(ref -> ref.getParameters().get("id").toString()).toList();
    }

    /**
     * The labels of the given page references, in order.
     *
     * @param refs the page references
     * @return their labels
     */
    private static List<String> labels(List<NanodashPageRef> refs) {
        return refs.stream().map(NanodashPageRef::getLabel).toList();
    }

    @Test
    void incomingPartIsFollowedUpItsDeclaredParents() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub rounds = nanopubWith(ROUNDS, SCHEMA_ABOUT_HTTP, TRUST_TOPIC, ROUNDS, SCHEMA_TITLE, "Rounds");
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_ABOUT_HTTP, TOPIC, PARAGRAPH, SCHEMA_TITLE, "Approval");
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        Nanopub trust = nanopubWith(TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        PageParameters params = partPageParams(ROUNDS.stringValue(), PARAGRAPH.stringValue(), "Approval");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(params, rounds, ROUNDS.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(PARAGRAPH, approval, TOPIC, person, TRUST_TOPIC, trust)));
        assertEquals(List.of(TOPIC.stringValue(), PARAGRAPH.stringValue()), ids(ancestors));
        assertEquals(List.of("Person", "Approval"), labels(ancestors));
        for (NanodashPageRef ref : ancestors) {
            assertSame(ResourcePartPage.class, ref.getPageClass());
            assertEquals(CONTEXT_ID, ref.getParameters().get(NavigationContext.CONTEXT_PARAM).toString());
        }
    }

    @Test
    void topLevelPartReachedFromAnotherPartGetsNoAncestors() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_ABOUT_HTTP, TOPIC, PARAGRAPH, SCHEMA_TITLE, "Approval");
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        Nanopub trust = nanopubWith(TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        PageParameters params = partPageParams(TRUST_TOPIC.stringValue(), PARAGRAPH.stringValue(), "Approval");
        assertTrue(ResourcePartPage.getAncestorRefs(params, trust, TRUST_TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(PARAGRAPH, approval, TOPIC, person, TRUST_TOPIC, trust))).isEmpty());
    }

    @Test
    void partWhoseDeclaredParentsAreNoPartsOfTheContextIsTopLevel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub trust = nanopubWith(TRUST_TOPIC, SCHEMA_ABOUT, OTHER_TOPIC, TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_TITLE, "Approval");
        PageParameters params = partPageParams(TRUST_TOPIC.stringValue(), PARAGRAPH.stringValue(), "Approval");
        assertTrue(ResourcePartPage.getAncestorRefs(params, trust, TRUST_TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(PARAGRAPH, approval))).isEmpty());
    }

    @Test
    void withoutIncomingPartTheChainStartsAtTheDeclaredParent() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_ABOUT, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        PageParameters params = partPageParams(PARAGRAPH.stringValue(), null, null);
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(params, approval, PARAGRAPH.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TOPIC, person)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ancestors));
        assertEquals(List.of("Person"), labels(ancestors));
    }

    @Test
    void declaredParentsThatAreNoPartsOfTheContextAreSkipped() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(
                PARAGRAPH, SCHEMA_ABOUT, OTHER_TOPIC,
                PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(partPageParams(PARAGRAPH.stringValue(), null, null),
                approval, PARAGRAPH.stringValue(), CONTEXT_ID, partsOfContext(Map.of(TOPIC, person)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ancestors));
    }

    @Test
    void incomingPartWithoutLabelTakesItsDeclaredLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_TITLE, "Approval");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(partPageParams(ROUNDS.stringValue(), PARAGRAPH.stringValue(), null),
                null, ROUNDS.stringValue(), CONTEXT_ID, partsOfContext(Map.of(PARAGRAPH, approval)));
        assertEquals(List.of("Approval"), labels(ancestors));
    }

    @Test
    void unresolvableIncomingPartIsShownWithItsShortName() {
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(partPageParams(ROUNDS.stringValue(), PARAGRAPH.stringValue(), null),
                null, ROUNDS.stringValue(), CONTEXT_ID, partsOfContext(Map.of()));
        assertEquals(List.of(PARAGRAPH.stringValue()), ids(ancestors));
        assertEquals(List.of(Utils.getShortNameFromURI(PARAGRAPH.stringValue())), labels(ancestors));
    }

    @Test
    void noAncestorsWithoutIncomingOrDeclaredParent() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        assertTrue(ResourcePartPage.getAncestorRefs(partPageParams(TOPIC.stringValue(), null, null), person, TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of())).isEmpty());
        assertTrue(ResourcePartPage.getAncestorRefs(partPageParams(TOPIC.stringValue(), null, null), null, TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of())).isEmpty());
    }

    @Test
    void incomingPartThatIsThePageItselfOrItsContextIsIgnored() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_ABOUT, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        Function<String, Nanopub> parts = partsOfContext(Map.of(TOPIC, person, PARAGRAPH, approval));
        assertEquals(List.of(TOPIC.stringValue()), ids(ResourcePartPage.getAncestorRefs(
                partPageParams(PARAGRAPH.stringValue(), PARAGRAPH.stringValue(), "Approval"), approval, PARAGRAPH.stringValue(), CONTEXT_ID, parts)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ResourcePartPage.getAncestorRefs(
                partPageParams(PARAGRAPH.stringValue(), CONTEXT_ID, "Docs"), approval, PARAGRAPH.stringValue(), CONTEXT_ID, parts)));
    }

    @Test
    void cyclicDeclaredParentsStopAtTheFirstRepeat() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, SCHEMA_ABOUT, TOPIC);
        Nanopub person = nanopubWith(TOPIC, SCHEMA_ABOUT, PARAGRAPH, TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(partPageParams(PARAGRAPH.stringValue(), null, null),
                approval, PARAGRAPH.stringValue(), CONTEXT_ID, partsOfContext(Map.of(TOPIC, person, PARAGRAPH, approval)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ancestors));
    }

    @Test
    void longChainsAreCutAtTheMaximum() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Map<IRI, Nanopub> chain = new HashMap<>();
        IRI child = PARAGRAPH;
        Nanopub childDefinition = null;
        for (int level = 0; level <= ResourcePartPage.MAX_ANCESTORS + 2; level++) {
            IRI parent = vf.createIRI("https://example.com/np/RAlevel/" + level);
            Nanopub definition = nanopubWith(child, SCHEMA_ABOUT, parent);
            if (child.equals(PARAGRAPH)) {
                childDefinition = definition;
            } else {
                chain.put(child, definition);
            }
            child = parent;
        }
        chain.put(child, nanopubWith(child, RDFS.LABEL, "Top"));
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(partPageParams(PARAGRAPH.stringValue(), null, null),
                childDefinition, PARAGRAPH.stringValue(), CONTEXT_ID, partsOfContext(chain));
        assertEquals(ResourcePartPage.MAX_ANCESTORS, ancestors.size());
        assertEquals("https://example.com/np/RAlevel/0", ids(ancestors).get(ancestors.size() - 1));
    }

    @Test
    void declaredParentCandidatesKeepTheStatedOrder() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(
                PARAGRAPH, SCHEMA_ABOUT, TOPIC,
                PARAGRAPH, DCTERMS.IS_PART_OF, OTHER_TOPIC);
        assertEquals(List.of(TOPIC.stringValue(), OTHER_TOPIC.stringValue()),
                ResourcePartPage.getDeclaredParentCandidates(np, PARAGRAPH.stringValue(), CONTEXT_ID));
    }

    @Test
    void declaredParentCandidatesAcceptBothSchemaOrgSpellings() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(PARAGRAPH, SCHEMA_ABOUT_HTTP, TOPIC);
        assertEquals(List.of(TOPIC.stringValue()),
                ResourcePartPage.getDeclaredParentCandidates(np, PARAGRAPH.stringValue(), CONTEXT_ID));
    }

    @Test
    void declaredParentCandidatesLeaveOutOtherSubjectsLiteralsSelfAndContext() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(
                TOPIC, SCHEMA_ABOUT, OTHER_TOPIC,
                PARAGRAPH, SCHEMA_ABOUT, "Person",
                PARAGRAPH, SCHEMA_ABOUT, PARAGRAPH,
                PARAGRAPH, DCTERMS.IS_PART_OF, vf.createIRI(CONTEXT_ID),
                PARAGRAPH, RDFS.SEEALSO, TOPIC);
        assertTrue(ResourcePartPage.getDeclaredParentCandidates(np, PARAGRAPH.stringValue(), CONTEXT_ID).isEmpty());
    }

    @Test
    void declaredLabelPrefersRdfsLabelOverSchemaTitle() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(
                TOPIC, SCHEMA_TITLE, "Person (title)",
                TOPIC, RDFS.LABEL, "Person");
        assertEquals("Person", ResourcePartPage.getDeclaredLabel(np, TOPIC.stringValue()));
    }

    @Test
    void declaredLabelFallsBackToANonBlankSchemaTitle() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(
                TOPIC, SCHEMA_TITLE, " ",
                TOPIC, SCHEMA_TITLE, "Person");
        assertEquals("Person", ResourcePartPage.getDeclaredLabel(np, TOPIC.stringValue()));
    }

    @Test
    void noDeclaredLabelForAnotherSubject() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(OTHER_TOPIC, RDFS.LABEL, "Space");
        assertNull(ResourcePartPage.getDeclaredLabel(np, TOPIC.stringValue()));
    }

}
