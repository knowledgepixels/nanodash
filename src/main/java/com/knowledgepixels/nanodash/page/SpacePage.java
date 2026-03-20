package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.*;
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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.QueryRef;

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
     * Space object with the data shown on this page.
     */
    private final Space space;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor for the SpacePage.
     *
     * @param parameters the page parameters
     */
    public SpacePage(final PageParameters parameters) {
        super(parameters);

        space = resolveSpace(parameters);
        space.triggerDataUpdate();

        Nanopub np = space.getNanopub();

        List<AbstractResourceWithProfile> superSpaces = space.getAllSuperSpacesUntilRoot();
        if (superSpaces.isEmpty()) {
            add(new TitleBar("titlebar", this, null,
                    new NanodashPageRef(SpacePage.class, new PageParameters().add("id", space.getId()), space.getLabel())
            ));
        } else {
            superSpaces.add(space);
            add(new TitleBar("titlebar", this, null,
                    superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
            ));
        }

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new ExternalLinkWithActionsPanel("id", Model.of(space.getId()), Model.of(space.getLabel()), np.getUri()));

        boolean isAdmin = SpaceMemberRole.isCurrentUserAdmin(space);
        add(new AddViewDisplayButton("addviewdisplay",
                "https://w3id.org/np/RAwPPxDxkXwgWwYhmvzi6SUs8djPZS4IgWJdp2G0blqoQ",
                "latest",
                space.getId(),
                space.getId(),
                new PageParameters()
                        .set("param_appliesToResource", space.getId())
                        .set("refresh-upon-publish", space.getId())
        ).setVisible(isAdmin));

        add(new ItemListPanel<String>(
                "altids",
                "Alternative IDs:",
                space.getAltIDs(),
                i -> new ExternalLinkWithActionsPanel("item", Model.of(i), Model.of(i))
        ));

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
            add(new Label("date", dateString));
        } else {
            add(new Label("date").setVisible(false));
        }

        add(new Label("description", "<span>" + Utils.sanitizeHtml(space.getDescription()) + "</span>").setEscapeModelStrings(false));

        if (space.isDataInitialized()) {
            add(new PinGroupList("pinned-section", space));
        } else {
            add(new AjaxLazyLoadPanel<Component>("pinned-section") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new PinGroupList(markupId, space);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id).setVisible(false);
                }

            });
        }

        if (space.isDataInitialized()) {
            add(new ViewList("views", space));
        } else {
            add(new AjaxLazyLoadPanel<Component>("views") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ViewList(markupId, space);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                }

            });
        }

        add(new ItemListPanel<>(
                        "roles",
                        "Roles:",
                        space::isDataInitialized,
                        space::getRoles,
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
            add(new SpaceUserList("user-lists", space));
        } else {
            add(new AjaxLazyLoadPanel<Component>("user-lists") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new SpaceUserList(markupId, space);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

            });
        }

        addSubspacePanel("Alliance", true);
        addSubspacePanel("Consortium", false);
        addSubspacePanel("Organization", true);
        addSubspacePanel("Taskforce", false);
        addSubspacePanel("Division", true);
        addSubspacePanel("Taskunit", false);
        addSubspacePanel("Group", true);
        addSubspacePanel("Project", false);
        addSubspacePanel("Program", true);
        addSubspacePanel("Initiative", false);
        addSubspacePanel("Outlet", true);
        addSubspacePanel("Campaign", false);
        addSubspacePanel("Community", true);
        addSubspacePanel("Event", false);

        add(new ItemListPanel<MaintainedResource>(
                "resources",
                "📦 Maintained Resources",
                new QueryRef(QueryApiAccess.GET_MAINTAINED_RESOURCES),
                (apiResponse) -> {
                    MaintainedResourceRepository.get().ensureLoaded();
                    return MaintainedResourceRepository.get().findResourcesBySpace(space);
                },
                (resource) -> new ItemListElement("item", MaintainedResourcePage.class, new PageParameters().set("id", resource.getId()), resource.getLabel())
        )
                .setResourceWithProfile(space)
                .setReadyFunction(space::isDataInitialized)
                .addMemberButton("+", PublishPage.class, new PageParameters()
                        .set("template", "https://w3id.org/np/RA25VaVFxSOgKEuZ70gFINn-N3QV4Pf62-IMK_SWkg-c8")
                        .set("param_space", space.getId())
                        .set("context", space.getId())
                        .set("refresh-upon-publish", "maintainedResources")
                        .set("template-version", "latest")
                ));

        String shortId = space.getId().replace("https://w3id.org/spaces/", "");
        ConnectorConfig cc = ConnectorConfig.get(shortId);
        if (cc != null) {
            add(new BookmarkablePageLink<Void>("content-button", GenOverviewPage.class, new PageParameters().set("journal", shortId)).setBody(Model.of("Nanopublication Submissions")));
        } else {
            add(new Label("content-button").setVisible(false));
        }
    }

    private void addSubspacePanel(String type, boolean openEnded) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");

        add(new ItemListPanel<>(
                        typePl.toLowerCase(),
                        Space.getTypeEmoji(type) + " " + typePl,
                        SpaceRepository.get().findSubspaces(space, KPXL_TERMS.NAMESPACE + type),
                        (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().set("id", space), space.getLabel())
                )
                        .setResourceWithProfile(space)
                        .setReadyFunction(space::isDataInitialized)
                        .addMemberButton("+", PublishPage.class, new PageParameters()
                                .set("template", openEnded ? "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ" : "https://w3id.org/np/RAaE7NP9RNIx03AHZxanFMdtUuaTfe50ns5tHhpEVloQ4")
                                .set("param_type", KPXL_TERMS.NAMESPACE + type)
                                .set("param_space", space.getId().replaceFirst("https://w3id.org/spaces/", "") + "/<SET-SUFFIX>")
                                .set("refresh-upon-publish", "spaces")
                                .set("template-version", "latest"))
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
     * Resolves the {@link Space} from the repository, or redirects as needed.
     *
     * @param parameters page parameters containing the space {@code id}
     * @return the resolved {@link Space}; never {@code null}
     * @throws RestartResponseException if the id belongs to a {@link MaintainedResource}
     * @throws IllegalArgumentException if the id cannot be resolved to any known resource
     */
    private Space resolveSpace(PageParameters parameters) {
        String id = parameters.get("id").toString();
        Space resolved = SpaceRepository.get().findById(id);
        if (resolved == null) {
            if (MaintainedResourceRepository.get().findById(id) != null) {
                throw new RestartResponseException(MaintainedResourcePage.class, parameters);
            }
            throw new IllegalArgumentException("No space or resource found for id: " + id);
        }

        return resolved;
    }

}
