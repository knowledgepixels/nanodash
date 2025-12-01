package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpaceMemberRoleRefTest {

    private SpaceMemberRole role;
    private final String NANOPUB_URI = "https://w3id.org/np/RAgCp3qfw--UnIZU8TKRGCSWVhz1BXSDaj-uQGlizFYR8";

    @BeforeEach
    void setUp() {
        ApiResponseEntry entry = mock(ApiResponseEntry.class);
        when(entry.get("role")).thenReturn("https://example.org/role");
        when(entry.get("roleLabel")).thenReturn("Role Label");
        when(entry.get("roleName")).thenReturn("Role Name");
        when(entry.get("roleTitle")).thenReturn("Role Title");
        when(entry.get("regularProperties")).thenReturn("https://example.org/prop1 https://example.org/prop2");
        when(entry.get("inverseProperties")).thenReturn("https://example.org/invprop1");
        role = new SpaceMemberRole(entry);
    }

    @Test
    void getRole() {
        SpaceMemberRoleRef roleRef = new SpaceMemberRoleRef(role, NANOPUB_URI);
        assertEquals(role, roleRef.getRole());
    }

    @Test
    void getNanopubUri() {
        SpaceMemberRoleRef roleRef = new SpaceMemberRoleRef(role, NANOPUB_URI);
        assertEquals(NANOPUB_URI, roleRef.getNanopubUri());
    }

}