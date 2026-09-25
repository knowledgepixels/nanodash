package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewAnchors;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.SpaceMemberRoleRef;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * a single space-ref. It has to name a view, not the view's kind, which
     * {@link View#get(String)} can't load as a view.
     */
    public static final String SPACE_INFO_VIEW = "https://w3id.org/np/RAFXDMbb0zePnt3IWM_nj7MdzvOhZVoeTOtc8x2ltNg5E/space-info-view";

    /**
     * View that lists all assigned view displays of a resource (built on the
     * get-view-displays query Nanodash uses internally). Shown on About tabs
     * instead of rendering the assigned views themselves.
     */
    public static final String VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAXvb-hYpV2yY_4ifrxX3lIhXi_abcr-aFdH5zk6DjYWo/view-displays-view";

    /**
     * View listing the presets assigned to a resource (issue #302).
     */
    public static final String PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RAhYRi-oLmLALs9HYunZu73cQSh6Z-eLP6QGlUkLMwSL0/preset-assignments-view";

    /**
     * View listing a space's assigned roles as a table (role, schema:name, and a
     * count of how many of the space's users hold each role), built on the
     * list-space-roles query. The built-in Admin role is always the first row.
     */
    public static final String SPACE_ROLES_VIEW = "https://w3id.org/np/RAbUQYCEiYIWBmJ4W1OvD2fpTXxxhrBjusUlIVT9cFlTE/space-roles-view";

    /**
     * View listing a space's members (admins, maintainers, members) with their
     * highest role tier, built on the list-space-members query. Observer-tier
     * members are excluded.
     */
    public static final String MEMBERS_VIEW = "https://w3id.org/np/RAsv1Tede_234X-bmKeq38nepPfFzwuJg2_BDKLqo_CQ0/view";

    /**
     * View listing a space's non-approved role claims (agents holding an
     * admin/maintainer/member-tier role instantiation that is not in the
     * validated state — a self-assigned or otherwise ungranted claim awaiting
     * approval), built on the list-space-non-approved query. Carries a per-row
     * "approve" action (visible to members and above) that opens the pending
     * grant nanopub in derive mode under its own creation template
     * (approve_np:@derive-a grant_template:@template), so the approver
     * re-publishes the same assertion under their own key — for every tier,
     * not only admin grants (issue #603). Deliberately a FRESH view identity
     * (own kind, no npx:supersedes): views resolve to the latest version of
     * their chain, so continuing the old pending-members-view chain would
     * change behavior on deployed instances that still pin the old query;
     * this way they keep working with what they have.
     */
    public static final String NON_APPROVED_VIEW = "https://w3id.org/np/RA1eynkJJ4d3QVzThyuSWW8qSihyWFq5Gi8pOVREu3cHQ/pending-members-view";

    /**
     * View listing a space's observers (members whose highest tier is observer,
     * i.e. holding no admin/maintainer/member role), built on the
     * list-space-observers query.
     */
    public static final String OBSERVERS_VIEW = "https://w3id.org/np/RADW4rDxlnkF7cAInPjmUGnRBktsZkr46K9-6dkI0YsWE/view";

    /**
     * View listing a space's direct sub-spaces with their types, built on the
     * list-sub-spaces query.
     */
    public static final String SUB_SPACES_VIEW = "https://w3id.org/np/RAoO4uYnSJXCHr0F5uVC5sYpkD4HOh-lsHQj4k3epqeH0/sub-spaces-view";

    /**
     * View listing the resources maintained by a space, built on the
     * list-maintained-and-not-yet-listed-resources query. It lists first, marked as not
     * listed yet, the kinds of the definitions (views, templates and the like) whose versions
     * say they are governed by the space but that the space does not list as maintained
     * resources yet, so their gen:governedBy has no effect. Its admin actions on those rows
     * list one as a maintained resource or dismiss it (which only takes that version off the
     * list). The view is itself governed by knowledgepixels/nanodash, so any member of that
     * space can publish its next version.
     */
    public static final String MAINTAINED_RESOURCES_VIEW = "https://w3id.org/np/RAid3m--zOJ1OL4eNfDz3JpoSfhodtYsG2_1gz_cSrx48/maintained-resources-view";

    /**
     * Every view this panel resolves through {@link View#get(String)} when it is built.
     * The page gates on these: while any of them is unresolved the panel is built in a
     * follow-up Ajax request, so that resolving them cannot block the page render. Keeping
     * the list here, next to the constants it names, is what keeps it from drifting out of
     * step with the panel — a page that gates on the wrong ids either blocks on a view it
     * did not wait for, or waits forever for one this panel never resolves and so reloads
     * the tab on every visit.
     */
    public static final String[] REQUIRED_VIEWS = {
            SPACE_INFO_VIEW,
            PRESET_ASSIGNMENTS_VIEW,
            SPACE_ROLES_VIEW,
            VIEW_DISPLAYS_VIEW,
            MEMBERS_VIEW,
            NON_APPROVED_VIEW,
            OBSERVERS_VIEW,
            SUB_SPACES_VIEW,
            MAINTAINED_RESOURCES_VIEW,
    };

    /**
     * @param id    the Wicket markup id
     * @param space the space whose About listings to render
     */
    public AboutSpacePanel(String id, Space space) {
        this(id, space, null);
    }

    /**
     * @param id            the Wicket markup id
     * @param space         the space whose About listings to render
     * @param effectiveRoot the root nanopub of the specific ref to scope the ref-aware views
     *                      (Info, Observers) to, or null to use the representative ref. See
     *                      docs/space-ref-identity.md.
     */
    public AboutSpacePanel(String id, Space space, String effectiveRoot) {
        super(id);

        // The ref (root definition) every ref-scoped listing on this page is keyed to: the
        // pinned claimant when viewing one (?root=), otherwise this space's representative ref.
        // Falls back to null when no ref root is known (pre-v3 data), in which case each table
        // below uses its IRI-keyed query. See docs/space-ref-identity.md.
        final String refRoot = effectiveRoot != null ? effectiveRoot : space.getRefRootId();

        // This tab builds its view panels itself rather than going through ViewList, so it
        // hands out the section anchors itself too (see docs/section-anchors.md).
        ViewAnchors.Allocator anchors = new ViewAnchors.Allocator();

        // "Structure" section: key-value info, presets, assigned roles, view displays.

        // The info view leads the section (to the left of the presets). Its query is
        // scoped to a single space-ref, so it needs both the space IRI and the ref's
        // nanopub — bind them as separate params. When viewing a specific claimant the
        // effectiveRoot pins it to that ref's root definition.
        View infoView = View.get(SPACE_INFO_VIEW);
        Multimap<String, String> infoParams = ArrayListMultimap.create();
        infoParams.put("space", space.getId());
        infoParams.put("spaceNp", effectiveRoot != null ? effectiveRoot : space.getNanopubId());
        ViewDisplay infoDisplay = new ViewDisplay(infoView);
        add(anchors.anchor(QueryResultTableBuilder.create("info", new QueryRef(infoView.getQuery().getQueryId(), infoParams), infoDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).refRoot(refRoot).build(), infoDisplay));

        View presetsView = View.get(PRESET_ASSIGNMENTS_VIEW);
        QueryRef presetsQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_PRESET_ASSIGNMENTS_REF, "root_np", refRoot)
                : new QueryRef(presetsView.getQuery().getQueryId(), "resource", space.getId());
        ViewDisplay presetsDisplay = new ViewDisplay(presetsView);
        add(anchors.anchor(QueryResultTableBuilder.create("presets", presetsQuery, presetsDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).refRoot(refRoot).build(), presetsDisplay));

        View rolesView = View.get(SPACE_ROLES_VIEW);
        // Drive the roles table from the ref-scoped query (scoped by npa:forSpaceRef) so a
        // ?root=-pinned page shows only that ref's roles, falling back to the view's IRI-keyed
        // query when the ref root is unknown. The view nanopub is left untouched. Pass the space
        // as resource/context so the roles view's per-entry action button (publish a role
        // assignment) renders with param_space prefilled, mirroring the "+" button on the content
        // tab's role list. postPublishTab keeps the user on the About tab after publishing a
        // role/assignment, so they see the updated roles list (the presets/view-display views
        // intentionally fall through to the Content tab, where their effect shows).
        QueryRef rolesQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_ROLES_REF, "root_np", refRoot)
                : new QueryRef(rolesView.getQuery().getQueryId(), "space", space.getId());
        ViewDisplay rolesDisplay = new ViewDisplay(rolesView);
        add(anchors.anchor(QueryResultTableBuilder.create("roles", rolesQuery, rolesDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), rolesDisplay));

        // Sits above the view displays, which is what it is about: a view listed there shows
        // nothing until the role it asks for is attached, which the roles table below offers.
        add(roleWarning("role-warning", space, effectiveRoot, rolesView, rolesQuery));

        // The view nanopub's hasViewQuery is the ref-scoped list-view-displays query (a federated
        // join gating authorised signers on the ref's admins/maintainers via npa:forSpaceRef); supply
        // the pinned-or-representative ref as root_np. View displays aren't materialised into the
        // spaces repo, so this stays a federated join rather than a local lookup.
        View vdView = View.get(VIEW_DISPLAYS_VIEW);
        Multimap<String, String> vdParams = ArrayListMultimap.create();
        vdParams.put("resource", space.getId());
        if (refRoot != null && !refRoot.isEmpty()) vdParams.put("root_np", refRoot);
        ViewDisplay vdDisplay = new ViewDisplay(vdView);
        add(anchors.anchor(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), vdParams), vdDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).refRoot(refRoot).build(), vdDisplay));

        // "Users" section: admins/maintainers/members, then observers.

        // Drive the members table from the ref-scoped query (scoped by npa:forSpaceRef) so a
        // ?root=-pinned page shows only that ref's members, falling back to the view's IRI-keyed
        // query when the ref root is unknown. The view nanopub is left untouched.
        View membersView = View.get(MEMBERS_VIEW);
        QueryRef membersQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_MEMBERS_REF, "root_np", refRoot)
                : new QueryRef(membersView.getQuery().getQueryId(), "space", space.getId());
        ViewDisplay membersDisplay = new ViewDisplay(membersView);
        // resourceWithProfile/contextId let the view's per-row "revoke role" action gate on the
        // viewer's tier in this space and pre-fill param_space (issue #639); postPublishTab
        // returns the revoker to the About tab, where the shrunk listing shows.
        add(anchors.anchor(QueryResultTableBuilder.create("members", membersQuery, membersDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), membersDisplay));

        // Non-approved (pending) higher-tier role claims, between members and observers.
        // Ref-scoped (root_np), like the observers table below; there is no IRI-keyed
        // fallback query, so when the ref root is unknown (pre-v3 data) the table is driven
        // by the param-less query, which yields no rows. The per-row "approve" action opens
        // the pending grant in derive mode under its own creation template (the row's
        // approve_np/grant_template pair via the view's query mappings), so the approver
        // re-publishes the same assertion under their own key — which works for every tier
        // (issue #603). postPublishTab returns the approver to the About tab, where the
        // approved member now shows.
        View nonApprovedView = View.get(NON_APPROVED_VIEW);
        QueryRef nonApprovedQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_NON_APPROVED_REF, "root_np", refRoot)
                : new QueryRef(QueryApiAccess.LIST_SPACE_NON_APPROVED_REF);
        ViewDisplay nonApprovedDisplay = new ViewDisplay(nonApprovedView);
        add(anchors.anchor(QueryResultTableBuilder.create("pendingmembers", nonApprovedQuery, nonApprovedDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), nonApprovedDisplay));

        View observersView = View.get(OBSERVERS_VIEW);
        // Drive the Observers table from the ref-scoped query that also includes un-introduced
        // self-declared observers (flagged via the headerless ⚠️ column), instead of the view's
        // own validated-only query. The view nanopub is left untouched. Falls back to the view's
        // IRI-keyed query when the ref root is unknown (pre-v3 data). See docs/space-ref-identity.md.
        QueryRef observersQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SPACE_OBSERVERS_REF, "root_np", refRoot)
                : new QueryRef(observersView.getQuery().getQueryId(), "space", space.getId());
        ViewDisplay observersDisplay = new ViewDisplay(observersView);
        // Same action wiring as the members table above (issue #639).
        add(anchors.anchor(QueryResultTableBuilder.create("observers", observersQuery, observersDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), observersDisplay));

        // "Sub-units" section: sub-spaces and maintained resources, side by side
        // (both views declare 6/12 width). resourceWithProfile/id/contextId let the
        // views' "add" actions pre-fill the new sub-space's IRI under this space's
        // namespace and this space as the maintainer, respectively; postPublishTab
        // returns the user here, where the new entry shows up.

        // Both sub-unit tables are ref-scoped via the ref-level npa:hasSubSpace /
        // npa:hasMaintainedResource edges (subject = the ref), falling back to the IRI-keyed
        // listing when the ref root is unknown. The sub-spaces view nanopub is left untouched.
        View subSpacesView = View.get(SUB_SPACES_VIEW);
        QueryRef subSpacesQuery = (refRoot != null && !refRoot.isEmpty())
                ? new QueryRef(QueryApiAccess.LIST_SUB_SPACES_REF, "root_np", refRoot)
                : new QueryRef(subSpacesView.getQuery().getQueryId(), "space", space.getId());
        ViewDisplay subSpacesDisplay = new ViewDisplay(subSpacesView);
        add(anchors.anchor(QueryResultListBuilder.create("subspaces", subSpacesQuery, subSpacesDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), subSpacesDisplay));

        // The maintained-resources view's own query takes the space and, when known, the ref's
        // root nanopub, like the view displays above: it lists that ref's maintained resources
        // (every ref's when no root is given), preceded by the governed kinds the space doesn't
        // list as maintained resources yet, whose admin actions list or dismiss them.
        // postPublishTab returns the admin here, where a listed kind has lost its marker.
        View maintainedResourcesView = View.get(MAINTAINED_RESOURCES_VIEW);
        Multimap<String, String> maintainedResourcesParams = ArrayListMultimap.create();
        maintainedResourcesParams.put("space", space.getId());
        if (refRoot != null && !refRoot.isEmpty()) maintainedResourcesParams.put("root_np", refRoot);
        QueryRef maintainedResourcesQuery = new QueryRef(maintainedResourcesView.getQuery().getQueryId(), maintainedResourcesParams);
        ViewDisplay maintainedResourcesDisplay = new ViewDisplay(maintainedResourcesView);
        add(anchors.anchor(QueryResultListBuilder.create("maintainedresources", maintainedResourcesQuery, maintainedResourcesDisplay).resourceWithProfile(space).id(space.getId()).contextId(space.getId()).postPublishTab("about").refRoot(refRoot).build(), maintainedResourcesDisplay));
    }

    /**
     * A view that lists the holders of one role shows nothing at all until that role is
     * attached to the space, however many grants of it have been published — which reads as
     * an empty view rather than as a missing role (issue #648). This says so.
     *
     * @param id            the Wicket markup id
     * @param space         the space whose views and roles to compare
     * @param effectiveRoot the pinned ref's root nanopub, or null for the representative ref
     * @return the warning, invisible when every role the views are pinned to is attached
     */
    private WebMarkupContainer roleWarning(String id, Space space, String effectiveRoot, View rolesView, QueryRef rolesQuery) {
        List<UnattachedRole> unattached = unattachedRoles(space, effectiveRoot);
        WebMarkupContainer warning = new WebMarkupContainer(id);
        warning.setVisible(!unattached.isEmpty());
        warning.add(new ListView<UnattachedRole>("unattached-roles", unattached) {

            @Override
            protected void populateItem(ListItem<UnattachedRole> item) {
                UnattachedRole unattachedRole = item.getModelObject();
                item.add(new Label("view", unattachedRole.viewTitle()));
                BookmarkablePageLink<Void> roleLink = new BookmarkablePageLink<>("role", ExplorePage.class,
                        new PageParameters().add("id", unattachedRole.role()));
                roleLink.add(new Label("rolelabel", Utils.getShortNameFromURI(unattachedRole.role())));
                item.add(roleLink);
                item.add(attachRoleAction("attach", space, effectiveRoot, rolesView, rolesQuery, unattachedRole.role()));
            }

        });
        return warning;
    }

    /**
     * The roles listing's own "add role" action, pointed at the role the view is waiting for:
     * the same form, opened with the space and that role already filled in, so the warning
     * can be acted on where it is read. Invisible to a viewer the roles listing would not
     * offer the action to, and when the view declares none.
     *
     * @param id            the Wicket markup id
     * @param space         the space the role would be attached to
     * @param effectiveRoot the pinned ref's root nanopub, or null for the representative ref
     * @param rolesView     the view whose action this is
     * @param rolesQuery    the roles listing's query, refreshed once the role is attached
     * @param role          the role to attach
     * @return the link, or an invisible component when there is nothing to offer
     */
    private Component attachRoleAction(String id, Space space, String effectiveRoot, View rolesView, QueryRef rolesQuery, IRI role) {
        IRI actionIri = null;
        for (IRI candidate : rolesView.getViewResultActionList()) {
            if (rolesView.getTemplateForAction(candidate) != null) {
                actionIri = candidate;
                break;
            }
        }
        if (actionIri == null || !SpaceMemberRole.isViewerEntitled(rolesView.getActionVisibleTo(actionIri), space, effectiveRoot)) {
            return new WebMarkupContainer(id).setVisible(false);
        }
        Template template = rolesView.getTemplateForAction(actionIri);
        String targetField = rolesView.getTemplateTargetFieldForAction(actionIri);
        if (targetField == null) targetField = "resource";
        PageParameters params = new PageParameters()
                .set("template", template.getId())
                .set("template-version", "latest")
                .set("param_" + targetField, space.getId())
                .set("context", space.getId())
                .set("postpub-tab", "about")
                .set("refresh-upon-publish", rolesQuery.getAsUrlString());
        String roleField = roleFieldOf(template);
        if (roleField != null) params.set("param_" + roleField, role.stringValue());
        String label = rolesView.getLabelForAction(actionIri);
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, PublishPage.class, params);
        link.add(new Label("attachlabel", label == null ? "add role..." : label));
        return link;
    }

    /**
     * The name of the template field that takes the role, read off the statement that attaches
     * one ({@code ?space gen:hasRole ?role}), so that the form opens with the missing role
     * already chosen.
     *
     * @param template the role-attaching template
     * @return the field name, or null if the template attaches no role
     */
    private static String roleFieldOf(Template template) {
        for (IRI statement : template.getStatementIris()) {
            if (KPXL_TERMS.HAS_ROLE.equals(template.getPredicate(statement))
                    && template.getObject(statement) instanceof IRI placeholder
                    && template.isPlaceholder(placeholder)) {
                return Utils.getUriPostfix(placeholder);
            }
        }
        return null;
    }

    /**
     * Pairs every view shown for the space with the roles it is pinned to that the space has
     * not attached.
     *
     * @param space         the space whose views and roles to compare
     * @param effectiveRoot the pinned ref's root nanopub, or null for the representative ref
     * @return one entry per view and unattached role, in the order the views are shown
     */
    static List<UnattachedRole> unattachedRoles(Space space, String effectiveRoot) {
        Set<IRI> attached = new HashSet<>();
        for (SpaceMemberRoleRef role : space.getRoles()) {
            attached.add(role.getRole().getId());
        }
        List<UnattachedRole> unattached = new ArrayList<>();
        for (ViewDisplay display : space.getTopLevelViewDisplays(effectiveRoot)) {
            for (IRI role : display.getView().getPinnedRoles()) {
                if (!attached.contains(role)) {
                    unattached.add(new UnattachedRole(display.getView().getTitle(), role));
                }
            }
        }
        return unattached;
    }

    /**
     * One view of this space and one role it lists the holders of, which the space has not
     * attached.
     *
     * @param viewTitle the title the view is shown under
     * @param role      the role IRI the view's query is pinned to
     */
    record UnattachedRole(String viewTitle, IRI role) implements Serializable {
    }

}
