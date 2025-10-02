package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * A page that lists all available connectors.
 */
public class SpaceListPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/spaces";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the SpaceListPage.
     *
     * @param parameters the page parameters
     */
    public SpaceListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "connectors"));

        addSpacePanel("Alliance", true);
        addSpacePanel("Consortium", false);
        addSpacePanel("Organization", true);
        addSpacePanel("Taskforce", false);
        addSpacePanel("Division", true);
        addSpacePanel("Taskunit", false);
        addSpacePanel("Group", true);
        addSpacePanel("Project", false);
        addSpacePanel("Program", true);
        addSpacePanel("Initiative", false);
        addSpacePanel("Outlet", true);
        addSpacePanel("Campaign", false);
        addSpacePanel("Community", true);
        addSpacePanel("Event", false);

        add(new ItemListPanel<Project>(
                "legacy-projects",
                "Legacy Projects",
                new QueryRef("get-projects"),
                (apiResponse) -> { Project.refresh(apiResponse); return Project.getProjectList(); },
                (project) -> {
                    return new ItemListElement("item", ProjectPage.class, new PageParameters().add("id", project.getId()), project.getLabel());
                }
            ).setDescription("These legacy project pages will be migrated into the Spaces above:"));
    }

    private void addSpacePanel(String type, boolean openEnded) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");

        PageParameters newLinkParams = new PageParameters()
                .add("param_type", "https://w3id.org/kpxl/gen/terms/" + type)
                .add("template-version", "latest")
                .add("postpub-redirect-url", MOUNT_PATH);
        if (openEnded) {
            newLinkParams.add("template", "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ");
        } else {
            newLinkParams.add("template", "https://w3id.org/np/RAaE7NP9RNIx03AHZxanFMdtUuaTfe50ns5tHhpEVloQ4");
        }

        add(new ItemListPanel<Space>(
            typePl.toLowerCase(),
            typePl,
            new QueryRef("get-spaces"),
            (apiResponse) -> { Space.refresh(apiResponse); return Space.getSpaceList("https://w3id.org/kpxl/gen/terms/" + type); },
            (space) -> {
                return new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space.getId()), space.getLabel());
            }
        ).addButton("new...", PublishPage.class, newLinkParams));
    }

}
