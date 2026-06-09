package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "About" tab body for a space: its assigned roles, assigned presets, and
 * the listing of its configured view displays (issue #302). Rendered as views
 * (query result tables) rather than the live view content.
 */
public class AboutSpacePanel extends Panel {

    /**
     * View that lists all assigned view displays of a resource (built on the
     * get-view-displays query Nanodash uses internally). Shown on About tabs
     * instead of rendering the assigned views themselves.
     */
    public static final String VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAVVUjFMWIylf0Bz0n5-NdFG4_T0d6TWQvRYC23IZscYo/view-displays-view";

    /**
     * View listing the presets assigned to a resource (issue #302).
     */
    public static final String PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RAFRpDY_Tw7PYCGQ0t8UYLvM-EPIj-n4BpwwUDUjdj2I4/preset-assignments-view";

    /**
     * View listing a space's assigned roles, built on the existing
     * get-space-roles query.
     */
    public static final String SPACE_ROLES_VIEW = "https://w3id.org/np/RAsH9ItKDb5sRdMul-dTT-Dqb7u4u80RmeYUndZKyjGZ8/space-roles-view";

    /**
     * View listing a space's members (admins, maintainers, members) with their
     * highest role tier, built on the list-space-members query. Observer-tier
     * members are excluded.
     */
    public static final String MEMBERS_VIEW = "https://w3id.org/np/RAtXkvt5k8F0Q0wcHGBUeviereSSKFqPd5awX4TOi-VvQ/space-members-view";

    /**
     * View listing a space's observers (members whose highest tier is observer,
     * i.e. holding no admin/maintainer/member role), built on the
     * list-space-observers query.
     */
    public static final String OBSERVERS_VIEW = "https://w3id.org/np/RAWm9RSI6AHTl1n5LNQj-iYKcU0iJFknwMnZ4xgvpckM0/space-observers-view";

    /**
     * @param id    the Wicket markup id
     * @param space the space whose About listings to render
     */
    public AboutSpacePanel(String id, Space space) {
        super(id);

        View rolesView = View.get(SPACE_ROLES_VIEW);
        add(QueryResultTableBuilder.create("roles", new QueryRef(rolesView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(rolesView)).build());

        View membersView = View.get(MEMBERS_VIEW);
        add(QueryResultTableBuilder.create("members", new QueryRef(membersView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(membersView)).build());

        View observersView = View.get(OBSERVERS_VIEW);
        add(QueryResultTableBuilder.create("observers", new QueryRef(observersView.getQuery().getQueryId(), "space", space.getId()), new ViewDisplay(observersView)).build());

        View presetsView = View.get(PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", space.getId()), new ViewDisplay(presetsView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).build());

        View vdView = View.get(VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), "resource", space.getId()), new ViewDisplay(vdView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).build());
    }

}
