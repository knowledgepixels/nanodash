package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.TitleBar.CrumbPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TitleBarTest {

    private static NanodashPageRef[] refs(String... labels) {
        NanodashPageRef[] r = new NanodashPageRef[labels.length];
        for (int i = 0; i < labels.length; i++) r[i] = new NanodashPageRef(labels[i]);
        return r;
    }

    private static List<String> crumbLabels(String... labels) {
        return TitleBar.buildCrumbParts(refs(labels)).stream().map(CrumbPart::label).toList();
    }

    @Test
    void stripsParentSuffixChildPrefixOverlap() {
        assertEquals("Jkl", TitleBar.stripParentOverlap("Abc Def Ghi", "Ghi Jkl"));
    }

    @Test
    void breadcrumbStripsOverlap() {
        assertEquals(List.of("Abc Def Ghi", "Jkl"), crumbLabels("Abc Def Ghi", "Ghi Jkl"));
    }

    @Test
    void stripsLongestMultiWordOverlap() {
        assertEquals("Qux", TitleBar.stripParentOverlap("Foo Bar Baz", "Bar Baz Qux"));
    }

    @Test
    void overlapMatchIsCaseInsensitive() {
        assertEquals("Jkl", TitleBar.stripParentOverlap("Abc Def Ghi", "ghi Jkl"));
    }

    @Test
    void noOverlapLeavesChildUnchanged() {
        assertEquals("Mno Pqr", TitleBar.stripParentOverlap("Abc Def Ghi", "Mno Pqr"));
    }

    @Test
    void neverStripsWholeChildAway() {
        // The child's only word equals the parent's last word — keep it rather than
        // produce an empty crumb.
        assertEquals("Ghi", TitleBar.stripParentOverlap("Abc Def Ghi", "Ghi"));
    }

    @Test
    void handlesNullAndEmpty() {
        assertEquals("Ghi Jkl", TitleBar.stripParentOverlap(null, "Ghi Jkl"));
        assertEquals("Ghi Jkl", TitleBar.stripParentOverlap("", "Ghi Jkl"));
        assertNull(TitleBar.stripParentOverlap("Abc", null));
    }

    @Test
    void stripsSuffixOverlapWithPluralVariation() {
        // Parent's trailing "Office Hours" overlaps the child's leading "Office
        // Hour" (singular/plural), and the matching words are a suffix of the
        // parent rather than its whole label.
        assertEquals("24 June 2026",
                TitleBar.stripParentOverlap("Knowledge Pixels Incubator Office Hours", "Office Hour 24 June 2026"));
    }

    @Test
    void breadcrumbStripsSuffixOverlapWithPlural() {
        assertEquals(
                List.of("Knowledge Pixels", "Incubator", "Office Hours", "24 June 2026"),
                crumbLabels(
                        "Knowledge Pixels",
                        "Knowledge Pixels Incubator",
                        "Knowledge Pixels Incubator Office Hours",
                        "Office Hour 24 June 2026"));
    }

    @Test
    void breadcrumbStripsAncestorPrefixedLeafToo() {
        // The same leaf may instead carry the fully ancestor-prefixed label; it
        // must reduce to the same crumb via the character-prefix transform.
        assertEquals(
                List.of("Knowledge Pixels", "Incubator", "Office Hours", "24 June 2026"),
                crumbLabels(
                        "Knowledge Pixels",
                        "Knowledge Pixels Incubator",
                        "Knowledge Pixels Incubator Office Hours",
                        "Knowledge Pixels Incubator Office Hour 24 June 2026"));
    }

    @Test
    void existingParentPrefixStrippingStillApplies() {
        // child extends parent — handled by the character-prefix transform.
        assertEquals(List.of("Knowledge Pixels", "Incubator"),
                crumbLabels("Knowledge Pixels", "Knowledge Pixels Incubator"));
    }
}
