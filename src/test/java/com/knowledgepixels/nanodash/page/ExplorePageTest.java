package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NPX;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorePageTest {

    // A request that lost its id -- a stale link, or an Ajax call to a page that is no longer
    // in the page store -- used to fail as a server error, which left the status line and the
    // other lazy-loaded panels spinning forever.
    @Test
    void anExploreRequestWithoutAnIdIsRejected() {
        WicketTester tester = new WicketTester(new WicketApplication());
        tester.executeUrl("." + ExplorePage.MOUNT_PATH);
        assertEquals(400, tester.getLastResponse().getStatus(),
                "an explore URL without an 'id' parameter should be rejected, not fail as a server error");
    }

    @Test
    void membershipIsDeclaredByPartVersionAndSchemeRelations() {
        assertTrue(ExplorePage.declaresMembership(DCTERMS.IS_PART_OF));
        assertTrue(ExplorePage.declaresMembership(DCTERMS.IS_VERSION_OF));
        assertTrue(ExplorePage.declaresMembership(SKOS.IN_SCHEME));
    }

    // Hundreds of term definitions were published with dct:partOf, a term DCMI never defined,
    // and nanopublications can't be edited: a reader that only accepts dct:isPartOf would stop
    // recognising every one of them (#511).
    @Test
    void membershipIsAlsoDeclaredByTheLegacyPartOfSpelling() {
        assertTrue(ExplorePage.declaresMembership(iri("http://purl.org/dc/terms/partOf")));
    }

    @Test
    void unrelatedPredicatesDeclareNoMembership() {
        assertFalse(ExplorePage.declaresMembership(RDFS.LABEL));
        // The inverse direction says the subject is the whole, not the part.
        assertFalse(ExplorePage.declaresMembership(DCTERMS.HAS_PART));
        // A real property of another vocabulary that only looks like the legacy one.
        assertFalse(ExplorePage.declaresMembership(iri("http://purl.org/vocab/frbr/core#partOf")));
    }


    private static final String NP = "https://w3id.org/np/RAExplorePageTest0000000000000000000000001";
    // A part's IRI is minted inside the nanopublication that introduces it, which is why it
    // reaches the explore page with no context of its own.
    private static final String TALK = NP + "/a-talk";
    private static final String SPACE = "https://w3id.org/spaces/example/2026";

    /**
     * A definition of {@link #TALK} that states one thing about one subject, and introduces the
     * given resources.
     */
    private static Nanopub definition(String subject, IRI predicate, Value object, String... introduced) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP);
        creator.addAssertionStatement(iri(subject), predicate, object);
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        for (String i : introduced) creator.addPubinfoStatement(NPX.INTRODUCES, iri(i));
        return creator.finalizeNanopub();
    }

    // The point of the whole rule: a part opened by its own IRI carries no context, and its own
    // definition is where the resource it belongs to is named.
    @Test
    void aPartsOwnDefinitionNamesTheResourceItBelongsTo() throws Exception {
        assertEquals(iri(SPACE),
                ExplorePage.declaredContainerOf(definition(TALK, DCTERMS.IS_PART_OF, iri(SPACE), TALK), TALK));
    }

    @Test
    void theLegacyPartOfSpellingNamesItToo() throws Exception {
        IRI legacy = iri("http://purl.org/dc/terms/partOf");
        assertEquals(iri(SPACE),
                ExplorePage.declaredContainerOf(definition(TALK, legacy, iri(SPACE), TALK), TALK));
    }

    // A statement about something else in the same nanopublication says nothing about the term.
    @Test
    void membershipOfAnotherSubjectIsNotTheTermsMembership() throws Exception {
        Nanopub np = definition(NP + "/another-talk", DCTERMS.IS_PART_OF, iri(SPACE), TALK);
        assertNull(ExplorePage.declaredContainerOf(np, TALK));
    }

    // With several resources introduced, nothing tells which of them the statements are about.
    @Test
    void aDefinitionIntroducingSeveralResourcesDeclaresNothing() throws Exception {
        Nanopub np = definition(TALK, DCTERMS.IS_PART_OF, iri(SPACE), TALK, NP + "/a-session");
        assertNull(ExplorePage.declaredContainerOf(np, TALK));
    }

    @Test
    void aTermTheDefinitionDoesNotIntroduceDeclaresNothing() throws Exception {
        Nanopub np = definition(TALK, DCTERMS.IS_PART_OF, iri(SPACE), NP + "/a-session");
        assertNull(ExplorePage.declaredContainerOf(np, TALK));
    }

    @Test
    void aPredicateThatDeclaresNoMembershipNamesNoContainer() throws Exception {
        Nanopub np = definition(TALK, RDF.TYPE, iri("http://www.wikidata.org/entity/Q604733"), TALK);
        assertNull(ExplorePage.declaredContainerOf(np, TALK));
    }

    // Only a resource can hold parts, so a literal is not a container.
    @Test
    void aLiteralIsNoContainer() throws Exception {
        Nanopub np = definition(TALK, DCTERMS.IS_PART_OF, literal("DMKG 2026"), TALK);
        assertNull(ExplorePage.declaredContainerOf(np, TALK));
    }

    @Test
    void nothingIsDeclaredWithoutADefinition() {
        assertNull(ExplorePage.declaredContainerOf(null, TALK));
    }

}
