package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.QueryApiAccess;
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
     * The "ℹ️ Info" view: key-value facts about the space (type, alternative IDs,
     * dates, latest and root definition). Also shown on the Content tab; surfaced
     * here at the top of the About tab. Its query needs both the space IRI
     * ({@code space}) and the space's nanopub ({@code spaceNp}) so it can scope to
     * a single space-ref.
     */
    public static final String SPACE_INFO_VIEW = "https://w3id.org/np/RAIh3Cq4K99abRiL2xZphMYTjByvZYATK-d--dI3DD05g/space-info-view-kind";

    /**
     * View that lists all assigned view displays of a resource (built on the
     * get-view-displays query Nanodash uses internally). Shown on About tabs
     * instead of rendering the assigned views themselves.
     */
    public static final String VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RA2Wxm80NAzOrXCjIZK9oVJ2vzbycuQtT3wLSGTX5vDVw/view-displays-view";

    /**
     * View listing the presets assigned to a resource (issue #302).
     */
    public static final String PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RA1kCQYXscKPY_qDvQ-WAKhoVomrTQLSt91JSD4Y03CKI/preset-assignments-view";

    /**
     * View listing a space's assigned roles as a table (role, schema:name, and a
     * count of how many of the space's users hold each role), built on the
     * list-space-roles query. The built-in Admin role is always the first row.
     */
    public static final String SPACE_ROLES_VIEW = "https://w3id.org/np/RActRjB7sOPegsSWdlJPNXvEfqEuomlO9HvjmBuXMqfQw/space-roles-view";

    /**
     * View listing a space's members (admins, maintainers, members) with their
     * highest role tier, built on the list-space-members query. Observer-tier
     * members are excluded.
     */
    public static final String MEMBERS_VIEW = "https://w3id.org/np/RAFmrcEqniX7mqrZQ8c4OCqjU-3wwKjLhE2glXAZKFNT0/space-members-view";

    /**
     * View listing a space's non-approved role claims (agents holding an
     * admin/maintainer/member-tier role instantiation that is not in the
     * validated state — a self-assigned or otherwise ungranted claim awaiting
     * approval), built on the list-space-non-approved query. Carries a per-row
     * "approve" action (visible to members and above) that re-asserts the same
     * role triple, signed by the approver.
     */
    public static final String NON_APPROVED_VIEW = "https://w3id.org/np/RAk5nU4XXK1-CzrE2mcSLcRmJXnANVWgkQ2dNUQzDVR64/pending-members-view";

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
    public static final String SUB_SPACES_VIEW = "https://w3id.org/np/RAoO4uYnSJXCHr0F5uVC5sYpkD4HOh-lsHQj4k3epqeH0/sub-spaces-view";

    /**
     * View listing the resources maintained by a space, built on the
     * list-maintained-resources query.
     */
    public static final String MAINTAINED_RESOURCES_VIEW = "https://w3id.org/np/RA4mk84QDZ4njO5N1sryJ5_wbyG7bAisL4BIAsNoISt-Y/maintained-resources-view";

    /**
     * @param id    the Wicket markup id
     * @param space the space whose About listings to render
     */
    public AboutSpacePanel(String id, Space space) {
        super(id);

        // "Structure" section: key-value info, presets, assigned roles, view displays.

        // The info view leads the section (to the left of the presets). Its query is
        // scoped to a single space-ref, so it needs both the space IRI and the
        // space's nanopub (the latest-definition NP) — bind them as separate params.
        View infoView = View.get(SPACE_INFO_VIEW);
        Multimap<String, String> infoParams = ArrayListMultimap.create();
        infoParams.put("space", space.getId());
        infoParams.put("spaceNp", space.getNanopubId());
        add(QueryResultTableBuilder.create("info", new QueryRef(infoView.getQuery().getQueryId(), infoParams), new ViewDisplay(infoView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).build());

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

        // Non-approved (pending) higher-tier role claims, between members and observers.
        // Ref-scoped (root_np), like the observers table below; there is no IRI-keyed
        // fallback query, so when the ref root is unknown (pre-v3 data) the table is driven
        // by the param-less query, which yields no rows. The space is passed as
        // resource/context so the per-row "approve" action pre-fills param_space; the agent
        // and role template come from the row via the view's query mappings. postPublishTab
        // returns the approver to the About tab, where the approved member now shows.
        View nonApprovedView = View.get(NON_APPROVED_VIEW);
        String nonApprovedRefRoot = space.getRefRootId();
        QueryRef nonApprovedQuery = (nonApprovedRefRoot != null && !nonApprovedRefRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_NON_APPROVED_REF, "root_np", nonApprovedRefRoot)
                : new QueryRef(QueryApiAccess.LIST_SPACE_NON_APPROVED_REF);
        add(QueryResultTableBuilder.create("pendingmembers", nonApprovedQuery, new ViewDisplay(nonApprovedView)).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").build());

        View observersView = View.get(OBSERVERS_VIEW);
        // Drive the Observers table from the ref-scoped query that also includes un-introduced
        // self-declared observers (flagged via the headerless ⚠️ column), instead of the view's
        // own validated-only query. The view nanopub is left untouched. Falls back to the view's
        // IRI-keyed query when the ref root is unknown (pre-v3 data). See docs/space-ref-identity.md.
        String observersRefRoot = space.getRefRootId();
        QueryRef observersQuery = (observersRefRoot != null && !observersRefRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_OBSERVERS_REF, "root_np", observersRefRoot)
                : new QueryRef(observersView.getQuery().getQueryId(), "space", space.getId());
        add(QueryResultTableBuilder.create("observers", observersQuery, new ViewDisplay(observersView)).build());

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
