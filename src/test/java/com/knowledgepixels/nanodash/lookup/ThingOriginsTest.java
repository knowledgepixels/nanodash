package com.knowledgepixels.nanodash.lookup;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.services.ApiResponseEntry;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Tests for issue #765: a thing found in the nanopub network is listed with who it comes from,
 * and the things found are listed newest first. Per issue #817, a thing that declares a
 * governing Space is listed with that Space rather than with its signer, and the note says when
 * the thing was published.
 */
class ThingOriginsTest {

    private static final String THING = "https://example.org/things/thing";
    private static final String NP = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final String VIEW = NP + "/view";
    private static final String SPACE = "https://w3id.org/spaces/knowledgepixels/x";
    private static final String DATE = "2026-10-17T09:12:00Z";
    private static final String PUBKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQD";
    private static final String PUBKEY_HASH = Utils.createSha256HexHash(PUBKEY);
    private static final IRI JANE = Utils.vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private MockedStatic<User> users;
    private MockedStatic<MaintainedResourceRepository> repositories;
    private MockedStatic<AbstractResourceWithProfile> resources;
    private MockedStatic<Utils> utils;
    private MaintainedResourceRepository repository;

    @BeforeEach
    void setUp() {
        ThingOrigins.clear();
        users = mockStatic(User.class);
        users.when(() -> User.findSingleIdForPubkeyhash(PUBKEY_HASH)).thenReturn(JANE);
        users.when(() -> User.getShortDisplayNameForPubkeyhash(JANE, PUBKEY_HASH)).thenReturn("Jane Doe");
        repository = mock(MaintainedResourceRepository.class);
        repositories = mockStatic(MaintainedResourceRepository.class);
        repositories.when(MaintainedResourceRepository::get).thenReturn(repository);
        resources = mockStatic(AbstractResourceWithProfile.class, CALLS_REAL_METHODS);
        utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        utils.close();
        resources.close();
        repositories.close();
        users.close();
        ThingOrigins.clear();
    }

    private static Space space(String label) {
        Space space = mock(Space.class);
        when(space.getLabel()).thenReturn(label);
        return space;
    }

    private static ApiResponseEntry entry(String thing, String date) {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("thing", thing);
        if (date != null) entry.add("date", date);
        return entry;
    }

    @Test
    void aThingIsShownWithTheUserWhoSignedIt() {
        ThingOrigins.record(THING, PUBKEY, null);
        assertEquals("by Jane Doe", ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aThingInTheNamespaceOfAMaintainedResourceIsShownWithItsSpace() {
        MaintainedResource resource = mock(MaintainedResource.class);
        Space space = space("Knowledge Pixels");
        when(resource.getSpace()).thenReturn(space);
        when(repository.findByNamespace("https://example.org/things/")).thenReturn(resource);
        ThingOrigins.record(THING, PUBKEY, null);
        assertEquals("by Knowledge Pixels", ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aMaintainedResourceIsShownWithItsSpace() {
        MaintainedResource resource = mock(MaintainedResource.class);
        Space space = space("Knowledge Pixels");
        when(resource.getSpace()).thenReturn(space);
        resources.when(() -> AbstractResourceWithProfile.get(THING)).thenReturn(resource);
        assertEquals("by Knowledge Pixels", ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aSpaceIsShownWithTheUserWhoSignedIt() {
        Space space = space("A space");
        resources.when(() -> AbstractResourceWithProfile.get(THING)).thenReturn(space);
        ThingOrigins.record(THING, PUBKEY, null);
        assertEquals("by Jane Doe", ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aThingSignedByAnUnknownKeyHasNoNote() {
        ThingOrigins.record(THING, "unknown key", null);
        assertNull(ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aThingWithoutARecordedSignerHasNoNote() {
        assertNull(ThingOrigins.getOriginNote(THING));
    }

    @Test
    void resultsAreListedNewestFirst() {
        List<ApiResponseEntry> entries = List.of(
                entry("old", "2024-01-05T10:00:00Z"),
                entry("undated", null),
                entry("newest", "2026-03-01T09:00:00.123+01:00"),
                entry("middle", "2025-07-20T12:00:00Z"));
        List<String> things = NanopubQueryLookup.newestFirst(entries).stream().map(e -> e.get("thing")).toList();
        assertEquals(List.of("newest", "middle", "old", "undated"), things);
    }

    /**
     * A nanopublication declaring a view, which declares a governing Space if one is given.
     */
    private static Nanopub nanopub(String governingSpace) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP);
        creator.addAssertionStatement(Utils.vf.createIRI(VIEW), RDFS.LABEL, Utils.vf.createLiteral("A view"));
        if (governingSpace != null) {
            creator.addAssertionStatement(Utils.vf.createIRI(VIEW), KPXL_TERMS.GOVERNED_BY, Utils.vf.createIRI(governingSpace));
        }
        creator.addProvenanceStatement(RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        return creator.finalizeNanopub();
    }

    @Test
    void aThingDeclaringAGoverningSpaceIsShownWithThatSpaceRatherThanItsSigner() throws Exception {
        Nanopub np = nanopub(SPACE);
        Space space = space("Knowledge Pixels");
        utils.when(() -> Utils.getNanopub(NP)).thenReturn(np);
        resources.when(() -> AbstractResourceWithProfile.get(SPACE)).thenReturn(space);
        ThingOrigins.record(VIEW, PUBKEY, null);
        ThingOrigins.resolveOrigins(List.of(VIEW));
        assertEquals("by Knowledge Pixels", ThingOrigins.getOriginNote(VIEW));
    }

    @Test
    void aThingDeclaringNoGoverningSpaceKeepsItsSigner() throws Exception {
        Nanopub np = nanopub(null);
        utils.when(() -> Utils.getNanopub(NP)).thenReturn(np);
        ThingOrigins.record(VIEW, PUBKEY, null);
        ThingOrigins.resolveOrigins(List.of(VIEW));
        assertEquals("by Jane Doe", ThingOrigins.getOriginNote(VIEW));
    }

    @Test
    void aThingThatIsNotPartOfANanopublicationIsNotLookedUp() {
        ThingOrigins.record(THING, PUBKEY, null);
        ThingOrigins.resolveOrigins(List.of(THING));
        utils.verify(() -> Utils.getNanopub(THING), never());
        assertEquals("by Jane Doe", ThingOrigins.getOriginNote(THING));
    }

    /**
     * A value a template names itself is found by no lookup query, so there is no result to read
     * a signer or a date off: they come from the thing's own nanopublication (issue #817).
     */
    @Test
    void aThingNoQueryReportedGetsItsNoteFromItsOwnNanopublication() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.generateKeyPair();
        NanopubCreator creator = new NanopubCreator("https://example.org/np/temp");
        creator.addAssertionStatement(Utils.vf.createIRI("https://example.org/np/temp/view"), RDFS.LABEL, Utils.vf.createLiteral("A view"));
        creator.addProvenanceStatement(RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        creator.addTimestamp(Date.from(Instant.parse(DATE)));
        Nanopub signed = SignNanopub.signAndTransform(creator.finalizeNanopub(), new TransformContext(
                SignatureAlgorithm.RSA, keyPair, Utils.vf.createIRI("https://example.org/signer"), false, false, false));
        String thing = signed.getUri().stringValue() + "/view";
        String pubkeyHash = Utils.createSha256HexHash(java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        users.when(() -> User.findSingleIdForPubkeyhash(pubkeyHash)).thenReturn(JANE);
        users.when(() -> User.getShortDisplayNameForPubkeyhash(JANE, pubkeyHash)).thenReturn("Jane Doe");
        utils.when(() -> Utils.getNanopub(signed.getUri().stringValue())).thenReturn(signed);

        ThingOrigins.resolveOrigins(List.of(thing));
        String note = ThingOrigins.getOriginNote(thing);
        assertTrue(note.startsWith("by Jane Doe \u00b7 "), note);
        // The timestamp is passed on as the nanopublication spells it, which is the local time
        // of whoever published it; it names the instant the test gave it.
        assertEquals(Instant.parse(DATE), OffsetDateTime.parse(note.substring(note.indexOf("\u00b7 ") + 2)).toInstant());
    }

    @Test
    void theNoteSaysWhenTheThingWasPublished() {
        ThingOrigins.record(THING, PUBKEY, DATE);
        assertEquals("by Jane Doe \u00b7 " + DATE, ThingOrigins.getOriginNote(THING));
    }

    @Test
    void aThingWithADateButNoKnownOriginIsShownWithItsDateAlone() {
        ThingOrigins.record(THING, null, DATE);
        assertEquals(DATE, ThingOrigins.getOriginNote(THING));
    }

    /**
     * The things offered are listed newest first, however their date became known: a lookup
     * reports it for the things it finds, and it is read from its own nanopublication for a
     * thing a template names.
     */
    @Test
    void thingsAreListedNewestFirst() {
        ThingOrigins.record("https://example.org/a", null, "2026-03-23T06:45:09.054+01:00");
        ThingOrigins.record("https://example.org/b", null, "2026-09-04T08:20:08Z");
        ThingOrigins.record("https://example.org/c", null, "2026-07-03T10:00:00Z");
        assertEquals(
                List.of("https://example.org/b", "https://example.org/c", "https://example.org/a"),
                ThingOrigins.newestFirst(List.of("https://example.org/a", "https://example.org/b", "https://example.org/c")));
    }

    /**
     * A thing whose date is not known -- one whose nanopublication has not been read yet -- is
     * taken for neither the newest nor the oldest: it keeps its place, at the end of the list.
     */
    @Test
    void aThingWithoutADateIsListedLast() {
        ThingOrigins.record("https://example.org/dated", null, DATE);
        assertEquals(
                List.of("https://example.org/dated", "https://example.org/undated1", "https://example.org/undated2"),
                ThingOrigins.newestFirst(List.of("https://example.org/undated1", "https://example.org/dated", "https://example.org/undated2")));
    }

    @Test
    void aDateThatCannotBeReadDoesNotDecideTheOrder() {
        ThingOrigins.record("https://example.org/odd", null, "whenever");
        ThingOrigins.record("https://example.org/dated", null, DATE);
        assertEquals(
                List.of("https://example.org/dated", "https://example.org/odd"),
                ThingOrigins.newestFirst(List.of("https://example.org/odd", "https://example.org/dated")));
    }

    @Test
    void theOriginNoteGetsItsSeparator() {
        assertEquals(" · by Jane Doe", Utils.getOriginNoteSuffix("by Jane Doe"));
        assertEquals("", Utils.getOriginNoteSuffix(null));
    }

}
