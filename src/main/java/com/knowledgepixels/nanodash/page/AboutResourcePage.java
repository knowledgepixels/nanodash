package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.extra.services.QueryRef;

import java.util.List;

/**
 * Standalone "About" page for a maintained resource, showing its views and
 * references. Reachable by direct URL only; not yet linked from the main
 * {@link MaintainedResourcePage}.
 * <p>
 * Resource-level members/roles are intentionally not shown: the domain model
 * has no per-resource membership (roles/members live on the parent
 * {@link Space}). See issue #478 for the planned follow-up.
 */
public class AboutResourcePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/resourceabout";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private final String resourceId;
    private final IModel<MaintainedResource> resourceModel;

    /**
     * Constructor for the AboutResourcePage.
     *
     * @param parameters the page parameters, must include "id" with the resource IRI
     */
    public AboutResourcePage(final PageParameters parameters) {
        super(parameters);

        MaintainedResource resource = MaintainedResourceRepository.get().findById(parameters.get("id").toString());
        if (resource == null) {
            throw new IllegalArgumentException("No maintained resource found for id: " + parameters.get("id"));
        }
        resourceId = resource.getId();
        resourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                return MaintainedResourceRepository.get().findById(resourceId);
            }
        };
        Space space = resource.getSpace();
        resource.triggerDataUpdate();

        List<AbstractResourceWithProfile> superSpaces = resource.getAllSuperSpacesUntilRoot();
        superSpaces.add(resource.getSpace());
        superSpaces.add(resource);
        add(new TitleBar("titlebar", this, null,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ));

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        add(new Label("pagetitle", resource.getLabel() + " (about) | nanodash"));
        add(new Label("resourcename", resource.getLabel()));
        add(new ExternalLinkWithActionsPanel("id", Model.of(resource.getId()), Model.of(resource.getLabel()), Values.iri(resource.getNanopubId())));

        String namespaceUri = resource.getNamespace() == null ? "" : resource.getNamespace();
        add(new BookmarkablePageLink<Void>("namespace", ExplorePage.class, new PageParameters().set("id", namespaceUri)).setBody(Model.of(namespaceUri)));

        // Pointer to the maintaining space (membership lives there, not on the resource).
        if (space != null) {
            add(new BookmarkablePageLink<Void>("space", SpacePage.class, new PageParameters().set("id", space.getId())).setBody(Model.of(space.getLabel())));
        } else {
            add(new BookmarkablePageLink<Void>("space", SpacePage.class).setVisible(false));
        }

        add(new DownloadRdfLinks("download-rdf", "resource", resource.getId()));

        // Assigned view displays (a listing of the configured view displays,
        // not the rendered views themselves).
        View vdView = View.get(AboutSpacePage.VIEW_DISPLAYS_VIEW);
        QueryRef vdQueryRef = new QueryRef(vdView.getQuery().getQueryId(), "resource", resource.getId());
        add(QueryResultTableBuilder.create("viewdisplays", vdQueryRef, new ViewDisplay(vdView)).build());

        // References
        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        QueryRef refQueryRef = new QueryRef(refView.getQuery().getQueryId(), "ref", resource.getId());
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetach() {
        resourceModel.detach();
        super.onDetach();
    }

}
