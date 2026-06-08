package com.knowledgepixels.nanodash;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.Arrays;
import java.util.Set;

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

    private static SpaceMemberRole roleWithType(String roleType) {
        ApiResponseEntry entry = mock(ApiResponseEntry.class);
        when(entry.get("role")).thenReturn("https://example.org/role");
        when(entry.get("roleType")).thenReturn(roleType);
        return new SpaceMemberRole(entry);
    }

    @Test
    void defaultsToObserverTierWhenRoleTypeAbsent() {
        // The shared setUp() role stubs no roleType.
        assertEquals(KPXL_TERMS.OBSERVER_ROLE, role.getTier());
        assertEquals(1, role.getTierRank());
    }

    @Test
    void parsesTierFromRoleType() {
        assertEquals(KPXL_TERMS.MAINTAINER_ROLE, roleWithType(KPXL_TERMS.MAINTAINER_ROLE.stringValue()).getTier());
        assertEquals(3, roleWithType(KPXL_TERMS.MAINTAINER_ROLE.stringValue()).getTierRank());
        assertEquals(KPXL_TERMS.MEMBER_ROLE, roleWithType(KPXL_TERMS.MEMBER_ROLE.stringValue()).getTier());
        assertEquals(2, roleWithType(KPXL_TERMS.MEMBER_ROLE.stringValue()).getTierRank());
    }

    @Test
    void tierRankIsStrictlyOrdered() {
        int admin = SpaceMemberRole.tierRank(KPXL_TERMS.ADMIN_ROLE_TYPE);
        int maintainer = SpaceMemberRole.tierRank(KPXL_TERMS.MAINTAINER_ROLE);
        int member = SpaceMemberRole.tierRank(KPXL_TERMS.MEMBER_ROLE);
        int observer = SpaceMemberRole.tierRank(KPXL_TERMS.OBSERVER_ROLE);
        assertTrue(admin > maintainer);
        assertTrue(maintainer > member);
        assertTrue(member > observer);
        assertTrue(observer > SpaceMemberRole.EVERYONE_RANK);
    }

    @Test
    void unknownAndNullTierRankToEveryoneFloor() {
        assertEquals(SpaceMemberRole.EVERYONE_RANK, SpaceMemberRole.tierRank(Values.iri("https://example.org/role")));
        assertEquals(SpaceMemberRole.EVERYONE_RANK, SpaceMemberRole.tierRank(null));
        assertEquals(0, SpaceMemberRole.EVERYONE_RANK);
    }

    @Test
    void isTierRecognizesOnlyTierIris() {
        assertTrue(SpaceMemberRole.isTier(KPXL_TERMS.ADMIN_ROLE_TYPE));
        assertTrue(SpaceMemberRole.isTier(KPXL_TERMS.MAINTAINER_ROLE));
        assertTrue(SpaceMemberRole.isTier(KPXL_TERMS.MEMBER_ROLE));
        assertTrue(SpaceMemberRole.isTier(KPXL_TERMS.OBSERVER_ROLE));
        assertFalse(SpaceMemberRole.isTier(Values.iri("https://example.org/role")));
        assertFalse(SpaceMemberRole.isTier(null));
    }

    @Test
    void adminRoleHasAdminTier() {
        assertEquals(KPXL_TERMS.ADMIN_ROLE_TYPE, SpaceMemberRole.ADMIN_ROLE.getTier());
        assertEquals(4, SpaceMemberRole.ADMIN_ROLE.getTierRank());
        assertTrue(SpaceMemberRole.ADMIN_ROLE.isAdminRole());
    }

    private static final IRI VIEWER = Values.iri("https://orcid.org/0000-0000-0000-0001");

    @Test
    void emptyRestrictionIsVisibleToEveryone() {
        assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(), null, null, false));
        assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(), VIEWER, mock(Space.class), false));
        assertTrue(SpaceMemberRole.isViewerEntitled(null, null, null, false));
    }

    @Test
    void userPageOwnerHoldsAdminTier() {
        // No governing space (user page): the owner is the sole admin, so any tier
        // requirement matches the owner and nobody else.
        for (IRI tier : new IRI[]{KPXL_TERMS.ADMIN_ROLE_TYPE, KPXL_TERMS.MAINTAINER_ROLE,
                KPXL_TERMS.MEMBER_ROLE, KPXL_TERMS.OBSERVER_ROLE}) {
            assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(tier), VIEWER, null, true), tier.toString());
            assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(tier), VIEWER, null, false), tier.toString());
        }
    }

    @Test
    void everyoneRoleIsVisibleToAll() {
        Set<IRI> reqs = Set.of(KPXL_TERMS.EVERYONE_ROLE);
        // anonymous viewer on a space page
        assertTrue(SpaceMemberRole.isViewerEntitled(reqs, null, mock(Space.class), false));
        // member of a space (any tier)
        Space space = mock(Space.class);
        when(space.userTier(VIEWER)).thenReturn(SpaceMemberRole.EVERYONE_RANK);
        assertTrue(SpaceMemberRole.isViewerEntitled(reqs, VIEWER, space, false));
        // non-owner on a user page
        assertTrue(SpaceMemberRole.isViewerEntitled(reqs, VIEWER, null, false));
        // EveryoneRole ranks at the floor, below Observer
        assertEquals(SpaceMemberRole.EVERYONE_RANK, SpaceMemberRole.tierRank(KPXL_TERMS.EVERYONE_ROLE));
        assertTrue(SpaceMemberRole.isTier(KPXL_TERMS.EVERYONE_ROLE));
    }

    @Test
    void userPageSpecificRoleMatchesNobody() {
        // Specific roles are unholdable without a space, so even the owner (admin
        // tier, not that role) does not match.
        IRI customRole = Values.iri("https://example.org/newsletterEditor");
        assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(customRole), VIEWER, null, true));
        assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(customRole), VIEWER, null, false));
    }

    @Test
    void loggedOutViewerFailsRestriction() {
        assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(KPXL_TERMS.MEMBER_ROLE), null, mock(Space.class), false));
    }

    @Test
    void tierThresholdMatchesAtOrAboveRequired() {
        Space space = mock(Space.class);
        when(space.userTier(VIEWER)).thenReturn(3); // maintainer
        assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(KPXL_TERMS.MAINTAINER_ROLE), VIEWER, space, false));
        assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(KPXL_TERMS.MEMBER_ROLE), VIEWER, space, false));
        assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(KPXL_TERMS.ADMIN_ROLE_TYPE), VIEWER, space, false));
    }

    @Test
    void specificRoleMatchesOnlyWhenHeld() {
        IRI customRole = Values.iri("https://example.org/newsletterEditor");
        Space space = mock(Space.class);
        when(space.userTier(VIEWER)).thenReturn(4); // even an admin...
        when(space.viewerHoldsRole(VIEWER, customRole)).thenReturn(false);
        // ...does not see a specific-role-gated action they don't hold (no admin override)
        assertFalse(SpaceMemberRole.isViewerEntitled(Set.of(customRole), VIEWER, space, false));
        when(space.viewerHoldsRole(VIEWER, customRole)).thenReturn(true);
        assertTrue(SpaceMemberRole.isViewerEntitled(Set.of(customRole), VIEWER, space, false));
    }

}