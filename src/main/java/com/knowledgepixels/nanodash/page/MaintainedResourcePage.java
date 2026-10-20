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
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.domain.ProfilePicture;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.Nanopub;

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

    /**
     * {@inheritDoc}
     * <p>
     * A resource with a profile picture is shown with it in a link preview (issue #804).
     */
    @Override
    protected String getMetaImageUrl() {
        String own = ownPicture();
        return own != null ? own : super.getMetaImageUrl();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A profile picture is a small square; stretching it into the wide card shape looks
     * worse than the small card it suits.
     */
    @Override
    protected boolean hasWideMetaImage() {
        return ownPicture() == null;
    }

    /**
     * This resource's own picture, where it has one that a link preview can use.
     *
     * @return the picture URL, or null
     */
    private String ownPicture() {
        MaintainedResource resource = resourceModel == null ? null : resourceModel.getObject();
        ProfilePicture picture = resource == null ? null : resource.getProfilePicture();
        return picture == null ? null : sharableImageUrl(picture.getSrc());
    }

    public MaintainedResourcePage(final PageParameters parameters) {
        super(parameters);

        MaintainedResource resource = resolveResource(parameters);
        resourceId = resource.getId();
        redirectIfRdfRequested(new RdfSource("resource", resourceId, null, List.of()));
        resourceModel = new LoadableDetachableModel<MaintainedResource>() {
            @Override
            protected MaintainedResource load() {
                // Last known, as in the constructor: the page's own renders must not start
                // handing out null halfway through an answer that came back short, which the
                // view list and the empty-state check would dereference (the same pairing
                // HomePage makes for its configured resource, issue #623).
                return MaintainedResourceRepository.get().findLastKnownById(resourceId);
            }
        };
        resource.triggerDataUpdate();

        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);

        List<AbstractResourceWithProfile> superSpaces = resource.getAllSuperSpacesUntilRoot();
        superSpaces.add(resource.getSpace());
        superSpaces.add(resource);
        add(new TitleBar("titlebar", this,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ).setTabs(new ResourceTabs("tabs", "resource", resource.getId(), activeTab)));

        add(new Label("pagetitle", resource.getLabel() + " (resource)" + titleSuffix()));
        // Optional profile picture, left of the title/URI block (issue #632). Shown
        // plainly, i.e. without the tilted-square mask that user icons get, and simply
        // omitted when the resource declares none.
        ProfilePicture profilePicture = resource.getProfilePicture();
        add(profilePicture != null
                ? new ExternalImage("profilepic", profilePicture.getSrc())
                : new WebMarkupContainer("profilepic").setVisible(false));
        add(new Label("resourcename", resource.getLabel()));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
        add(PageTitleMenu.forResource("titlemenu", resource));
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
                    contentContainer.add(RefreshingStructurePanel.of("views", resource,
                            markupId -> new ViewList(markupId, resourceModel)));
                }
                addUnconfiguredFallback(contentContainer, resourceModel, empty);
            } else {
                // Data not yet loaded: render the views lazily, then reveal the unconfigured
                // notice + general-info fallback once we know whether any views exist.
                final WebMarkupContainer unconfiguredNotice = new WebMarkupContainer("unconfigured-notice");
                unconfiguredNotice.setVisible(false);
                unconfiguredNotice.setOutputMarkupPlaceholderTag(true);
                contentContainer.add(unconfiguredNotice);

                final ViewList generalInfoView = new ViewList("generalinfoview", resourceModel, List.of(generalInfoViewDisplay()));
                generalInfoView.setVisible(false);
                generalInfoView.setOutputMarkupPlaceholderTag(true);
                contentContainer.add(generalInfoView);

                contentContainer.add(new LazyContentPanel("views", markupId -> new ViewList(markupId, resourceModel)) {

                    @Override
                    protected boolean isContentReady() {
                        return resourceModel.getObject().isDataInitialized();
                    }

                    @Override
                    public Component getLoadingComponent(String id) {
                        return new Label(id, ResultComponent.getSectionWaitHtml()).setEscapeModelStrings(false);
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
                        AboutResourcePanel.REQUIRED_VIEWS));
            } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
                add(LazyContentPanel.of("otherTab", markupId -> new ExplorePanel(markupId, resourceId),
                        ReferencesPage.REFERENCES_VIEW));
            } else {
                add(new DownloadLinks("otherTab", "resource", resource.getId()));
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
    private void addUnconfiguredFallback(WebMarkupContainer contentContainer, IModel<? extends AbstractResourceWithProfile> resource, boolean empty) {
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
     * <p>
     * The resource's declaring nanopublication describes it.
     */
    @Override
    protected RdfSource getRdfSource() {
        MaintainedResource resource = resourceModel.getObject();
        List<Nanopub> declarations = resource != null && resource.getNanopub() != null ? List.of(resource.getNanopub()) : List.of();
        return new RdfSource("resource", resourceId, null, declarations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetach() {
        resourceModel.detach();
        super.onDetach();
    }

    /**
     * The maintained resource this page is about, or a forward to wherever the given id does
     * belong. Callers normally check that the id names a maintained resource before linking
     * here, but a hand-typed or out-of-date address need not: one that names a space, or a term
     * inside a resource, is answered where it belongs, and one that names nothing known is
     * answered plainly rather than failing to build (the same treatment {@code SpacePage} gives
     * an unknown space, issue #284).
     *
     * @param parameters the page parameters, holding the {@code id}
     * @return the resource to show; never null
     * @throws RestartResponseException forwarding to the page that does answer the id, or to
     *                                  {@link ErrorPage} when nothing known goes by it
     */
    private MaintainedResource resolveResource(PageParameters parameters) {
        String id = parameters.get("id").toString("");
        if (id.isEmpty()) {
            throw new RestartResponseException(ErrorPage.class, new PageParameters()
                    .set(ErrorPage.MESSAGE_PARAM, "No resource was given to show.")
                    .set(ErrorPage.KIND_PARAM, ErrorPage.Kind.REQUEST.getParamValue()));
        }
        // Last known as well as current: a resource this instance has been shown does not stop
        // existing because one answer came back short (issue #623).
        MaintainedResource resource = MaintainedResourceRepository.get().findLastKnownById(id);
        if (resource != null) return resource;
        if (SpaceRepository.get().findByIdOrAltId(id) != null) {
            throw new RestartResponseException(SpacePage.class, parameters);
        }
        ResourcePartPage.forwardToContainingResource(parameters);
        throw new RestartResponseException(ErrorPage.class, new PageParameters()
                .set(ErrorPage.MESSAGE_PARAM, "No maintained resource is known under the id " + id + ".")
                .set(ErrorPage.KIND_PARAM, ErrorPage.Kind.REQUEST.getParamValue()));
    }

}
