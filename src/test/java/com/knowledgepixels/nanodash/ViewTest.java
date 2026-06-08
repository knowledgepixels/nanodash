package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewTest {

    @Test
    void parseMappingLiteralSplitsOnWhitespace() {
        // single mapping
        assertEquals(List.of("np:nanopubToBeRetracted"),
                View.parseMappingLiteral("np:nanopubToBeRetracted"));
        // multiple mappings share one literal (templates can't repeat the statement)
        assertEquals(List.of("derive_target:@derive-a", "local_pubkey:public-key__.1"),
                View.parseMappingLiteral("derive_target:@derive-a local_pubkey:public-key__.1"));
        // irregular whitespace is tolerated
        assertEquals(List.of("a:foo", "b:@bar"),
                View.parseMappingLiteral("  a:foo   b:@bar  "));
    }

    @Test
    void parseMappingLiteralHandlesVoidAndEmpty() {
        assertEquals(List.of(), View.parseMappingLiteral("void"));
        assertEquals(List.of(), View.parseMappingLiteral(""));
        assertEquals(List.of(), View.parseMappingLiteral("   "));
        assertEquals(List.of(), View.parseMappingLiteral(null));
        // a stray "void" token among real mappings is dropped
        assertEquals(List.of("a:foo"), View.parseMappingLiteral("a:foo void"));
    }

}
