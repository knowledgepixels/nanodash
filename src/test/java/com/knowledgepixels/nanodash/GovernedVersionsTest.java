package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GovernedVersionsTest {

    /**
     * Fixtures normally belong in the nanopub test suite rather than in this repository (#620),
     * but these two stay here: they are hand-built, unsigned, and carry placeholder artifact
     * codes, because they exist to exercise Nanodash's own governance logic rather than to test
     * whether an implementation reads nanopublications correctly. A suite for validating
     * nanopublication implementations is not their home, and an unsigned nanopub with a made-up
     * code has no business in its {@code valid/} folder.
     */
    private static Nanopub load(String fileName) throws MalformedNanopubException, IOException {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
    }

    @Test
    void findsGovernedRefOfEmbeddedDefinition() throws Exception {
        GovernedVersions.GovernedRef ref = GovernedVersions.findGovernedRef(load("np-governed-definition.trig"));

        assertNotNull(ref);
        assertEquals("https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/templateKind", ref.getKind());
        assertEquals("https://w3id.org/spaces/knowledgepixels/nanoarguments", ref.getSpace());
    }

    @Test
    void findsNoGovernedRefWithoutGovernedBy() throws Exception {
        assertNull(GovernedVersions.findGovernedRef(load("np-nongoverned-definition.trig")));
    }

    @Test
    void findsNoGovernedRefForNullNanopub() {
        assertNull(GovernedVersions.findGovernedRef(null));
    }

    @Test
    void queryRefCarriesKindAndSpace() {
        String url = GovernedVersions.getQueryRef("https://example.org/kind", "https://example.org/space").getAsUrlString();

        assertTrue(url.contains(QueryApiAccess.GET_LATEST_GOVERNED_VERSION));
        assertTrue(url.contains("kind=" + Utils.urlEncode("https://example.org/kind")));
        assertTrue(url.contains("space=" + Utils.urlEncode("https://example.org/space")));
    }

    @Test
    void readsVersionAndNanopubOffResponse() {
        ApiResponse response = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("version", "https://example.org/np/RAe/template");
        entry.add("np", "https://example.org/np/RAe");
        response.getData().add(entry);

        assertEquals("https://example.org/np/RAe/template", GovernedVersions.getVersionIri(response));
        assertEquals("https://example.org/np/RAe", GovernedVersions.getVersionNanopubIri(response));
    }

    @Test
    void readsNothingOffEmptyOrMissingResponse() {
        assertNull(GovernedVersions.getVersionIri(null));
        assertNull(GovernedVersions.getVersionNanopubIri(null));
        assertNull(GovernedVersions.getVersionIri(new ApiResponse()));
        assertNull(GovernedVersions.getVersionNanopubIri(new ApiResponse()));
    }

}
