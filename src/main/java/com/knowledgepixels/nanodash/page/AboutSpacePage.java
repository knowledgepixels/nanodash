package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

/**
 * Standalone "About" page for a space, showing a listing of its assigned view
 * displays and references. Reachable by direct URL only; not yet linked from
 * the main {@link SpacePage}.
 */
public class AboutSpacePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/spaceabout";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * View that lists all assigned view displays of a resource (built on the
     * get-view-displays query Nanodash uses internally). Shown on About pages
     * instead of rendering the assigned views themselves.
     */
    public static final String VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RA-o92qp6rr50wsMSfwy-HUNEkHiCdBR6nTMjSQs3wvII/view-displays-view";

    /**
     * View listing the presets assigned to a resource (issue #302). Shown on
     * About pages just before the view displays listing.
     */
    public static final String PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RAlcZ2FdqNVFqbxtzs1o2NPWmOhL8hqnokHm7mbjo8KuQ/preset-assignments-view";

    /**
     * View listing a space's assigned roles, built on the existing
     * get-space-roles query.
     */
    public static final String SPACE_ROLES_VIEW = "https://w3id.org/np/RAsH9ItKDb5sRdMul-dTT-Dqb7u4u80RmeYUndZKyjGZ8/space-roles-view";

    /**
     * Constructor for the AboutSpacePage.
     *
     * @param parameters the page parameters, must include "id" with the space IRI
     */
    public AboutSpacePage(final PageParameters parameters) {
        super(parameters);

        Space space = SpaceRepository.get().findById(parameters.get("id").toString());
        if (space == null) {
            throw new IllegalArgumentException("No space found for id: " + parameters.get("id"));
        }

        List<AbstractResourceWithProfile> superSpaces = space.getAllSuperSpacesUntilRoot();
        superSpaces.add(space);
        add(new TitleBar("titlebar", this, null,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ));

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        add(new Label("pagetitle", space.getLabel() + " (about) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new ExternalLinkWithActionsPanel("id", Model.of(space.getId()), Model.of(space.getLabel())));
        add(new DownloadRdfLinks("download-rdf", "space", space.getId()));

        // Assigned roles (rendered as a view, using the existing get-space-roles query).
        View rolesView = View.get(SPACE_ROLES_VIEW);
        QueryRef rolesQueryRef = new QueryRef(rolesView.getQuery().getQueryId(), "space", space.getId());
        add(QueryResultTableBuilder.create("roles", rolesQueryRef, new ViewDisplay(rolesView)).build());

        // Assigned presets (issue #302).
        View presetsView = View.get(PRESET_ASSIGNMENTS_VIEW);
        QueryRef presetsQueryRef = new QueryRef(presetsView.getQuery().getQueryId(), "resource", space.getId());
        add(QueryResultTableBuilder.create("presets", presetsQueryRef, new ViewDisplay(presetsView)).build());

        // Assigned view displays (a listing of the configured view displays,
        // not the rendered views themselves; includes preset-supplied views).
        View vdView = View.get(VIEW_DISPLAYS_VIEW);
        QueryRef vdQueryRef = new QueryRef(vdView.getQuery().getQueryId(), "resource", space.getId());
        add(QueryResultTableBuilder.create("viewdisplays", vdQueryRef, new ViewDisplay(vdView)).build());

        // References
        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        QueryRef refQueryRef = new QueryRef(refView.getQuery().getQueryId(), "ref", space.getId());
        add(QueryResultTableBuilder.create("references", refQueryRef, new ViewDisplay(refView)).build());
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
