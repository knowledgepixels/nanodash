package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.request.cycle.RequestCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The post-publish refresh of a resource's page structure (issue #622): a structure that
 * is already on screen is kept and swapped in place, and only an empty one is invalidated
 * outright so the pages' lazy path can make a first view display appear.
 * <p>
 * Also the page-level "refresh now", which goes one step further: the views are refreshed
 * after the list of view displays, so that it is the refreshed list whose views are
 * brought up to date rather than the one that was on screen when the user clicked.
 */
class StructureRefreshTest {

    private static class TestResource extends AbstractResourceWithProfile {

        TestResource(String id) {
            super(id);
        }

        @Override
        public String getNanopubId() {
            return null;
        }

        @Override
        public Nanopub getNanopub() {
            return null;
        }

        @Override
        public String getNamespace() {
            return null;
        }

        @Override
        public String getLabel() {
            return getId();
        }

    }

    // isViewRefreshDue remembers its answer for the rest of the request cycle, so each
    // call below has to stand on its own: whatever cycle another test left bound to this
    // thread is taken away for the duration and put back afterwards.
    private RequestCycle boundRequestCycle;

    @BeforeEach
    void detachRequestCycle() {
        boundRequestCycle = RequestCycle.get();
        ThreadContext.setRequestCycle(null);
    }

    @AfterEach
    void reattachRequestCycle() {
        ThreadContext.setRequestCycle(boundRequestCycle);
    }

    // The flags forceRefresh acts on are read directly: going through isDataInitialized()
    // would kick off the (network-bound) data update the assertions are not about.
    private static Object field(AbstractResourceWithProfile r, String name) throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(r);
    }

    private static void setField(AbstractResourceWithProfile r, String name, Object value) throws Exception {
        Field f = AbstractResourceWithProfile.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(r, value);
    }

    private static void setStructure(AbstractResourceWithProfile r, ViewDisplay... viewDisplays) throws Exception {
        AbstractResourceWithProfile.ResourceWithProfile data = new AbstractResourceWithProfile.ResourceWithProfile();
        for (ViewDisplay vd : viewDisplays) data.viewDisplays.add(vd);
        setField(r, "data", data);
        setField(r, "dataInitialized", !data.viewDisplays.isEmpty());
    }

    private static ViewDisplay viewDisplay() {
        // A minimal view display with no view attached: enough to be an entry of the
        // structure, which is all the signature and the empty checks look at.
        return new ViewDisplay(10);
    }

    @Test
    @DisplayName("a loaded structure is kept on screen and marked as refreshing")
    void loadedStructureIsKept() throws Exception {
        TestResource r = new TestResource("https://example.org/test/kept");
        setStructure(r, viewDisplay());

        r.forceRefresh(5000);

        assertTrue((Boolean) field(r, "dataInitialized"), "the loaded structure must stay renderable");
        assertTrue((Boolean) field(r, "structureRefreshPending"), "the refresh must be marked as pending");
        assertTrue((Boolean) field(r, "dataNeedsUpdate"));
        assertTrue(r.getRunUpdateAfter() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("an empty structure is invalidated outright, as before")
    void emptyStructureIsInvalidated() throws Exception {
        TestResource r = new TestResource("https://example.org/test/empty");
        setStructure(r);

        r.forceRefresh(5000);

        assertFalse((Boolean) field(r, "dataInitialized"), "nothing on screen to keep");
        assertFalse((Boolean) field(r, "structureRefreshPending"), "the lazy path handles this one");
    }

    @Test
    @DisplayName("the structure signature tells a changed structure from an unchanged one")
    void structureSignature() throws Exception {
        TestResource r = new TestResource("https://example.org/test/signature");
        setStructure(r, viewDisplay());
        String before = r.getStructureSignature();

        setStructure(r, viewDisplay());
        assertEquals(before, r.getStructureSignature(), "the same structure must fingerprint the same");

        setStructure(r, viewDisplay(), viewDisplay());
        assertNotEquals(before, r.getStructureSignature(), "an added view display must show up");
    }

    @Test
    @DisplayName("no refresh pending without a forceRefresh")
    void nothingPendingByDefault() throws Exception {
        TestResource r = new TestResource("https://example.org/test/idle");
        setStructure(r, viewDisplay());
        assertFalse(r.isStructureRefreshPending());
    }

    @Test
    @DisplayName("the views wait for the refreshed structure before they are refreshed")
    void viewRefreshWaitsForTheStructure() throws Exception {
        TestResource r = new TestResource("https://example.org/test/views-wait");
        setStructure(r, viewDisplay());

        r.forceRefresh(5000);
        r.requestViewRefresh();

        assertTrue(r.isViewRefreshRequested());
        assertFalse(r.isViewRefreshDue(true), "the list on screen is the old one, so it must not refresh yet");
        assertTrue(r.isViewRefreshRequested(), "the request must survive for the refreshed list");

        // The refreshed structure lands.
        setField(r, "structureRefreshPending", false);

        assertTrue(r.isViewRefreshDue(true), "the refreshed list refreshes its views");
        assertFalse(r.isViewRefreshRequested(), "and takes the request away");
        assertFalse(r.isViewRefreshDue(true), "so a later list does not refresh again");
    }

    @Test
    @DisplayName("a list carrying its own view displays has no structure to wait for")
    void viewRefreshOnAnExplicitList() throws Exception {
        TestResource r = new TestResource("https://example.org/test/views-explicit");
        setStructure(r, viewDisplay());

        r.forceRefresh(5000);
        r.requestViewRefresh();

        // A ?root=-pinned page fetches its view displays itself, so what it shows is
        // already the refreshed list even while the singleton structure is still in flight.
        assertTrue(r.isViewRefreshDue(false));
        assertFalse(r.isViewRefreshRequested());
    }

    @Test
    @DisplayName("no view refresh without a request")
    void noViewRefreshByDefault() throws Exception {
        TestResource r = new TestResource("https://example.org/test/views-idle");
        setStructure(r, viewDisplay());
        assertFalse(r.isViewRefreshRequested());
        assertFalse(r.isViewRefreshDue(true));
        assertFalse(r.isViewRefreshDue(false));
    }

}
