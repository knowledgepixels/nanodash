package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * The SpacePage class represents a space page in the Nanodash application.
 */
public class SpacePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/space";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Id of the space shown on this page. Only the id is held in the page
     * state; the {@link Space} itself is re-fetched from the repository on
     * every render via {@link #spaceModel}, so the page tree never carries
     * a serialized snapshot of singleton data.
     */
    private final String spaceId;

    /**
     * LDM that resolves {@link #spaceId} to the live {@link Space} singleton.
     */
    private final IModel<Space> spaceModel;

    /**
     * Constructor for the SpacePage.
     *
     * @param parameters the page parameters
     */
    public SpacePage(final PageParameters parameters) {
        super(parameters);

        Space space = resolveSpace(parameters);
        spaceId = space.getId();
        spaceModel = new LoadableDetachableModel<Space>() {
            @Override
            protected Space load() {
                return SpaceRepository.get().findById(spaceId);
            }
        };
        space.triggerDataUpdate();

        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);

        // Optional ?root=<NPID> pins the page to a specific ref (one of the IRI's claimants);
        // validated against the known refs. null = the representative (default) ref. Carried
        // across tab switches so the pinned ref survives navigation.
        String rootParam = parameters.get("root").toString(null);
        final String effectiveRoot = (rootParam != null && !rootParam.isEmpty()
                && space.getRefRoots().contains(rootParam)) ? rootParam : null;

        List<AbstractResourceWithProfile> superSpaces = space.getAllSuperSpacesUntilRoot();
        if (superSpaces.isEmpty()) {
            // Top-level space (no superspace): show only the tab strip, no breadcrumb.
            add(new TitleBar("titlebar", this, null)
                    .setTabs(new ResourceTabs("tabs", "space", space.getId(), null, activeTab, effectiveRoot)));
        } else {
            superSpaces.add(space);
            add(new TitleBar("titlebar", this, null,
                    superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
            ).setTabs(new ResourceTabs("tabs", "space", space.getId(), null, activeTab, effectiveRoot)));
        }

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
        add(new ExternalLinkWithActionsPanel("id", Model.of(space.getId()), Model.of(space.getLabel())));

        // Disambiguation notice. Three cases: viewing a specific claimant (ref pinned),
        // the default view of an identifier with conflicting claimants, or no notice.
        // Hidden on the claimants overview itself. See docs/space-ref-identity.md.
        boolean onClaimants = !parameters.get("claimants").isNull();
        String bannerText = "";
        if (!onClaimants && effectiveRoot != null) {
            bannerText = "You are viewing one of " + space.getRefCount()
                    + " definitions claiming this identifier (rooted at " + shortNp(effectiveRoot)
                    + "). Its members, roles, and sub-units below are scoped to this definition. ";
        } else if (!onClaimants && space.hasConflictingRefs()) {
            bannerText = "⚠ This space identifier is claimed by " + space.getRefCount()
                    + " competing definitions with different admins — showing the most recent one. ";
        }
        WebMarkupContainer refConflictNotice = new WebMarkupContainer("ref-conflict");
        refConflictNotice.setVisible(!bannerText.isEmpty());
        refConflictNotice.add(new Label("ref-conflict-text", bannerText));
        refConflictNotice.add(new BookmarkablePageLink<Void>("ref-conflict-link", SpacePage.class,
                new PageParameters().add("id", space.getId()).add("claimants", "true")));
        add(refConflictNotice);

        WebMarkupContainer contentContainer = new WebMarkupContainer("contentContainer");
        add(contentContainer);

        // Claimants overview: list all root definitions claiming this IRI (explicit
        // disambiguation), instead of the normal tab content.
        if (!parameters.get("claimants").isNull()) {
            contentContainer.add(new SpaceClaimantsPanel("views", space));
            add(new EmptyPanel("otherTab").setVisible(false));
            return;
        }
        if (activeTab != ResourceTabs.Tab.CONTENT) {
            contentContainer.setVisible(false);
            if (activeTab == ResourceTabs.Tab.ABOUT) {
                // The panel constructor resolves view nanopubs over the network when
                // they aren't freshly cached, which would block the initial page
                // render; the view-id list must mirror the panel's View.get calls.
                add(LazyContentPanel.of("otherTab", markupId -> new AboutSpacePanel(markupId, spaceModel.getObject(), effectiveRoot),
                        AboutSpacePanel.SPACE_INFO_VIEW, AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW, AboutSpacePanel.SPACE_ROLES_VIEW, AboutSpacePanel.VIEW_DISPLAYS_VIEW,
                        AboutSpacePanel.MEMBERS_VIEW, AboutSpacePanel.OBSERVERS_VIEW));
            } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
                add(LazyContentPanel.of("otherTab", markupId -> new ExplorePanel(markupId, spaceId),
                        ReferencesPage.REFERENCES_VIEW));
            } else {
                add(new DownloadRdfLinks("otherTab", "space", space.getId()));
            }
            return;
        }
        add(new EmptyPanel("otherTab").setVisible(false));

        if (space.isDataInitialized()) {
            contentContainer.add(new ViewList("views", space));
        } else {
            contentContainer.add(new AjaxLazyLoadPanel<Component>("views") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ViewList(markupId, spaceModel.getObject());
                }

                @Override
                protected boolean isContentReady() {
                    return spaceModel.getObject().isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                }

            });
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

    /** A short, human-scannable form of a nanopub IRI (its artifact-code prefix). */
    private static String shortNp(String npIri) {
        int i = Math.max(npIri.lastIndexOf('/'), npIri.lastIndexOf('#'));
        String code = i < 0 ? npIri : npIri.substring(i + 1);
        return code.length() > 16 ? code.substring(0, 16) + "…" : code;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetach() {
        spaceModel.detach();
        super.onDetach();
    }

    /**
     * Resolves the {@link Space} from the repository, or redirects as needed.
     *
     * @param parameters page parameters containing the space {@code id}
     * @return the resolved {@link Space}; never {@code null}
     * @throws RestartResponseException if the id belongs to a {@link MaintainedResource} or to a part within one
     * @throws IllegalArgumentException if the id cannot be resolved to any known resource
     */
    private Space resolveSpace(PageParameters parameters) {
        String id = parameters.get("id").toString();
        Space resolved = SpaceRepository.get().findById(id);
        if (resolved == null) {
            if (MaintainedResourceRepository.get().findById(id) != null) {
                throw new RestartResponseException(MaintainedResourcePage.class, parameters);
            }
            MaintainedResource containingResource = MaintainedResourceRepository.get().findByNamespace(MaintainedResource.getNamespace(id));
            if (containingResource != null) {
                PageParameters partParameters = new PageParameters(parameters);
                partParameters.set("context", containingResource.getId());
                throw new RestartResponseException(ResourcePartPage.class, partParameters);
            }
            throw new IllegalArgumentException("No space or resource found for id: " + id);
        }

        return resolved;
    }

}
