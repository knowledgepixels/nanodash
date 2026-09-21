package com.knowledgepixels.nanodash.page;

import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.jupiter.api.Test;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorePageTest {

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

}
