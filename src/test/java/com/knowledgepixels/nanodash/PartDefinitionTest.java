package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.domain.UserData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Whose nanopublication defines a part of a space ({@link PartDefinition}): the space's own
 * rule first, then the lower tiers its view displays open themselves to with
 * {@code gen:hasPartDefinitionTier} — down to {@code gen:EveryoneRole}, where the
 * nanopublication that minted the part's IRI defines it.
 */
class PartDefinitionTest {

    private static final String SPACE_ID = "https://w3id.org/spaces/example";
    private static final String PART_NP = "https://w3id.org/np/RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPART1";
    private static final String PART = PART_NP + "/statement";
    private static final IRI STATEMENT = Values.iri("https://schema.org/Statement");
    private static final IRI MEMBER = Values.iri("https://orcid.org/0000-0000-0000-0002");

    private Nanopub contribution;
    private Space space;
    private AbstractResourceWithProfile resource;
    private UserData userData;
    private ViewDisplay openDisplay;

    @BeforeEach
    void setUp() throws Exception {
        contribution = new NanopubImpl(new File("src/test/resources/np-part-contribution.trig"), RDFFormat.TRIG);
        space = mock(Space.class);
        lenient().when(space.getUsers()).thenReturn(List.of(MEMBER));
        lenient().when(space.userTier(MEMBER)).thenReturn(2);
        lenient().when(space.getPartDefinitionTierRank()).thenReturn(TierRanks.EVERYONE);
        resource = mock(AbstractResourceWithProfile.class);
        lenient().when(resource.getSpace()).thenReturn(space);
        lenient().when(resource.getViewDisplays()).thenReturn(List.of());
        lenient().when(resource.getPartLevelViewDisplays(anyString(), any())).thenReturn(List.of());
        userData = mock(UserData.class);
        lenient().when(userData.getPubkeyHashes(MEMBER, true)).thenReturn(List.of("keyMember"));
        openDisplay = mock(ViewDisplay.class);
        lenient().when(openDisplay.getPartDefinitionTier()).thenReturn(KPXL_TERMS.EVERYONE_ROLE);
        lenient().when(openDisplay.acceptsPartDefinition(TierRanks.ANYONE, TierRanks.EVERYONE)).thenReturn(true);
    }

    private static ApiResponse rows(String column, String... values) {
        ApiResponse response = new ApiResponse();
        for (String value : values) {
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add(column, value);
            response.getData().add(entry);
        }
        return response;
    }

    /**
     * Resolves the part with the network stood in for: {@code definitions} answers the
     * definitions lookup, {@code latest} the latest-version lookup of the minting nanopub,
     * and the contribution is signed with {@code signerKey}.
     */
    private PartDefinition resolve(ApiResponse definitions, ApiResponse latest, String signerKey) {
        try (MockedStatic<ApiCache> cache = mockStatic(ApiCache.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class, CALLS_REAL_METHODS);
             MockedStatic<User> user = mockStatic(User.class);
             MockedStatic<PartDefinition> hash = mockStatic(PartDefinition.class, CALLS_REAL_METHODS)) {
            user.when(User::getUserData).thenReturn(userData);
            utils.when(() -> Utils.getAsNanopub(PART_NP)).thenReturn(contribution);
            utils.when(() -> Utils.getAsNanopub("RAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPART1")).thenReturn(contribution);
            hash.when(() -> PartDefinition.pubkeyHash(any())).thenReturn(signerKey);
            cache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), eq(false))).thenAnswer(inv -> {
                QueryRef ref = inv.getArgument(0);
                if (ref.getQueryId().equals(QueryApiAccess.GET_TERM_DEFINITIONS)) return definitions;
                if (ref.getQueryId().equals(QueryApiAccess.GET_LATEST_THING_NANOPUB)) return latest;
                return new ApiResponse();
            });
            return PartDefinition.resolve(PART, SPACE_ID, resource);
        }
    }

    @Test
    void theSpacesOwnRuleComesFirst() {
        PartDefinition definition = resolve(rows("np", PART_NP), rows("latestVersion", PART_NP), "keyMember");
        assertTrue(definition.exists());
        assertEquals(PART_NP, definition.nanopubId());
        assertEquals(2, definition.signerRank(), "the member's own tier");
        assertEquals(List.of("keyMember"), definition.pubkeys());
    }

    @Test
    void withoutADisplayOpeningUpNothingElseDefinesIt() {
        PartDefinition definition = resolve(new ApiResponse(), rows("latestVersion", PART_NP), "keyStranger");
        assertFalse(definition.exists());
        assertEquals(Integer.MAX_VALUE, definition.signerRank(), "no definition filters no display");
        // The keys the space's rule looked among, never none: no keys would mean anybody.
        assertEquals(List.of("keyMember"), definition.pubkeys());
    }

    @Test
    void aDisplayOpenToEveryoneLetsTheMintingNanopubDefineThePart() {
        lenient().when(resource.getViewDisplays()).thenReturn(List.of(openDisplay));
        lenient().when(resource.getPartLevelViewDisplays(PART, Set.of(STATEMENT))).thenReturn(List.of(openDisplay));
        PartDefinition definition = resolve(new ApiResponse(), rows("latestVersion", PART_NP), "keyStranger");
        assertTrue(definition.exists());
        assertEquals(PART_NP, definition.nanopubId());
        assertEquals(TierRanks.ANYONE, definition.signerRank());
        assertEquals(List.of("keyStranger"), definition.pubkeys(), "the signer's key, so the Info view finds it too");
    }

    @Test
    void aDisplayOpenToEveryoneDoesNotDefinePartsItDoesNotApplyTo() {
        // The display is there, but not for statements: the space's other parts stay closed.
        lenient().when(resource.getViewDisplays()).thenReturn(List.of(openDisplay));
        PartDefinition definition = resolve(new ApiResponse(), rows("latestVersion", PART_NP), "keyStranger");
        assertFalse(definition.exists());
    }

    @Test
    void aRetractedMintingNanopubDefinesNothing() {
        lenient().when(resource.getViewDisplays()).thenReturn(List.of(openDisplay));
        lenient().when(resource.getPartLevelViewDisplays(PART, Set.of(STATEMENT))).thenReturn(List.of(openDisplay));
        // The latest-version lookup answers nothing for a retracted nanopub.
        PartDefinition definition = resolve(new ApiResponse(), new ApiResponse(), "keyStranger");
        assertFalse(definition.exists());
    }

    @Test
    void aSpaceOpenToEveryoneNeedsNoDisplayToOpenUp() {
        lenient().when(space.getPartDefinitionTierRank()).thenReturn(TierRanks.ANYONE);
        PartDefinition definition = resolve(new ApiResponse(), rows("latestVersion", PART_NP), "keyStranger");
        assertTrue(definition.exists());
        assertEquals(TierRanks.ANYONE, definition.signerRank());
    }

    @Test
    void anIriNotMintedInANanopubHasNoMintingNanopub() {
        lenient().when(resource.getViewDisplays()).thenReturn(List.of(openDisplay));
        lenient().when(resource.getPartLevelViewDisplays(anyString(), any())).thenReturn(List.of(openDisplay));
        try (MockedStatic<ApiCache> cache = mockStatic(ApiCache.class);
             MockedStatic<User> user = mockStatic(User.class)) {
            user.when(User::getUserData).thenReturn(userData);
            cache.when(() -> ApiCache.retrieveResponseSync(any(QueryRef.class), eq(false))).thenReturn(new ApiResponse());
            assertFalse(PartDefinition.resolve("https://doi.org/10.1234/example", SPACE_ID, resource).exists());
        }
    }

    @Test
    void signerRanks() {
        try (MockedStatic<User> user = mockStatic(User.class);
             MockedStatic<PartDefinition> hash = mockStatic(PartDefinition.class, CALLS_REAL_METHODS)) {
            user.when(User::getUserData).thenReturn(userData);
            hash.when(() -> PartDefinition.pubkeyHash(any())).thenReturn("keyMember");
            assertEquals(2, PartDefinition.signerRank(contribution, space, SPACE_ID), "a role-holder ranks by tier");
            hash.when(() -> PartDefinition.pubkeyHash(any())).thenReturn("keyStranger");
            assertEquals(TierRanks.ANYONE, PartDefinition.signerRank(contribution, space, SPACE_ID));

            String owner = "https://orcid.org/0000-0000-0000-0009";
            lenient().when(userData.getPubkeyHashes(Values.iri(owner), true)).thenReturn(List.of("keyOwner"));
            hash.when(() -> PartDefinition.pubkeyHash(any())).thenReturn("keyOwner");
            assertEquals(TierRanks.ADMIN, PartDefinition.signerRank(contribution, null, owner),
                    "on a user page the owner counts as its admin");
        }
    }

    @Test
    void partDefinitionRanks() {
        assertEquals(TierRanks.EVERYONE, TierRanks.partDefinitionRank(null), "undeclared: any role-holder");
        assertEquals(TierRanks.ANYONE, TierRanks.partDefinitionRank(KPXL_TERMS.EVERYONE_ROLE));
        assertEquals(2, TierRanks.partDefinitionRank(KPXL_TERMS.MEMBER_ROLE));
        assertEquals(TierRanks.ADMIN, TierRanks.partDefinitionRank(KPXL_TERMS.ADMIN_ROLE_TYPE));
        // Access gating is untouched: there gen:EveryoneRole needs no rank at all.
        assertEquals(TierRanks.EVERYONE, TierRanks.tierRank(KPXL_TERMS.EVERYONE_ROLE));
    }

}
