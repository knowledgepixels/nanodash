package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewAnchorsTest {

    private static ViewDisplay viewDisplay(String title, String structuralPosition) {
        ViewDisplay vd = mock(ViewDisplay.class);
        when(vd.getTitle()).thenReturn(title);
        when(vd.getStructuralPosition()).thenReturn(structuralPosition);
        return vd;
    }

    /**
     * A view display with no title of its own, showing the label of its query as heading.
     */
    private static ViewDisplay queryLabelledViewDisplay(String queryLabel, String structuralPosition) {
        ViewDisplay vd = viewDisplay(null, structuralPosition);
        GrlcQuery query = mock(GrlcQuery.class);
        when(query.getLabel()).thenReturn(queryLabel);
        View view = mock(View.class);
        when(view.getQuery()).thenReturn(query);
        when(vd.getView()).thenReturn(view);
        return vd;
    }

    @Test
    void slugifyMakesFragmentIdentifiersFromTitles() {
        assertEquals("messages", ViewAnchors.slugify("Messages"));
        // Leading emoji (common in view titles) is dropped, not turned into a separator.
        assertEquals("messages", ViewAnchors.slugify("💬 Messages"));
        assertEquals("open-tasks", ViewAnchors.slugify("Open tasks"));
        assertEquals("papers-preprints", ViewAnchors.slugify("Papers & preprints"));
        // Accents fold onto their base letter rather than being dropped.
        assertEquals("ubersicht", ViewAnchors.slugify("Übersicht"));
    }

    @Test
    void slugifyReturnsEmptyWhenNothingUsableIsLeft() {
        assertEquals("", ViewAnchors.slugify(null));
        assertEquals("", ViewAnchors.slugify(""));
        assertEquals("", ViewAnchors.slugify("   "));
        assertEquals("", ViewAnchors.slugify("💬"));
        assertEquals("", ViewAnchors.slugify("--"));
    }

    @Test
    void slugifyKeepsAnchorsUsableAsIdSelectorsAndReasonablyShort() {
        // A digit-initial id is legal HTML but not a legal CSS id selector.
        assertEquals("view-2026-meetings", ViewAnchors.slugify("2026 meetings"));
        String longSlug = ViewAnchors.slugify("A very long section title that goes on and on and on beyond any sensible limit");
        assertTrue(longSlug.length() <= 60, longSlug);
        assertTrue(longSlug.startsWith("a-very-long-section-title"), longSlug);
        assertTrue(!longSlug.endsWith("-"), longSlug);
    }

    @Test
    void anchorsComeFromTitles() {
        assertEquals(List.of("highlightings", "messages"),
                ViewAnchors.forViewDisplays(List.of(
                        viewDisplay("Highlightings", "4.4.highlightings"),
                        viewDisplay("💬 Messages", "4.5.messages"))));
    }

    @Test
    void anchorsComeFromTheQueryLabelWhenTheViewDisplayHasNoTitleOfItsOwn() {
        // What such a section shows as heading is the query's label (every query-result
        // panel falls back to it), so that is what the anchor has to be named after.
        assertEquals(List.of("latest-nanopublications"),
                ViewAnchors.forViewDisplays(List.of(
                        queryLabelledViewDisplay("Latest Nanopublications", "5.5.default"))));
        // An explicitly empty title hides the heading, so it does not reach for the label.
        ViewDisplay untitled = queryLabelledViewDisplay("Latest Nanopublications", "5.5.default");
        when(untitled.getTitle()).thenReturn("");
        assertEquals(List.of("view"), ViewAnchors.forViewDisplays(List.of(untitled)));
    }

    @Test
    void anchorsFallBackToTheStructuralPositionLabel() {
        // No title: the free label part of the position identifies the section instead.
        assertEquals(List.of("papers"),
                ViewAnchors.forViewDisplays(List.of(viewDisplay(null, "4.4.papers"))));
        // ...including its dotted sub-segments.
        assertEquals(List.of("concepts-1"),
                ViewAnchors.forViewDisplays(List.of(viewDisplay("", "4.5.concepts.1"))));
        // The default position carries no meaning of its own, so it isn't used.
        assertEquals(List.of("view"),
                ViewAnchors.forViewDisplays(List.of(viewDisplay(null, "5.5.default"))));
        assertEquals(List.of("view"),
                ViewAnchors.forViewDisplays(List.of(viewDisplay(null, "malformed"))));
    }

    @Test
    void anchorsAreUniqueWithinAPage() {
        assertEquals(List.of("messages", "messages-2", "messages-3"),
                ViewAnchors.forViewDisplays(List.of(
                        viewDisplay("Messages", "4.4.a"),
                        viewDisplay("Messages", "4.5.b"),
                        viewDisplay("💬 messages", "4.6.c"))));
        // Untitled views all fall back to the same base anchor.
        assertEquals(List.of("view", "view-2"),
                ViewAnchors.forViewDisplays(List.of(
                        viewDisplay(null, "5.5.default"),
                        viewDisplay(null, "5.5.default"))));
    }

    @Test
    void anAllocatorKeepsTheAnchorsOfAPageDistinctAcrossSeparateCalls() {
        // Pages that build their view panels one by one (the list pages, …) ask for one
        // anchor at a time, so uniqueness has to hold across calls, not just within a list.
        ViewAnchors.Allocator allocator = new ViewAnchors.Allocator();
        assertEquals("messages", allocator.next(viewDisplay("Messages", "4.4.a")));
        assertEquals("messages-2", allocator.next(viewDisplay("Messages", "4.5.b")));
        assertEquals("titlebar-2", allocator.next(viewDisplay("Titlebar", "4.6.c")));
    }

    @Test
    void anchorsNeverShadowThePageChrome() {
        assertEquals(List.of("content-pane-2"),
                ViewAnchors.forViewDisplays(List.of(viewDisplay("Content pane", "4.4.x"))));
    }

}
