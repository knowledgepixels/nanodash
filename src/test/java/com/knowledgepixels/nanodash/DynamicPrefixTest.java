package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Token detection for the space-/namespace-dependent prefixes of issue #571
 * (docs/space-namespace-prefixes.md).
 */
class DynamicPrefixTest {

    @Test
    void spaceTokenIsDetected() {
        assertEquals(DynamicPrefix.SPACE_TOKEN, DynamicPrefix.getToken("~~SPACE~~/"));
        assertEquals(DynamicPrefix.SPACE_TOKEN, DynamicPrefix.getToken("~~SPACE~~/r/"));
    }

    @Test
    void namespaceTokenIsDetected() {
        assertEquals(DynamicPrefix.NAMESPACE_TOKEN, DynamicPrefix.getToken("~~NAMESPACE~~"));
    }

    @Test
    void staticPrefixHasNoToken() {
        assertNull(DynamicPrefix.getToken("https://example.org/"));
        assertNull(DynamicPrefix.getToken(""));
        assertNull(DynamicPrefix.getToken(null));
        // The unrelated artifact-code placeholder must not be mistaken for one of ours:
        assertNull(DynamicPrefix.getToken("https://w3id.org/np/~~ARTIFACTCODE~~/"));
    }

    @Test
    void withoutContextNothingResolves() {
        assertNull(DynamicPrefix.resolveFromContext(DynamicPrefix.SPACE_TOKEN, null));
        assertNull(DynamicPrefix.resolveFromContext(DynamicPrefix.NAMESPACE_TOKEN, ""));
    }

    @Test
    void selectionLabelsNameWhatIsPicked() {
        assertEquals("space", DynamicPrefix.getSelectionLabel(DynamicPrefix.SPACE_TOKEN));
        assertEquals("resource", DynamicPrefix.getSelectionLabel(DynamicPrefix.NAMESPACE_TOKEN));
    }

}
