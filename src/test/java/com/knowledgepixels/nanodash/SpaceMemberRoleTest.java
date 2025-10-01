package com.knowledgepixels.nanodash;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpaceMemberRoleTest {

    private SpaceMemberRole role;

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
    void constructorInitializesFieldsCorrectly() {
        assertNotNull(role);
        assertEquals(Values.iri("https://example.org/role"), role.getId());
        assertEquals("Role Label", role.getLabel());
        assertEquals("Role Name", role.getName());
        assertEquals("Role Title", role.getTitle());
        assertArrayEquals(new IRI[]{
                Values.iri("https://example.org/prop1"),
                Values.iri("https://example.org/prop2")
        }, role.getRegularProperties());
        assertArrayEquals(new IRI[]{
                Values.iri("https://example.org/invprop1")
        }, role.getInverseProperties());
    }

    @Test
    void addRoleParamsAddsCorrectParameters() {
        Multimap<String, String> params = ArrayListMultimap.create();
        role.addRoleParams(params);

        assertArrayEquals(params.get("role").toArray(), Arrays.stream(role.getRegularProperties()).map(IRI::stringValue).toArray());
        assertArrayEquals(params.get("invrole").toArray(), Arrays.stream(role.getInverseProperties()).map(IRI::stringValue).toArray());
    }

    @Test
    void getId() {
        assertEquals(Values.iri("https://example.org/role"), role.getId());
    }

    @Test
    void getLabel() {
        assertEquals("Role Label", role.getLabel());
    }

    @Test
    void getName() {
        assertEquals("Role Name", role.getName());
    }

    @Test
    void getTitle() {
        assertEquals("Role Title", role.getTitle());
    }

    @Test
    void getRegularProperties() {
        assertArrayEquals(new IRI[]{
                Values.iri("https://example.org/prop1"),
                Values.iri("https://example.org/prop2")
        }, role.getRegularProperties());
    }

    @Test
    void getInverseProperties() {
        assertArrayEquals(new IRI[]{
                Values.iri("https://example.org/invprop1")
        }, role.getInverseProperties());
    }

}