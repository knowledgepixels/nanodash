package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.util.Values;

import java.util.List;
import java.util.Optional;

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

    @Override
    public String getContextId() {
        return resourceId;
    }

    @Override
    public boolean isContextPage() {
        return true;
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

        WebMarkupContainer contentContainer = new WebMarkupContainer("contentContainer");
        add(contentContainer);
        if (activeTab == ResourceTabs.Tab.CONTENT) {
            add(new EmptyPanel("otherTab").setVisible(false));
            if (resource.isDataInitialized()) {
                boolean empty = resource.getTopLevelViewDisplays().isEmpty();
                if (empty) {
                    contentContainer.add(new WebMarkupContainer("views").setVisible(false));
                } else {
                    contentContainer.add(new ViewList("views", resource));
                }
                addUnconfiguredFallback(contentContainer, resource, empty);
            } else {
                // Data not yet loaded: render the views lazily, then reveal the unconfigured
                // notice + general-info fallback once we know whether any views exist.
                final WebMarkupContainer unconfiguredNotice = new WebMarkupContainer("unconfigured-notice");
                unconfiguredNotice.setVisible(false);
                unconfiguredNotice.setOutputMarkupPlaceholderTag(true);
                contentContainer.add(unconfiguredNotice);

                final ViewList generalInfoView = new ViewList("generalinfoview", resource, List.of(generalInfoViewDisplay()));
                generalInfoView.setVisible(false);
                generalInfoView.setOutputMarkupPlaceholderTag(true);
                contentContainer.add(generalInfoView);

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

                    @Override
                    protected void onContentLoaded(Component content, Optional<AjaxRequestTarget> target) {
                        super.onContentLoaded(content, target);
                        target.ifPresent(t -> {
                            boolean isEmpty = resourceModel.getObject().getTopLevelViewDisplays().isEmpty();
                            if (isEmpty) {
                                t.appendJavaScript("document.getElementById('" + getMarkupId() + "').remove();");
                            }
                            unconfiguredNotice.setVisible(isEmpty);
                            t.add(unconfiguredNotice);
                            generalInfoView.setVisible(isEmpty);
                            t.add(generalInfoView);
                        });
                    }

                });
            }
        } else {
            contentContainer.setVisible(false);
            if (activeTab == ResourceTabs.Tab.ABOUT) {
                // The panel constructor resolves view nanopubs over the network when
                // they aren't freshly cached, which would block the initial page
                // render; the view-id list must mirror the panel's View.get calls.
                add(LazyContentPanel.of("otherTab", markupId -> new AboutResourcePanel(markupId, resourceModel.getObject()),
                        AboutResourcePanel.MAINTAINED_RESOURCE_INFO_VIEW, AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW, AboutSpacePanel.VIEW_DISPLAYS_VIEW));
            } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
                add(LazyContentPanel.of("otherTab", markupId -> new ExplorePanel(markupId, resourceId),
                        ReferencesPage.REFERENCES_VIEW));
            } else {
                add(new DownloadRdfLinks("otherTab", "resource", resource.getId()));
            }
        }
    }

    /**
     * View shown as a general-information fallback on pages that have no view displays
     * configured yet.
     */
    private static final String GENERAL_INFO_VIEW = "https://w3id.org/np/RA9FKwtTWqknnDrFz1vMNeUdWpYYVQp7sznigpaXlBrU8/resource-info-view";

    private static ViewDisplay generalInfoViewDisplay() {
        return new ViewDisplay(View.get(GENERAL_INFO_VIEW));
    }

    /**
     * Adds the "page not configured yet" notice and the general-information fallback view,
     * both visible only when the resource has no view displays.
     */
    private void addUnconfiguredFallback(WebMarkupContainer contentContainer, AbstractResourceWithProfile resource, boolean empty) {
        contentContainer.add(new WebMarkupContainer("unconfigured-notice").setVisible(empty));
        if (empty) {
            contentContainer.add(new ViewList("generalinfoview", resource, List.of(generalInfoViewDisplay())));
        } else {
            contentContainer.add(new EmptyPanel("generalinfoview").setVisible(false));
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
