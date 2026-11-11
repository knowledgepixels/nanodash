package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link GovernedVersions#resolve(Nanopub)} and
 * {@link GovernedVersions#forgetResolution(Nanopub)}: a definition reference follows the
 * governance a version declares, including a hand-over along a supersedes chain — from no
 * governance to a space, or from a space that doesn't maintain the kind (any more) to another.
 */
class GovernedVersionsResolveTest {

    private static final String KIND = "https://example.org/kind";
    private static final String SPACE_A = "https://example.org/space-a";
    private static final String SPACE_B = "https://example.org/space-b";

    private static final String NP_1 = np("01");
    private static final String NP_2 = np("02");
    private static final String NP_3 = np("03");

    private static String np(String suffix) {
        return "https://w3id.org/np/RA" + "H".repeat(41) + suffix;
    }

    private static String def(String npId) {
        return npId + "/def";
    }

    /**
     * A version of the definition {@link #KIND}, embedded as {@code <np>/def}, governed by the
     * given space or by none.
     */
    private static Nanopub version(String npId, String space) throws Exception {
        String trig = """
                @prefix this: <%1$s> .
                @prefix sub: <%1$s/> .
                @prefix np: <http://www.nanopub.org/nschema#> .
                @prefix dct: <http://purl.org/dc/terms/> .
                @prefix npx: <http://purl.org/nanopub/x/> .
                @prefix gen: <https://w3id.org/kpxl/gen/terms/> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix prov: <http://www.w3.org/ns/prov#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                sub:Head {
                  this: a np:Nanopublication ;
                    np:hasAssertion sub:assertion ;
                    np:hasProvenance sub:provenance ;
                    np:hasPublicationInfo sub:pubinfo .
                }
                sub:assertion {
                  sub:def rdfs:label "A definition" ; dct:isVersionOf <%2$s> %3$s.
                }
                sub:provenance {
                  sub:assertion prov:wasAttributedTo <https://orcid.org/0000-0000-0000-0000> .
                }
                sub:pubinfo {
                  this: dct:created "2026-11-11T00:00:00Z"^^xsd:dateTime ; npx:embeds sub:def .
                }
                """.formatted(npId, KIND, space == null ? "" : "; gen:governedBy <" + space + "> ");
        return new NanopubImpl(trig, RDFFormat.TRIG);
    }

    private static ApiResponse response(String npId, String mode) {
        ApiResponse response = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("version", def(npId));
        entry.add("np", npId);
        entry.add("mode", mode);
        response.getData().add(entry);
        return response;
    }

    private static ArgumentMatcher<QueryRef> governedQueryFor(String space, String pinId) {
        return ref -> ref != null && QueryApiAccess.GET_LATEST_GOVERNED_VERSION.equals(ref.getQueryId())
                && ref.getParams().get("kind").contains(KIND)
                && ref.getParams().get("space").contains(space)
                && ref.getParams().get("pin").contains(pinId);
    }

    private static ArgumentMatcher<QueryRef> latestVersionQueryFor(String npId) {
        return ref -> ref != null && QueryApiAccess.GET_LATEST_VERSION_OF_NP.equals(ref.getQueryId())
                && ref.getParams().get("np").contains(npId);
    }

    @Test
    void ungovernedPinFollowsItsSupersedesChain() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            Nanopub v1 = version(NP_1, null);
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, null));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_1)).thenReturn(NP_2);

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(v1);

            assertEquals(new GovernedVersions.Resolution(NP_2, null), resolution);
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), never());
        }
    }

    @Test
    void ungovernedPinThatIsItsOwnHeadStays() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_1)).thenReturn(NP_1);

            assertTrue(GovernedVersions.resolve(version(NP_1, null)).staysAt(NP_1));
        }
    }

    @Test
    void handOverToASpaceIsFollowedIntoThatSpacesVersions() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            // V1 is ungoverned; its key superseded it with V2, which declares space A; the
            // newest version of (kind, A) by a member of A is V3, from another key.
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_A));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_1)).thenReturn(NP_2);
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_2)), eq(false)))
                    .thenReturn(response(NP_3, "governed"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, null));

            assertEquals(new GovernedVersions.Resolution(NP_3, def(NP_3)), resolution);
        }
    }

    @Test
    void handOverToASpaceNotMaintainingTheKindStopsAtTheHead() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_A));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_1)).thenReturn(NP_2);
            // Space A doesn't maintain the kind: the query follows V2's own supersedes chain,
            // of which V2 is the head.
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_2)), eq(false)))
                    .thenReturn(response(NP_2, "supersedes"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, null));

            assertEquals(NP_2, resolution.nanopubId());
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), times(1));
        }
    }

    @Test
    void handOverToASpaceWithoutVersionsKeepsTheHandingOverVersion() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_A));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_1)).thenReturn(NP_2);
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false))).thenReturn(new ApiResponse());

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, null));

            assertEquals(new GovernedVersions.Resolution(NP_2, null), resolution);
        }
    }

    @Test
    void governedPinWithoutCandidateStaysAtThePin() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false))).thenReturn(new ApiResponse());

            assertTrue(GovernedVersions.resolve(version(NP_1, SPACE_A)).staysAt(NP_1));
            api.verify(() -> QueryApiAccess.getLatestVersionId(anyString()), never());
        }
    }

    /**
     * A space that maintains the kind keeps it: a member's version declaring another space is
     * not a hand-over, however new it is.
     */
    @Test
    void governedResultIsFinal() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_B));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_1)), eq(false)))
                    .thenReturn(response(NP_2, "governed"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, SPACE_A));

            assertEquals(new GovernedVersions.Resolution(NP_2, def(NP_2)), resolution);
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), times(1));
        }
    }

    @Test
    void spaceThatLetGoHandsOverToAnotherSpace() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            // Space A no longer maintains the kind, so the query follows V1's supersedes chain
            // to V2, which declares space B; B maintains the kind, its newest version is V3.
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_B));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_1)), eq(false)))
                    .thenReturn(response(NP_2, "supersedes"));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_B, NP_2)), eq(false)))
                    .thenReturn(response(NP_3, "governed"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, SPACE_A));

            assertEquals(new GovernedVersions.Resolution(NP_3, def(NP_3)), resolution);
        }
    }

    @Test
    void headOfTheSameUnmaintainedPairIsNotQueriedAgain() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_A));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_1)), eq(false)))
                    .thenReturn(response(NP_2, "supersedes"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, SPACE_A));

            assertEquals(new GovernedVersions.Resolution(NP_2, def(NP_2)), resolution);
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), times(1));
        }
    }

    @Test
    void headThatDropsGovernanceEndsResolution() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, null));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_1)), eq(false)))
                    .thenReturn(response(NP_2, "supersedes"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(version(NP_1, SPACE_A));

            assertEquals(NP_2, resolution.nanopubId());
            api.verify(() -> QueryApiAccess.getLatestVersionId(anyString()), never());
        }
    }

    @Test
    void handOversThatLoopBackTerminate() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            Nanopub v1 = version(NP_1, SPACE_A);
            utils.when(() -> Utils.getAsNanopub(NP_1)).thenReturn(v1);
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_B));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_A, NP_1)), eq(false)))
                    .thenReturn(response(NP_2, "supersedes"));
            cache.when(() -> ApiCache.retrieveResponseSync(argThat(governedQueryFor(SPACE_B, NP_2)), eq(false)))
                    .thenReturn(response(NP_1, "supersedes"));

            GovernedVersions.Resolution resolution = GovernedVersions.resolve(v1);

            assertEquals(NP_1, resolution.nanopubId());
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), times(2));
        }
    }

    /**
     * Forgetting retraces the steps of a resolution from what the caches hold and marks each
     * lookup outdated, the hand-over's governed-version query included, without querying.
     */
    @Test
    void forgetResolutionClearsEveryLookupAlongAHandOver() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            ApiResponse latest = new ApiResponse();
            ApiResponseEntry entry = new ApiResponseEntry();
            entry.add("latest", NP_2);
            latest.getData().add(entry);
            utils.when(() -> Utils.getAsNanopub(NP_2)).thenReturn(version(NP_2, SPACE_A));
            cache.when(() -> ApiCache.retrieveStaleResponse(argThat(latestVersionQueryFor(NP_1)))).thenReturn(latest);
            cache.when(() -> ApiCache.retrieveStaleResponse(argThat(governedQueryFor(SPACE_A, NP_2))))
                    .thenReturn(response(NP_3, "governed"));

            GovernedVersions.forgetResolution(version(NP_1, null));

            api.verify(() -> QueryApiAccess.forgetLatestVersion(NP_1));
            cache.verify(() -> ApiCache.clearCache(argThat(latestVersionQueryFor(NP_1)), eq(0L)));
            cache.verify(() -> ApiCache.clearCache(argThat(governedQueryFor(SPACE_A, NP_2)), eq(0L)));
            cache.verify(() -> ApiCache.clearCache(any(QueryRef.class), anyLong()), times(2));
            api.verify(() -> QueryApiAccess.getLatestVersionId(anyString()), never());
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), never());
        }
    }

    @Test
    void forgetResolutionWithNothingCachedClearsTheFirstLookupOnly() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            GovernedVersions.forgetResolution(version(NP_1, SPACE_A));

            cache.verify(() -> ApiCache.clearCache(argThat(governedQueryFor(SPACE_A, NP_1)), eq(0L)));
            cache.verify(() -> ApiCache.clearCache(any(QueryRef.class), anyLong()), times(1));
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), never());
        }
    }

}
