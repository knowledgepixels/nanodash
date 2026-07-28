package com.knowledgepixels.nanodash.component;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;

import com.knowledgepixels.nanodash.QueryApiAccess;

/**
 * Tests for the checks that prevent superseding or overriding a nanopublication
 * that is not the latest version anymore.
 */
class PublishFormSourceTest {

    private static final String OLD_NP = "https://w3id.org/np/RAoldoldoldoldoldoldoldoldoldoldoldoldoldoldold";
    private static final String NEW_NP = "https://w3id.org/np/RAnewnewnewnewnewnewnewnewnewnewnewnewnewnewnew";

    private static PageParameters params(String... keyValuePairs) {
        PageParameters p = new PageParameters();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            p.set(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return p;
    }

    @ParameterizedTest
    @ValueSource(strings = {"supersede", "supersede-a", "override", "override-a", "fill"})
    void sourceIdIsFoundForEverySourceParam(String key) {
        assertEquals(OLD_NP, PublishForm.getSupersededOrOverriddenNanopubId(params(key, OLD_NP)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"use", "use-a", "derive", "derive-a", "improve", "fill-all"})
    void sourceIdIsNullForNonSupersedingParams(String key) {
        assertNull(PublishForm.getSupersededOrOverriddenNanopubId(params(key, OLD_NP)));
    }

    @Test
    void sourceIdIsNullWithoutAnyParams() {
        assertNull(PublishForm.getSupersededOrOverriddenNanopubId(new PageParameters()));
    }

    @Test
    void emptySourceParamIsIgnored() {
        assertNull(PublishForm.getSupersededOrOverriddenNanopubId(params("supersede", "")));
    }

    @Test
    void supersedeTakesPrecedenceOverOverride() {
        assertEquals(OLD_NP, PublishForm.getSupersededOrOverriddenNanopubId(params("override", NEW_NP, "supersede", OLD_NP)));
    }

    // --- isSourceOutdated ---
    @Test
    void sourceIsOutdatedWhenNewerVersionExists() {
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.getLatestVersionId(OLD_NP)).thenReturn(NEW_NP);
            assertTrue(PublishForm.isSourceOutdated(params("supersede", OLD_NP)));
            assertTrue(PublishForm.isSourceOutdated(params("override", OLD_NP)));
            assertTrue(PublishForm.isSourceOutdated(params("fill", OLD_NP)));
        }
    }

    @Test
    void sourceIsNotOutdatedWhenItIsTheLatestVersion() {
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            q.when(() -> QueryApiAccess.getLatestVersionId(NEW_NP)).thenReturn(NEW_NP);
            assertFalse(PublishForm.isSourceOutdated(params("supersede", NEW_NP)));
        }
    }

    @Test
    void sourceIsNotOutdatedForNonSupersedingParams() {
        try (MockedStatic<QueryApiAccess> q = mockStatic(QueryApiAccess.class)) {
            assertFalse(PublishForm.isSourceOutdated(params("derive", OLD_NP)));
            assertFalse(PublishForm.isSourceOutdated(new PageParameters()));
            // The latest version should not even be looked up if there is nothing to supersede:
            q.verifyNoInteractions();
        }
    }

    // --- withSourceNanopub ---
    @ParameterizedTest
    @ValueSource(strings = {"supersede", "supersede-a", "override", "override-a", "fill"})
    void sourceParamIsReplacedByLatestVersion(String key) {
        PageParameters result = PublishForm.withSourceNanopub(params(key, OLD_NP), NEW_NP);
        assertEquals(NEW_NP, result.get(key).toString());
    }

    @Test
    void otherParamsArePreserved() {
        PageParameters result = PublishForm.withSourceNanopub(params("supersede", OLD_NP, "template", "RAtemplate"), NEW_NP);
        assertEquals(NEW_NP, result.get("supersede").toString());
        assertEquals("RAtemplate", result.get("template").toString());
    }

    @Test
    void originalParamsAreNotModified() {
        PageParameters original = params("supersede", OLD_NP);
        PublishForm.withSourceNanopub(original, NEW_NP);
        assertEquals(OLD_NP, original.get("supersede").toString());
    }

    @Test
    void nothingIsReplacedWithoutSourceParams() {
        PageParameters result = PublishForm.withSourceNanopub(params("derive", OLD_NP), NEW_NP);
        assertEquals(OLD_NP, result.get("derive").toString());
    }

}
