package com.knowledgepixels.nanodash.template;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTypeTest {

    @Test
    void contextTypeContainsAssertion() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.ASSERTION));
    }

    @Test
    void contextTypeContainsProvenance() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.PROVENANCE));
    }

    @Test
    void contextTypeContainsPubinfo() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.PUBINFO));
    }

    @Test
    void contextTypeDoesNotContainInvalidValue() {
        assertFalse(EnumSet.allOf(ContextType.class).contains(null));
    }

}