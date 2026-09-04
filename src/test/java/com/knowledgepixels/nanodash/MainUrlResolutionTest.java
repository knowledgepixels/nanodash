package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolution of the main registry/query URLs from NANODASH_MAIN_* and the library instance list.
 */
class MainUrlResolutionTest {

    private static final String VAR = "NANODASH_MAIN_REGISTRY";
    private static final String LIB_VAR = "NANOPUB_REGISTRY_INSTANCES";
    private static final String DEFAULT = "https://registry.knowledgepixels.com/";
    private static final List<String> PUBLIC_INSTANCES =
            List.of("https://registry.knowledgepixels.com/", "https://registry.petapico.org/");

    private static String resolve(String envValue, List<String> instances) {
        return Utils.resolveMainUrl(VAR, envValue, instances, LIB_VAR, DEFAULT);
    }

    @Test
    void configuredUrlOutsideTheLibraryListIsStillUsed() {
        // Issue #680: a private service named by the operator must not be replaced by a public one.
        assertEquals("https://registry.example.org/", resolve("https://registry.example.org/", PUBLIC_INSTANCES));
    }

    @Test
    void configuredUrlWinsWithoutATrailingSlash() {
        assertEquals("https://registry.example.org/", resolve("https://registry.example.org", PUBLIC_INSTANCES));
    }

    @Test
    void configuredUrlIsUsedWhenTheLibraryListIsEmpty() {
        assertEquals("https://registry.example.org/", resolve("https://registry.example.org/", Collections.emptyList()));
    }

    @Test
    void configuredUrlInTheLibraryListIsUsed() {
        assertEquals("https://registry.petapico.org/", resolve("https://registry.petapico.org", PUBLIC_INSTANCES));
    }

    @Test
    void firstLibraryInstanceIsUsedWhenNothingIsConfigured() {
        assertEquals("https://registry.knowledgepixels.com/", resolve(null, PUBLIC_INSTANCES));
    }

    @Test
    void builtInDefaultIsUsedWhenNothingIsConfiguredAndNoInstancesAreKnown() {
        assertEquals(DEFAULT, resolve(null, Collections.emptyList()));
    }
}
