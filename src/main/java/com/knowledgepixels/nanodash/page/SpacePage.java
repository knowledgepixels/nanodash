package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryRef;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The ProjectPage class represents a space page in the Nanodash application.
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
    private Space space;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Constructor for the SpacePage.
     *
     * @param parameters the page parameters
     * @throws org.nanopub.extra.services.FailedApiCallException if the API call fails
     */
    public SpacePage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        String id = parameters.get("id").toString();
        space = Space.get(id);
        if (space == null && MaintainedResource.get(id) != null) {
            throw new RestartResponseException(MaintainedResourcePage.class, parameters);
        }
        Nanopub np = space.getNanopub();

        List<ProfiledResource> superSpaces = space.getAllSuperSpacesUntilRoot();
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

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", space.getLabel())).setBody(Model.of(space.getId())));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", np.getUri())));

        add(new ItemListPanel<String>(
                "altids",
                "Alternative IDs:",
                space.getAltIDs(),
                i -> new ItemListElement("item", ExplorePage.class, new PageParameters().set("id", i), i)
        ));

        if (space.getStartDate() != null) {
            String dateString;
            LocalDateTime dt = LocalDateTime.ofInstant(space.getStartDate().toInstant(), ZoneId.systemDefault());
            dateString = dateTimeFormatter.format(dt);
            if (space.getEndDate() != null) {
                dt = LocalDateTime.ofInstant(space.getEndDate().toInstant(), ZoneId.systemDefault());
                String endDate = dateTimeFormatter.format(dt);
                if (!dateString.equals(endDate)) {
                    dateString += " - " + endDate;
                }
            }
            add(new Label("date", dateString));
        } else {
            add(new Label("date").setVisible(false));
        }

        add(new Label("description", "<span>" + Utils.sanitizeHtml(space.getDescription()) + "</span>").setEscapeModelStrings(false));

        final List<AbstractLink> pinButtons = new ArrayList<>();

        AbstractLink addPinnedTemplateButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .set("template", "https://w3id.org/np/RA2YwreWrGW9HkzWls8jgwaIINKUB5ZTli1aFKQt13dUk")
                .set("template-version", "latest")
                .set("param_space", space.getId())
                .set("context", space.getId())
        );
        addPinnedTemplateButton.setBody(Model.of("+ template"));
        pinButtons.add(addPinnedTemplateButton);

        AbstractLink addPinnedQueryButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .set("template", "https://w3id.org/np/RAuLESdeRUlk1GcTwvzVXShiBMI0ntJs2DL2Bm5DzW_ZQ")
                .set("template-version", "latest")
                .set("param_space", space.getId())
        );
        addPinnedQueryButton.setBody(Model.of("+ query"));
        pinButtons.add(addPinnedQueryButton);

        if (space.isDataInitialized()) {
            add(new PinGroupList("pin-groups", space));
            add(new ButtonList("pin-buttons", space, null, null, pinButtons));
        } else {
            add(new AjaxLazyLoadPanel<Component>("pin-groups") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new PinGroupList(markupId, space);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

            });
            add(new AjaxLazyLoadPanel<Component>("pin-buttons") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ButtonList(markupId, space, null, null, pinButtons);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

                public Component getLoadingComponent(String id) {
                    return new Label(id).setVisible(false);
                }

            });
        }

        final List<AbstractLink> viewButtons = new ArrayList<>();
        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .set("template", "https://w3id.org/np/RAxERE0cQ9jLQZ5VjeA-1v3XnE9ugxLpFG8vpkAd5FqHE")
                .set("template-version", "latest")
                .set("param_resource", space.getId())
                .set("context", space.getId())
        );
        addViewButton.setBody(Model.of("+ view"));
        viewButtons.add(addViewButton);

        if (space.isDataInitialized()) {
            add(new ViewList("views", space));
            add(new ButtonList("view-buttons", space, null, null, viewButtons));
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

            });
            add(new AjaxLazyLoadPanel<Component>("view-buttons") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ButtonList(markupId, space, null, null, viewButtons);
                }

                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }

                public Component getLoadingComponent(String id) {
                    return new Label(id).setVisible(false);
                }

            });
        }

        add(new ItemListPanel<>(
                        "roles",
                        "Roles:",
                        () -> space.isDataInitialized(),
                        () -> space.getRoles(),
                        r -> new ItemListElement("item", ExplorePage.class, new PageParameters().set("id", r.getRole().getId()), r.getRole().getName(), null, Utils.getAsNanopub(r.getNanopubUri()))
                )
                        .makeInline()
                        .setProfiledResource(space)
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

        add(new ItemListPanel<Space>(
                "superspaces",
                "Part of",
                space.getSuperspaces(),
                (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().set("id", space), space.getLabel(), "(" + space.getTypeLabel() + ")", null)
        ));

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
                "Resources",
                new QueryRef("get-maintained-resources"),
                (apiResponse) -> {
                    MaintainedResource.ensureLoaded();
                    return MaintainedResource.getResourcesBySpace(space);
                },
                (resource) -> {
                    return new ItemListElement("item", MaintainedResourcePage.class, new PageParameters().set("id", resource.getId()), resource.getLabel());
                }
        )
                .setProfiledResource(space)
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
                        typePl,
                        space.getSubspaces(KPXL_TERMS.NAMESPACE + type),
                        (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().set("id", space), space.getLabel())
                )
                        .setProfiledResource(space)
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

}
