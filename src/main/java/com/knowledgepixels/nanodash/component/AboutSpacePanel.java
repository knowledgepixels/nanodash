package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "About" tab body for a space: its structure (assigned presets, roles, and
 * configured view displays; issue #302), its users (members and observers), and
 * its sub-units (sub-spaces and maintained resources). Rendered as views
 * (query result tables) rather than the live view content.
 */
public class AboutSpacePanel extends Panel {

    /**
     * View that lists all assigned view displays of a resource (built on the
     * get-view-displays query Nanodash uses internally). Shown on About tabs
     * instead of rendering the assigned views themselves.
     */
    public static final String VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAmWY2SeqBn-3cb5uHhJL8mWUHfbT70qGZyTxqKrIRiyk/view-displays-view";

    /**
     * View listing the presets assigned to a resource (issue #302).
     */
    public static final String PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RA1jT4w9WA_omLXpC-VrGaeAAlKcyMf4Fj57741ebUxO0/preset-assignments-view";

    /**
     * View listing a space's assigned roles as a table (role, schema:name, and a
     * count of how many of the space's users hold each role), built on the
     * list-space-roles query. The built-in Admin role is always the first row.
     */
    public static final String SPACE_ROLES_VIEW = "https://w3id.org/np/RAAdFYTo9fnsJRwQCR3jkWHA3eroTQlZHHLnpPdzV2-t0/space-roles-view";

    /**
     * View listing a space's members (admins, maintainers, members) with their
     * highest role tier, built on the list-space-members query. Observer-tier
     * members are excluded.
     */
    public static final String MEMBERS_VIEW = "https://w3id.org/np/RAFmrcEqniX7mqrZQ8c4OCqjU-3wwKjLhE2glXAZKFNT0/space-members-view";

    /**
     * View listing a space's observers (members whose highest tier is observer,
     * i.e. holding no admin/maintainer/member role), built on the
     * list-space-observers query.
     */
    public static final String OBSERVERS_VIEW = "https://w3id.org/np/RA7uYe4WmsJCk3NaMTE8p0YofcMcRfjLyM8PGLaADkH18/space-observers-view";

    /**
     * View listing a space's direct sub-spaces with their types, built on the
     * list-sub-spaces query.
     */
    public static final String SUB_SPACES_VIEW = "https://w3id.org/np/RAeBFAztK931M_rw6FXoALjoLpIgwCk3oLiwy53CTlRJM/sub-spaces-view";

    /**
     * View listing the resources maintained by a space, built on the
     * list-maintained-resources query.
     */
    public static final String MAINTAINED_RESOURCES_VIEW = "https://w3id.org/np/RAaSQjXbKDgQTKiQuuArU9hzbHOduSaLqZNklyuj3HtZk/maintained-resources-view";

    /**
     * @param id    the Wicket markup id
     * @param space the space whose About listings to render
     */
    public AboutSpacePanel(String id, Space space) {
        super(id);

        // "Structure" section: presets, assigned roles, view displays.

        View presetsView = View.get(PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", space.getId()), new ViewDisplay(presetsView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).build());

        View rolesView = View.get(SPACE_ROLES_VIEW);
        // Pass the space as resource/context so the roles view's per-entry action
        // button (publish a role assignment) renders with param_space prefilled,
        // mirroring the "+" button on the content tab's role list. postPublishTab
        // keeps the user on the About tab after publishing a role/assignment, so
        // they see the updated roles list (the presets/view-display views
        // intentionally fall through to the Content tab, where their effect shows).
        add(QueryResultTableBuilder.create("roles", new QueryRef(rolesView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(rolesView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").build());

        View vdView = View.get(VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), "resource", space.getId()), new ViewDisplay(vdView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).build());

        // "Users" section: admins/maintainers/members, then observers.

        View membersView = View.get(MEMBERS_VIEW);
        add(QueryResultTableBuilder.create("members", new QueryRef(membersView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(membersView)).build());

        View observersView = View.get(OBSERVERS_VIEW);
        add(QueryResultTableBuilder.create("observers", new QueryRef(observersView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(observersView)).build());

        // "Sub-units" section: sub-spaces and maintained resources, side by side
        // (both views declare 6/12 width). resourceWithProfile/id/contextId let the
        // views' "add" actions pre-fill the new sub-space's IRI under this space's
        // namespace and this space as the maintainer, respectively; postPublishTab
        // returns the user here, where the new entry shows up.

        View subSpacesView = View.get(SUB_SPACES_VIEW);
        add(QueryResultListBuilder.create("subspaces", new QueryRef(subSpacesView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(subSpacesView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").build());

        View maintainedResourcesView = View.get(MAINTAINED_RESOURCES_VIEW);
        add(QueryResultListBuilder.create("maintainedresources", new QueryRef(maintainedResourcesView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(maintainedResourcesView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").build());
    }

}
