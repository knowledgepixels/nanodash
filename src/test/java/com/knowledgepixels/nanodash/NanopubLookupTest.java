package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NanopubLookupTest {

    // Well-formed nanopublication id: "RA" plus exactly 43 artifact-code characters.
    private static final String VALID_ID = "https://w3id.org/np/RAFl3dEaZocvP1BAyakcX_cXhFiRQ6uO8K6qMA_3p3j_t";
    // Same id with the last character lost, as happens with a mangled copy-paste.
    private static final String TRUNCATED_ID = VALID_ID.substring(0, VALID_ID.length() - 1);

    @Test
    void wellFormedIdIsRecognized() {
        assertTrue(NanopubLookup.isPotentialNanopubId(VALID_ID));
        assertFalse(NanopubLookup.looksLikeMalformedNanopubId(VALID_ID));
    }

    @Test
    void truncatedIdIsRecognizedAsMalformedNanopubId() {
        assertFalse(NanopubLookup.isPotentialNanopubId(TRUNCATED_ID));
        assertTrue(NanopubLookup.looksLikeMalformedNanopubId(TRUNCATED_ID));
    }

    @Test
    void idOfAnyWrongLengthIsRecognizedAsMalformedNanopubId() {
        for (String wrongLength : new String[]{VALID_ID.substring(0, VALID_ID.length() - 18), VALID_ID + "abc"}) {
            assertFalse(NanopubLookup.isPotentialNanopubId(wrongLength), wrongLength);
            assertTrue(NanopubLookup.looksLikeMalformedNanopubId(wrongLength), wrongLength);
        }
    }

    // Artifact codes of other trusty URI modules (files, for one) denote things that are
    // not nanopublications, whole or broken, so neither predicate claims them.
    @Test
    void artifactCodeOfAnotherModuleIsNoNanopubId() {
        String fileModuleId = VALID_ID.replaceFirst("/RA", "/FA");
        assertFalse(NanopubLookup.isPotentialNanopubId(fileModuleId));
        assertFalse(NanopubLookup.looksLikeMalformedNanopubId(fileModuleId));
        assertFalse(NanopubLookup.looksLikeMalformedNanopubId(TRUNCATED_ID.replaceFirst("/RA", "/FA")));
    }

    @Test
    void plainTermIriIsNeitherValidNorMalformedNanopubId() {
        for (String termIri : new String[]{"http://example.com/my-term", "https://w3id.org/np/RAFl3dEa/term", "https://orcid.org/0000-0000-0000-0000"}) {
            assertFalse(NanopubLookup.isPotentialNanopubId(termIri), termIri);
            assertFalse(NanopubLookup.looksLikeMalformedNanopubId(termIri), termIri);
        }
    }

    @Test
    void missingIdIsNeitherValidNorMalformedNanopubId() {
        assertFalse(NanopubLookup.isPotentialNanopubId(null));
        assertFalse(NanopubLookup.isPotentialNanopubId(" "));
        assertFalse(NanopubLookup.looksLikeMalformedNanopubId(null));
        assertFalse(NanopubLookup.looksLikeMalformedNanopubId(" "));
    }

    @Test
    void missingIdGivesInvalidIdWithMessage() {
        NanopubLookup lookup = NanopubLookup.lookUp(null);
        assertEquals(NanopubLookup.Status.INVALID_ID, lookup.getStatus());
        assertFalse(lookup.isFound());
        assertNotNull(lookup.getErrorMessage());
    }

    @Test
    void malformedIdGivesInvalidIdWithoutRetrieval() {
        NanopubLookup lookup = NanopubLookup.lookUp(TRUNCATED_ID, 10_000, id -> {
            throw new AssertionError("must not try to retrieve a malformed id");
        });
        assertEquals(NanopubLookup.Status.INVALID_ID, lookup.getStatus());
        assertTrue(lookup.getErrorMessage().contains(TRUNCATED_ID));
        assertEquals(TRUNCATED_ID, lookup.getId());
    }

    @Test
    void retrievedNanopubGivesFound() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub(VALID_ID);
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_ID, 10_000, id -> np);
        assertEquals(NanopubLookup.Status.FOUND, lookup.getStatus());
        assertTrue(lookup.isFound());
        assertSame(np, lookup.getNanopub());
        assertNull(lookup.getErrorMessage());
    }

    @Test
    void unavailableNanopubGivesNotFound() {
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_ID, 10_000, id -> null);
        assertEquals(NanopubLookup.Status.NOT_FOUND, lookup.getStatus());
        assertFalse(lookup.isFound());
        assertNull(lookup.getNanopub());
        assertEquals(VALID_ID, lookup.getId());
    }

    @Test
    void slowRetrievalGivesTimeout() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        try {
            NanopubLookup lookup = NanopubLookup.lookUp(VALID_ID, 50, id -> {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            assertEquals(NanopubLookup.Status.TIMEOUT, lookup.getStatus());
            assertFalse(lookup.isFound());
        } finally {
            release.countDown();
        }
    }

    @Test
    void nonNanopubTrustyUriGivesInvalidId() {
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_ID, 10_000, id -> {
            throw new IllegalArgumentException("Not a trusty URI of type RA");
        });
        assertEquals(NanopubLookup.Status.INVALID_ID, lookup.getStatus());
        assertTrue(lookup.getErrorMessage().contains("Not a trusty URI of type RA"));
    }

    @Test
    void retrievalFailureGivesNotFound() {
        NanopubLookup lookup = NanopubLookup.lookUp(VALID_ID, 10_000, id -> {
            throw new IllegalStateException("network is down");
        });
        assertEquals(NanopubLookup.Status.NOT_FOUND, lookup.getStatus());
    }

    @Test
    void foundWrapsAvailableNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub np = TestUtils.createNanopub(VALID_ID);
        NanopubLookup lookup = NanopubLookup.found(np);
        assertTrue(lookup.isFound());
        assertSame(np, lookup.getNanopub());
        assertEquals(VALID_ID, lookup.getId());
    }

}
