package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.util.Values;

import java.util.List;

/**
 * This class represents a page for a maintained resource.
 */
public class MaintainedResourcePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/resource";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Id of the maintained resource shown on this page. Only the id is held in
     * the page state; the {@link MaintainedResource} itself is re-fetched from
     * the repository on every render via {@link #resourceModel}, so the page
     * tree never carries a serialized snapshot of singleton data.
     */
    private final String resourceId;

    /**
     * LDM that resolves {@link #resourceId} to the live {@link MaintainedResource} singleton.
     */
    private final IModel<MaintainedResource> resourceModel;

    public MaintainedResourcePage(final PageParameters parameters) {
        super(parameters);

        MaintainedResource resource = MaintainedResourceRepository.get().findById(parameters.get("id").toString());
        resourceId = resource.getId();
        resourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                return MaintainedResourceRepository.get().findById(resourceId);
            }
        };
        resource.triggerDataUpdate();

        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);

        List<AbstractResourceWithProfile> superSpaces = resource.getAllSuperSpacesUntilRoot();
        superSpaces.add(resource.getSpace());
        superSpaces.add(resource);
        add(new TitleBar("titlebar", this, null,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ).setTabs(new ResourceTabs("tabs", "resource", resource.getId(), activeTab)));

        add(new Label("pagetitle", resource.getLabel() + " (resource) | nanodash"));
        add(new Label("resourcename", resource.getLabel()));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
        add(new ExternalLinkWithActionsPanel("id", Model.of(resource.getId()), Model.of(resource.getLabel()), Values.iri(resource.getNanopubId())));

        String namespaceUri = resource.getNamespace() == null ? "" : resource.getNamespace();
        add(new BookmarkablePageLink<Void>("namespace", ExplorePage.class, new PageParameters().set("id", namespaceUri)).setBody(Model.of(namespaceUri)));


        WebMarkupContainer contentContainer = new WebMarkupContainer("contentContainer");
        add(contentContainer);
        if (activeTab == ResourceTabs.Tab.CONTENT) {
            add(new EmptyPanel("otherTab").setVisible(false));
            if (resource.isDataInitialized()) {
                contentContainer.add(new ViewList("views", resource));
            } else {
                contentContainer.add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, resourceModel.getObject());
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceModel.getObject().isDataInitialized();
                    }

                    @Override
                    public Component getLoadingComponent(String id) {
                        return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                    }

                });
            }
        } else {
            contentContainer.setVisible(false);
            if (activeTab == ResourceTabs.Tab.ABOUT) {
                add(new AboutResourcePanel("otherTab", resource));
            } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
                add(new ExplorePanel("otherTab", resource.getId()));
            } else {
                add(new DownloadRdfLinks("otherTab", "resource", resource.getId()));
            }
        }
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
