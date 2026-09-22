package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.utils.TestUtils;
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
 * Tests for the ancestors {@link ResourcePartPage} shows in its breadcrumb (issue #718):
 * the hierarchy the parts declare, however deep it goes, and never the path the reader
 * took to reach the page.
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
    void theChainFollowsTheDeclaredHierarchy() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub rounds = nanopubWith(ROUNDS, DCTERMS.IS_PART_OF, TRUST_TOPIC, ROUNDS, SCHEMA_TITLE, "Rounds");
        Nanopub trust = nanopubWith(TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(rounds, ROUNDS.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TRUST_TOPIC, trust)));
        assertEquals(List.of(TRUST_TOPIC.stringValue()), ids(ancestors));
        assertEquals(List.of("Trust and approval"), labels(ancestors));
        assertSame(ResourcePartPage.class, ancestors.get(0).getPageClass());
        assertEquals(CONTEXT_ID, ancestors.get(0).getParameters().get(NavigationContext.CONTEXT_PARAM).toString());
    }

    @Test
    void thePartTheReaderCameFromIsNotAnAncestor() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // Opening Rounds from the Approval paragraph must read the same as opening it directly:
        // Approval is a sibling in the hierarchy, not a parent (issue #718 review).
        Nanopub rounds = nanopubWith(ROUNDS, DCTERMS.IS_PART_OF, TRUST_TOPIC);
        Nanopub approval = nanopubWith(PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC, PARAGRAPH, SCHEMA_TITLE, "Approval");
        Nanopub trust = nanopubWith(TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(rounds, ROUNDS.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TRUST_TOPIC, trust, PARAGRAPH, approval, TOPIC, person)));
        assertEquals(List.of(TRUST_TOPIC.stringValue()), ids(ancestors));
    }

    @Test
    void partsOfPartsOfPartsAreAllShown() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        IRI section = vf.createIRI("https://example.com/np/RAsection/approval-section");
        Nanopub rounds = nanopubWith(ROUNDS, DCTERMS.IS_PART_OF, section);
        Nanopub sectionNp = nanopubWith(section, DCTERMS.IS_PART_OF, TRUST_TOPIC, section, RDFS.LABEL, "Approval rounds");
        Nanopub trust = nanopubWith(TRUST_TOPIC, DCTERMS.IS_PART_OF, TOPIC, TRUST_TOPIC, RDFS.LABEL, "Trust and approval");
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(rounds, ROUNDS.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(section, sectionNp, TRUST_TOPIC, trust, TOPIC, person)));
        assertEquals(List.of(TOPIC.stringValue(), TRUST_TOPIC.stringValue(), section.stringValue()), ids(ancestors));
        assertEquals(List.of("Person", "Trust and approval", "Approval rounds"), labels(ancestors));
    }

    @Test
    void declaredParentsThatAreNoPartsOfTheContextAreSkipped() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(
                PARAGRAPH, DCTERMS.IS_PART_OF, OTHER_TOPIC,
                PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(approval, PARAGRAPH.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TOPIC, person)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ancestors));
    }

    @Test
    void noAncestorsForATopLevelPart() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        assertTrue(ResourcePartPage.getAncestorRefs(person, TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of())).isEmpty());
        assertTrue(ResourcePartPage.getAncestorRefs(null, TOPIC.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of())).isEmpty());
    }

    @Test
    void anAncestorWithoutALabelFallsBackToItsShortName() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.SEEALSO, OTHER_TOPIC);
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(approval, PARAGRAPH.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TOPIC, person)));
        assertEquals(List.of(Utils.getShortNameFromURI(TOPIC.stringValue())), labels(ancestors));
    }

    @Test
    void cyclicDeclaredParentsStopAtTheFirstRepeat() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub approval = nanopubWith(PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC);
        Nanopub person = nanopubWith(TOPIC, DCTERMS.IS_PART_OF, PARAGRAPH, TOPIC, RDFS.LABEL, "Person");
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(approval, PARAGRAPH.stringValue(), CONTEXT_ID,
                partsOfContext(Map.of(TOPIC, person, PARAGRAPH, approval)));
        assertEquals(List.of(TOPIC.stringValue()), ids(ancestors));
    }

    @Test
    void longChainsAreCutAtTheMaximum() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Map<IRI, Nanopub> chain = new HashMap<>();
        IRI child = PARAGRAPH;
        Nanopub childDefinition = null;
        for (int level = 0; level <= ResourcePartPage.MAX_ANCESTORS + 2; level++) {
            IRI parent = vf.createIRI("https://example.com/np/RAlevel/" + level);
            Nanopub definition = nanopubWith(child, DCTERMS.IS_PART_OF, parent);
            if (child.equals(PARAGRAPH)) {
                childDefinition = definition;
            } else {
                chain.put(child, definition);
            }
            child = parent;
        }
        chain.put(child, nanopubWith(child, RDFS.LABEL, "Top"));
        List<NanodashPageRef> ancestors = ResourcePartPage.getAncestorRefs(childDefinition, PARAGRAPH.stringValue(), CONTEXT_ID,
                partsOfContext(chain));
        assertEquals(ResourcePartPage.MAX_ANCESTORS, ancestors.size());
        assertEquals("https://example.com/np/RAlevel/0", ids(ancestors).get(ancestors.size() - 1));
    }

    @Test
    void schemaAboutIsUsedOnlyWhenNoPartOfIsDeclared() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        // Parts published before the hierarchy was stated with dct:isPartOf still read correctly.
        Nanopub both = nanopubWith(PARAGRAPH, SCHEMA_ABOUT, OTHER_TOPIC, PARAGRAPH, DCTERMS.IS_PART_OF, TOPIC);
        assertEquals(List.of(TOPIC.stringValue(), OTHER_TOPIC.stringValue()),
                ResourcePartPage.getDeclaredParentCandidates(both, PARAGRAPH.stringValue(), CONTEXT_ID));
        Nanopub aboutOnly = nanopubWith(PARAGRAPH, SCHEMA_ABOUT_HTTP, TOPIC);
        Nanopub person = nanopubWith(TOPIC, RDFS.LABEL, "Person");
        assertEquals(List.of(TOPIC.stringValue()), ids(ResourcePartPage.getAncestorRefs(
                aboutOnly, PARAGRAPH.stringValue(), CONTEXT_ID, partsOfContext(Map.of(TOPIC, person)))));
    }

    @Test
    void declaredParentCandidatesPutPartOfFirstAndKeepTheStatedOrderWithin() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = nanopubWith(
                PARAGRAPH, SCHEMA_ABOUT, TOPIC,
                PARAGRAPH, DCTERMS.IS_PART_OF, OTHER_TOPIC,
                PARAGRAPH, DCTERMS.IS_PART_OF, TRUST_TOPIC);
        assertEquals(List.of(OTHER_TOPIC.stringValue(), TRUST_TOPIC.stringValue(), TOPIC.stringValue()),
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
