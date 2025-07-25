package com.knowledgepixels.nanodash.template;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTypeTest {

    @Test
    void contextType_containsAssertion() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.ASSERTION));
    }

    @Test
    void contextType_containsProvenance() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.PROVENANCE));
    }

    @Test
    void contextType_containsPubinfo() {
        assertTrue(EnumSet.allOf(ContextType.class).contains(ContextType.PUBINFO));
    }

    @Test
    void contextType_doesNotContainInvalidValue() {
        assertFalse(EnumSet.allOf(ContextType.class).contains(null));
    }

}