package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link Preset}'s version resolution: space-governed presets float within their
 * {@code (kind, space)} pair, ungoverned ones follow their supersedes chain.
 */
class PresetTest {

    private static final String NP_GOVERNED_V1 = "https://w3id.org/np/RAPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPGV1";
    private static final String NP_GOVERNED_V2 = "https://w3id.org/np/RAPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPGV2";
    private static final String NP_UNGOVERNED_V1 = "https://w3id.org/np/RAPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPUV1";
    private static final String NP_UNGOVERNED_V2 = "https://w3id.org/np/RAPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPUV2";
    private static final String PRESET_GOVERNED_V1 = NP_GOVERNED_V1 + "/preset";
    private static final String PRESET_GOVERNED_V2 = NP_GOVERNED_V2 + "/preset";
    private static final String PRESET_UNGOVERNED_V1 = NP_UNGOVERNED_V1 + "/preset";
    private static final String PRESET_UNGOVERNED_V2 = NP_UNGOVERNED_V2 + "/preset";

    private static Nanopub load(String fileName) throws MalformedNanopubException, IOException {
        return new NanopubImpl(new File("src/test/resources/" + fileName), RDFFormat.TRIG);
    }

    private static ApiResponse governedResponse(String versionIri, String nanopubIri) {
        ApiResponse response = new ApiResponse();
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add("version", versionIri);
        entry.add("np", nanopubIri);
        response.getData().add(entry);
        return response;
    }

    @Test
    void governedPresetResolvesThroughGovernedVersionQuery() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V1)).thenReturn(load("np-preset-governed-v1.trig"));
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V2)).thenReturn(load("np-preset-governed-v2.trig"));
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false)))
                    .thenReturn(governedResponse(PRESET_GOVERNED_V2, NP_GOVERNED_V2));

            Preset resolved = Preset.get(PRESET_GOVERNED_V1);

            assertEquals(PRESET_GOVERNED_V2, resolved.getId());
            assertEquals("Second version", resolved.getLabel());
            api.verify(() -> QueryApiAccess.getLatestVersionId(anyString()), never());
        }
    }

    @Test
    void governedPresetKeepsPinWhenQueryYieldsNoVersion() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V1)).thenReturn(load("np-preset-governed-v1.trig"));
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false))).thenReturn(new ApiResponse());

            Preset resolved = Preset.get(PRESET_GOVERNED_V1);

            assertEquals(PRESET_GOVERNED_V1, resolved.getId());
            assertEquals("First version", resolved.getLabel());
        }
    }

    @Test
    void ungovernedPresetFollowsSupersedesChain() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_UNGOVERNED_V1)).thenReturn(load("np-preset-ungoverned-v1.trig"));
            utils.when(() -> Utils.getAsNanopub(NP_UNGOVERNED_V2)).thenReturn(load("np-preset-ungoverned-v2.trig"));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_UNGOVERNED_V1)).thenReturn(NP_UNGOVERNED_V2);

            Preset resolved = Preset.get(PRESET_UNGOVERNED_V1);

            assertEquals(PRESET_UNGOVERNED_V2, resolved.getId());
            cache.verify(() -> ApiCache.retrieveResponseSync(any(), eq(false)), never());
        }
    }

    /**
     * An ungoverned preset whose supersedes chain ends at a version declaring a governing
     * space has been handed over to that space: resolution carries on within the space,
     * rather than stopping at the head of the chain.
     */
    @Test
    void ungovernedPresetFollowsHandOverIntoGovernedVersions() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_UNGOVERNED_V1)).thenReturn(load("np-preset-ungoverned-v1.trig"));
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V1)).thenReturn(load("np-preset-governed-v1.trig"));
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V2)).thenReturn(load("np-preset-governed-v2.trig"));
            api.when(() -> QueryApiAccess.getLatestVersionId(NP_UNGOVERNED_V1)).thenReturn(NP_GOVERNED_V1);
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false)))
                    .thenReturn(governedResponse(PRESET_GOVERNED_V2, NP_GOVERNED_V2));

            Preset resolved = Preset.get(PRESET_UNGOVERNED_V1);

            assertEquals(PRESET_GOVERNED_V2, resolved.getId());
            assertEquals("Second version", resolved.getLabel());
        }
    }

    @Test
    void parsesGoverningSpace() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<QueryApiAccess> api = mockStatic(QueryApiAccess.class);
             MockedStatic<ApiCache> cache = mockStatic(ApiCache.class)) {
            utils.when(() -> Utils.getAsNanopub(NP_GOVERNED_V2)).thenReturn(load("np-preset-governed-v2.trig"));
            utils.when(() -> Utils.getAsNanopub(NP_UNGOVERNED_V2)).thenReturn(load("np-preset-ungoverned-v2.trig"));
            cache.when(() -> ApiCache.retrieveResponseSync(any(), eq(false))).thenReturn(new ApiResponse());
            api.when(() -> QueryApiAccess.getLatestVersionId(anyString())).thenAnswer(inv -> inv.getArgument(0));

            Preset governed = Preset.get(PRESET_GOVERNED_V2);
            Preset ungoverned = Preset.get(PRESET_UNGOVERNED_V2);

            assertEquals("https://example.org/space", governed.getGoverningSpace().stringValue());
            assertTrue(Preset.isSpaceGoverned(governed));
            assertNull(ungoverned.getGoverningSpace());
            assertFalse(Preset.isSpaceGoverned(ungoverned));
        }
    }

}
