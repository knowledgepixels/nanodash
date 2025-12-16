package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfiledResourceTest {

    private final String PROFILED_RESOURCE_ID = "test-id";

    @Test
    void construct() {
        ProfiledResource pr = new ProfiledResource(PROFILED_RESOURCE_ID);
        assertNotNull(pr);
        assertEquals(PROFILED_RESOURCE_ID, pr.getId());
        assertTrue(ProfiledResource.isProfiledResource(PROFILED_RESOURCE_ID));
    }

    @Test
    void getNanopub() {
        assertNull(new ProfiledResource(PROFILED_RESOURCE_ID).getNanopub());
    }

    @Test
    void getNanopubId() {
        assertNull(new ProfiledResource(PROFILED_RESOURCE_ID).getNanopub());
    }

    @Test
    void getNamespace() {
        assertNull(new ProfiledResource(PROFILED_RESOURCE_ID).getNanopub());
    }

    @Test
    void getId() {
        ProfiledResource pr = new ProfiledResource(PROFILED_RESOURCE_ID);
        assertEquals(PROFILED_RESOURCE_ID, pr.getId());
    }

    @Test
    void testToString() {
        String id = PROFILED_RESOURCE_ID;
        ProfiledResource pr = new ProfiledResource(id);
        assertEquals(id, pr.toString());
    }

    @Test
    void isProfiledResource() {
        new ProfiledResource(PROFILED_RESOURCE_ID);
        assertTrue(ProfiledResource.isProfiledResource(PROFILED_RESOURCE_ID));
        assertFalse(ProfiledResource.isProfiledResource("non-existent-id"));
    }

}