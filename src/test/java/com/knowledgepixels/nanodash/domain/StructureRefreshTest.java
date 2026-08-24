package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.ViewDisplay;
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

}
