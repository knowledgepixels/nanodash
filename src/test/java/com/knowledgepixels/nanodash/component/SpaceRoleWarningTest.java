package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.SpaceMemberRoleRef;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.Space;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for issue #648: a view listing the holders of one role shows nothing until that role
 * is attached to the space, however many grants of it exist. The About tab says so, and what
 * it says is worked out here.
 */
class SpaceRoleWarningTest {

    private static final String OBSERVER_ROLE = "https://w3id.org/np/RAqAgIgZ/observer-role";
    private static final String ORGANIZER_ROLE = "https://w3id.org/np/RACYgvw/organizer-role";

    private static IRI iri(String value) {
        return Utils.vf.createIRI(value);
    }

    private static ViewDisplay displayOf(String title, String... pinnedRoles) {
        Set<IRI> roles = new LinkedHashSet<>();
        for (String role : pinnedRoles) roles.add(iri(role));
        View view = mock(View.class);
        when(view.getTitle()).thenReturn(title);
        when(view.getPinnedRoles()).thenReturn(roles);
        ViewDisplay display = mock(ViewDisplay.class);
        when(display.getView()).thenReturn(view);
        return display;
    }

    private static SpaceMemberRoleRef attached(String roleIri) {
        SpaceMemberRole role = mock(SpaceMemberRole.class);
        when(role.getId()).thenReturn(iri(roleIri));
        return new SpaceMemberRoleRef(role, null);
    }

    private static Space spaceWith(List<SpaceMemberRoleRef> roles, List<ViewDisplay> displays) {
        Space space = mock(Space.class);
        when(space.getRoles()).thenReturn(roles);
        when(space.getTopLevelViewDisplays(null)).thenReturn(displays);
        return space;
    }

    @Test
    void namesTheViewAndTheRoleItWaitsFor() {
        Space space = spaceWith(List.of(attached(ORGANIZER_ROLE)),
                List.of(displayOf("👁 Observers", OBSERVER_ROLE)));
        assertEquals(List.of(new AboutSpacePanel.UnattachedRole("👁 Observers", iri(OBSERVER_ROLE))),
                AboutSpacePanel.unattachedRoles(space, null));
    }

    @Test
    void saysNothingAboutARoleTheSpaceHasAttached() {
        Space space = spaceWith(List.of(attached(OBSERVER_ROLE)),
                List.of(displayOf("👁 Observers", OBSERVER_ROLE)));
        assertTrue(AboutSpacePanel.unattachedRoles(space, null).isEmpty());
    }

    @Test
    void saysNothingAboutAViewThatWorksWhicheverRolesAreAttached() {
        // Views that leave the role open (or have nothing to do with roles) show their
        // entries whatever the space has attached.
        Space space = spaceWith(List.of(), List.of(displayOf("📄 Presentations")));
        assertTrue(AboutSpacePanel.unattachedRoles(space, null).isEmpty());
    }

    @Test
    void reportsEveryViewWaitingOnARole() {
        Space space = spaceWith(List.of(),
                List.of(displayOf("👁 Observers", OBSERVER_ROLE),
                        displayOf("🎤 Organizers", ORGANIZER_ROLE, OBSERVER_ROLE)));
        assertEquals(List.of(
                        new AboutSpacePanel.UnattachedRole("👁 Observers", iri(OBSERVER_ROLE)),
                        new AboutSpacePanel.UnattachedRole("🎤 Organizers", iri(ORGANIZER_ROLE)),
                        new AboutSpacePanel.UnattachedRole("🎤 Organizers", iri(OBSERVER_ROLE))),
                AboutSpacePanel.unattachedRoles(space, null));
    }

}
