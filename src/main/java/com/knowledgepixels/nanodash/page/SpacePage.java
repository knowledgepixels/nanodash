package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.component.menu.SpaceExploreMenu;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
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
import org.nanopub.Nanopub;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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

        Nanopub np = space.getNanopub();

        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);

        List<AbstractResourceWithProfile> superSpaces = space.getAllSuperSpacesUntilRoot();
        if (superSpaces.isEmpty()) {
            add(new TitleBar("titlebar", this, null,
                    new NanodashPageRef(SpacePage.class, new PageParameters().add("id", space.getId()), space.getLabel())
            ).setTabs(new ResourceTabs("tabs", "space", space.getId(), activeTab)));
        } else {
            superSpaces.add(space);
            add(new TitleBar("titlebar", this, null,
                    superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
            ).setTabs(new ResourceTabs("tabs", "space", space.getId(), activeTab)));
        }

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new ExternalLinkWithActionsPanel("id", Model.of(space.getId()), Model.of(space.getLabel()),
                new SpaceExploreMenu("np", space.getId(), space.getLabel(), np.getUri(), space)));

        add(new ItemListPanel<String>(
                "altids",
                "Alternative IDs:",
                space.getAltIDs(),
                i -> new ExternalLinkWithActionsPanel("item", Model.of(i), Model.of(i))
        ));

        WebMarkupContainer contentContainer = new WebMarkupContainer("contentContainer");
        add(contentContainer);
        if (activeTab != ResourceTabs.Tab.CONTENT) {
            contentContainer.setVisible(false);
            if (activeTab == ResourceTabs.Tab.ABOUT) {
                add(new AboutSpacePanel("otherTab", space));
            } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
                add(new ExplorePanel("otherTab", space.getId()));
            } else {
                add(new DownloadRdfLinks("otherTab", "space", space.getId()));
            }
            return;
        }
        add(new EmptyPanel("otherTab").setVisible(false));

        if (space.getStartDate() != null) {
            ZoneId startZone = space.getStartDate().getTimeZone().toZoneId();
            ZonedDateTime startDt = ZonedDateTime.ofInstant(space.getStartDate().toInstant(), startZone);
            String dateString = DATE_FORMATTER.format(startDt);
            if (space.getEndDate() != null) {
                ZoneId endZone = space.getEndDate().getTimeZone().toZoneId();
                ZonedDateTime endDt = ZonedDateTime.ofInstant(space.getEndDate().toInstant(), endZone);
                String endDateStr = DATE_FORMATTER.format(endDt);
                if (dateString.equals(endDateStr)) {
                    String tzAbbr = startDt.getZone().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
                    dateString += " " + TIME_FORMATTER.format(startDt) + " - " + TIME_FORMATTER.format(endDt) + " " + tzAbbr;
                } else {
                    dateString += " - " + endDateStr;
                }
            }
            contentContainer.add(new Label("date", dateString));
        } else {
            contentContainer.add(new Label("date").setVisible(false));
        }

        contentContainer.add(new Label("description", "<span>" + Utils.sanitizeHtml(space.getDescription()) + "</span>").setEscapeModelStrings(false));

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

        contentContainer.add(new ItemListPanel<>(
                        "roles",
                        "Roles:",
                        () -> spaceModel.getObject().isDataInitialized(),
                        () -> spaceModel.getObject().getRoles(),
                        r -> new ItemListElement("item", ExplorePage.class, new PageParameters().set("id", r.getRole().getId()), r.getRole().getName(), null, Utils.getAsNanopub(r.getNanopubUri()))
                )
                        .makeInline()
                        .setResourceWithProfile(space)
                        .addAdminButton("+", PublishPage.class, new PageParameters()
                                .set("template", "https://w3id.org/np/RARBzGkEqiQzeiHk0EXFcv9Ol1d-17iOh9MoFJzgfVQDc")
                                .set("param_space", space.getId())
                                .set("refresh-upon-publish", space.getId())
                                .set("template-version", "latest")
                        )
        );

        if (space.isDataInitialized()) {
            contentContainer.add(new SpaceUserList("user-lists", space));
        } else {
            contentContainer.add(new AjaxLazyLoadPanel<Component>("user-lists") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new SpaceUserList(markupId, spaceModel.getObject());
                }

                @Override
                protected boolean isContentReady() {
                    return spaceModel.getObject().isDataInitialized();
                }

            });
        }

        addSubspacePanel(contentContainer, "Alliance");
        addSubspacePanel(contentContainer, "Consortium");
        addSubspacePanel(contentContainer, "Organization");
        addSubspacePanel(contentContainer, "Taskforce");
        addSubspacePanel(contentContainer, "Division");
        addSubspacePanel(contentContainer, "Taskunit");
        addSubspacePanel(contentContainer, "Group");
        addSubspacePanel(contentContainer, "Project");
        addSubspacePanel(contentContainer, "Program");
        addSubspacePanel(contentContainer, "Initiative");
        addSubspacePanel(contentContainer, "Outlet");
        addSubspacePanel(contentContainer, "Campaign");
        addSubspacePanel(contentContainer, "Community");
        addSubspacePanel(contentContainer, "Event");

        contentContainer.add(new ItemListPanel<MaintainedResource>(
                "resources",
                "📦 Maintained Resources",
                () -> true,
                () -> MaintainedResourceRepository.get().findResourcesBySpace(spaceModel.getObject()),
                (resource) -> new ItemListElement("item", MaintainedResourcePage.class, new PageParameters().set("id", resource.getId()), resource.getLabel())
        ));

        String shortId = space.getId().replace("https://w3id.org/spaces/", "");
        ConnectorConfig cc = ConnectorConfig.get(shortId);
        if (cc != null) {
            contentContainer.add(new BookmarkablePageLink<Void>("content-button", GenOverviewPage.class, new PageParameters().set("journal", shortId)).setBody(Model.of("Nanopublication Submissions")));
        } else {
            contentContainer.add(new Label("content-button").setVisible(false));
        }
    }

    private void addSubspacePanel(WebMarkupContainer container, String type) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");

        container.add(new ItemListPanel<>(
                        typePl.toLowerCase(),
                        Space.getTypeEmoji(type) + " " + typePl,
                        SpaceRepository.get().findSubspaces(spaceModel.getObject(), KPXL_TERMS.NAMESPACE + type),
                        (subspace) -> new ItemListElement("item", SpacePage.class, new PageParameters().set("id", subspace), subspace.getLabel())
                )
        );
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
