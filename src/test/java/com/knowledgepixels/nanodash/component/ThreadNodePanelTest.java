package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.DiscussionThread;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * The thread as a reader meets it: what a card shows, and what folding a branch, asking for
 * more of a level, and reaching the bottom of the shown depth actually do.
 */
class ThreadNodePanelTest {

    private static final String ROOT = "http://example.org/statement";
    private static final String VIEW_ID = "http://example.org/np/discussion-view";

    private WicketTester tester;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        tester.getSession().setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    @Test
    void aCardShowsTheRelationItStandsInAndWhatHangsBelowIt() {
        start(thread(
                row(ROOT, null, null, "Coffee reduces the risk of type 2 diabetes"),
                row("http://example.org/d1", ROOT, "disputes", "The effect disappears when adjusting"),
                row("http://example.org/d2", "http://example.org/d1", "disputes", "The re-analysis excluded over-60s")), 5);

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("statement"), "the root is labelled as the statement");
        assertTrue(html.contains("1 direct response"), "with the number of direct responses");
        assertTrue(html.contains("disputes this dispute"),
                "and a dispute of a dispute says so rather than repeating the bare relation");
        assertTrue(html.contains("The re-analysis excluded over-60s"), "the whole shown depth is rendered");
    }

    @Test
    void aBranchFoldsToOneLine() {
        start(thread(
                row(ROOT, null, null, "A statement"),
                row("http://example.org/r1", ROOT, "replies", "A response")), 5);
        assertTrue(tester.getLastResponseAsString().contains("A response"));

        clickFirst("collapse");

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("collapsed, 1 response"), "the fold says what it holds");
        assertFalse(html.contains("A response"), "and the branch itself is gone from the page");
    }

    @Test
    void aLevelHandsOverItsResponsesAFewAtATime() {
        List<ApiResponseEntry> rows = new ArrayList<>();
        rows.add(row(ROOT, null, null, "A statement"));
        for (int i = 1; i <= 5; i++) {
            rows.add(row("http://example.org/r" + i, ROOT, "replies", "Response " + i));
        }
        start(thread(rows.toArray(new ApiResponseEntry[0])), 2);

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("Response 2"), "the first two are on screen");
        assertFalse(html.contains("Response 3"), "the rest are not");
        assertTrue(html.contains("+ load 3 more responses"), "and are offered by the button");

        clickFirst("loadMore");

        html = tester.getLastResponseAsString();
        assertTrue(html.contains("Response 4"), "the next page arrives");
        assertFalse(html.contains("Response 5"), "one page at a time, not the rest at once");
        assertTrue(html.contains("+ load 1 more response"), "and the button counts down");

        clickFirst("loadMore");

        html = tester.getLastResponseAsString();
        assertTrue(html.contains("Response 5"), "the last one arrives");
        assertFalse(html.contains("+ load"), "and with nothing left the button goes");
    }

    @Test
    void belowTheShownDepthTheThreadContinuesBehindALink() {
        // Four levels, where three are shown: the fourth is not rendered but linked to.
        start(thread(
                row(ROOT, null, null, "A statement"),
                row("http://example.org/a", ROOT, "disputes", "Level one"),
                row("http://example.org/b", "http://example.org/a", "disputes", "Level two"),
                row("http://example.org/c", "http://example.org/b", "replies", "Level three"),
                row("http://example.org/d", "http://example.org/c", "replies", "Level four")), 5);

        String html = tester.getLastResponseAsString();
        assertTrue(html.contains("Level two"), "the shown depth is rendered");
        assertFalse(html.contains("Level three"), "what lies below it is not");
        assertTrue(html.contains("continue this thread (2 responses, 2 levels deep)"),
                "and says how much is waiting there");

        BookmarkablePageLink<?> link = (BookmarkablePageLink<?>) find("continueThread");
        assertNotNull(link);
        assertEquals(VIEW_ID, link.getPageParameters().get("view").toString());
        assertEquals("http://example.org/b", link.getPageParameters().get("queryparam_resource").toString(),
                "the view reopens rooted at the response the thread stopped under");
    }

    @Test
    void aThreadWithoutAViewStillReads() {
        // A display without a view behind it (no actions, nothing to link on to) must not
        // take the thread down with it.
        DiscussionThread thread = DiscussionThread.of(response(
                row(ROOT, null, null, "A statement"),
                row("http://example.org/r1", ROOT, "replies", "A response")));
        ThreadNodePanel.ThreadContext context = new ThreadNodePanel.ThreadContext(null,
                new QueryRef("RAxqXyhP1fnjvDdX-K0z9TgnwoXf462FxV1wEAWRm_gos/check-nanopub-loaded"),
                null, null, null, null, null, 5, 3);
        tester.startComponentInPage(new ThreadNodePanel("panel", context, thread.getRoots().get(0), 0));

        assertTrue(tester.getLastResponseAsString().contains("A response"));
    }

    /** Renders the thread's single root at depth 0, with the given responses per level. */
    private void start(DiscussionThread thread, int pageSize) {
        View view = mock(View.class, withSettings().serializable());
        when(view.getId()).thenReturn(VIEW_ID);
        when(view.getQueryField()).thenReturn("resource");
        when(view.getViewEntryActionList()).thenReturn(List.of());
        ThreadNodePanel.ThreadContext context = new ThreadNodePanel.ThreadContext(view,
                new QueryRef("RAxqXyhP1fnjvDdX-K0z9TgnwoXf462FxV1wEAWRm_gos/check-nanopub-loaded"),
                null, null, null, null, null, pageSize, QueryResultThread.MAX_DEPTH);
        tester.startComponentInPage(new ThreadNodePanel("panel", context, thread.getRoots().get(0), 0));
    }

    /** Clicks the first component with the given id, wherever the recursion put it. */
    private void clickFirst(String componentId) {
        Component component = find(componentId);
        assertNotNull(component, "no component '" + componentId + "' on the page");
        tester.executeAjaxEvent((AjaxLink<?>) component, "click");
    }

    private Component find(String componentId) {
        return tester.getLastRenderedPage().visitChildren(Component.class,
                (IVisitor<Component, Component>) (component, visit) -> {
                    if (component.getId().equals(componentId) && component.isVisibleInHierarchy()) {
                        visit.stop(component);
                    }
                });
    }

    private static DiscussionThread thread(ApiResponseEntry... rows) {
        return DiscussionThread.of(response(rows));
    }

    private static ApiResponse response(ApiResponseEntry... rows) {
        ApiResponse response = new ApiResponse();
        response.setHeader(new String[]{DiscussionThread.COL_RESPONSE, DiscussionThread.COL_RESPONDS_TO,
                DiscussionThread.COL_RELATION, DiscussionThread.COL_LABEL, DiscussionThread.COL_DATE});
        for (ApiResponseEntry row : rows) response.add(row);
        return response;
    }

    private static ApiResponseEntry row(String id, String respondsTo, String relation, String label) {
        ApiResponseEntry entry = new ApiResponseEntry();
        entry.add(DiscussionThread.COL_RESPONSE, id);
        entry.add(DiscussionThread.COL_RESPONDS_TO, respondsTo == null ? "" : respondsTo);
        entry.add(DiscussionThread.COL_RELATION, relation == null ? "" : relation);
        entry.add(DiscussionThread.COL_LABEL, label);
        return entry;
    }

}
