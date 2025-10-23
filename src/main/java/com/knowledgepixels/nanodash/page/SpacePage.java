package com.knowledgepixels.nanodash.page;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.PinGroupList;
import com.knowledgepixels.nanodash.component.SpaceUserList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

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

        space = Space.get(parameters.get("id").toString());
        Nanopub np = space.getRootNanopub();

        add(new TitleBar("titlebar", this, "connectors"));

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", space.getLabel())).setBody(Model.of(space.getId())));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", np.getUri())));

        add(new ItemListPanel<String>(
                "altids",
                "Alternative IDs:",
                space.getAltIDs(),
                id -> new ItemListElement("item", ExplorePage.class, new PageParameters().add("id", id), id)
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
                .add("template", "https://w3id.org/np/RA2YwreWrGW9HkzWls8jgwaIINKUB5ZTli1aFKQt13dUk")
                .add("param_space", space.getId())
                .add("context", space.getId())
            );
        addPinnedTemplateButton.setBody(Model.of("+ template"));
        pinButtons.add(addPinnedTemplateButton);

        AbstractLink addPinnedQueryButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .add("template", "https://w3id.org/np/RAuLESdeRUlk1GcTwvzVXShiBMI0ntJs2DL2Bm5DzW_ZQ")
                .add("param_space", space.getId())
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
                };
    
            });
        }

        final List<AbstractLink> viewButtons = new ArrayList<>();
        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .add("template", "https://w3id.org/np/RA7vjbk3kz4FCu2eTX5oekZshPeOGNGTw8b2WLk8ZS7VI")
                .add("param_resource", space.getId())
                .add("context", space.getId())
            );
        addViewButton.setBody(Model.of("+"));
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
                };
    
            });
        }

        add(new ItemListPanel<>(
                "roles",
                "Roles:",
                () -> space.isDataInitialized(),
                () -> space.getRoles(),
                r -> new ItemListElement("item", ExplorePage.class, new PageParameters().add("id", r.getId()), r.getName())
            )
            .makeInline()
            .setSpace(space)
            .addAdminButton("+", PublishPage.class, new PageParameters()
                    .add("template", "https://w3id.org/np/RARBzGkEqiQzeiHk0EXFcv9Ol1d-17iOh9MoFJzgfVQDc")
                    .add("param_space", space.getId())
                    .add("template-version", "latest")
                )
            );

        add(new ItemListPanel<IRI>(
                "users",
                "Users",
                () -> space.isDataInitialized(),
                () -> space.getUsers(),
                m -> {
                        String roleLabel = "(";
                        for (SpaceMemberRole r : space.getMemberRoles(m)) {
                            roleLabel += r.getName() + ", ";
                        }
                        roleLabel = roleLabel.replaceFirst(", $", ")");
                        return new ItemListElement("item", UserPage.class, new PageParameters().add("id", m), User.getShortDisplayName(m), roleLabel);
                    }
            ));


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
                (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space), space.getLabel(), "(" + space.getTypeLabel() + ")")
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

        add(new ItemListPanel<>(
                "resources",
                "Resources",
                new QueryRef("get-maintained-resources", "space", space.getId()),
                (apiResponse) -> apiResponse.getData(),
                (entry) -> {
                    return new ItemListElement("item", MaintainedResourcePage.class, new PageParameters().add("id", entry.get("resource")), entry.get("label"));
                }
            )
            .setSpace(space)
            .setReadyFunction(space::isDataInitialized)
            .addMemberButton("+", PublishPage.class, new PageParameters()
                    .set("template", "https://w3id.org/np/RA25VaVFxSOgKEuZ70gFINn-N3QV4Pf62-IMK_SWkg-c8")
                    .set("param_space", space.getId())
                    .set("context", space.getId())
                    .set("template-version", "latest")
                ));

        String shortId = space.getId().replace("https://w3id.org/spaces/", "");
        ConnectorConfig cc = ConnectorConfig.get(shortId);
        if (cc != null) {
            add(new BookmarkablePageLink<Void>("content-button", GenOverviewPage.class, new PageParameters().add("journal", shortId)).setBody(Model.of("Nanopublication Submissions")));
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
                space.getSubspaces("https://w3id.org/kpxl/gen/terms/" + type),
                (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space), space.getLabel())
            )
            .setSpace(space)
            .setReadyFunction(space::isDataInitialized)
            .addMemberButton("+", PublishPage.class, new PageParameters()
                    .set("template", openEnded ? "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ" : "https://w3id.org/np/RAaE7NP9RNIx03AHZxanFMdtUuaTfe50ns5tHhpEVloQ4")
                    .set("param_type", "https://w3id.org/kpxl/gen/terms/" + type)
                    .set("param_space", space.getId().replaceFirst("https://w3id.org/spaces/", "") + "/<...>")
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
