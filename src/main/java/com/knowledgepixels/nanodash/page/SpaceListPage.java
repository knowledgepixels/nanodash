package com.knowledgepixels.nanodash.page;

import java.util.Arrays;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

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

    private static final String[] journals = new String[]{"ios/ds", "pensoft/bdj", "pensoft/rio"};

    /**
     * Constructor for the SpaceListPage.
     *
     * @param parameters the page parameters
     */
    public SpaceListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "connectors"));

        addSpacePanel("Group");
        addSpacePanel("Project");
        addSpacePanel("Program");
        addSpacePanel("Initiative");
        addSpacePanel("Community");
        addSpacePanel("Event");

        add(new ItemListPanel<Project>(
                "legacy-projects",
                "Legacy Projects",
                new QueryRef("get-projects"),
                (apiResponse) -> { Project.refresh(apiResponse); return Project.getProjectList(); },
                (project) -> {
                    return new ItemListElement("item", ProjectPage.class, new PageParameters().add("id", project.getId()), project.getLabel());
                }
            ).setDescription("These legacy project pages will be migrated into the Spaces above:"));

        add(new ItemListPanel<String>(
                "journals",
                "Journals",
                Arrays.asList(journals),
                (journalId) -> {
                    String journalName = ConnectorConfig.get(journalId).getJournalName();
                    return new ItemListElement("item", GenOverviewPage.class, new PageParameters().add("journal", journalId), journalName);
                }
            ));
    }

    private void addSpacePanel(String type) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");
        add(new ItemListPanel<Space>(
            typePl.toLowerCase(),
            typePl,
            new QueryRef("get-spaces"),
            (apiResponse) -> { Space.refresh(apiResponse); return Space.getSpaceList("https://w3id.org/kpxl/gen/terms/" + type); },
            (space) -> {
                return new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space.getId()), space.getLabel());
            }
        ).addButton("new...",
                PublishPage.class,
                new PageParameters()
                    .add("template", "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ")
                    .add("param_type", "https://w3id.org/kpxl/gen/terms/" + type)
                    .add("template-version", "latest")
                    .add("postpub-redirect-url", MOUNT_PATH)
            )
        );
    }

}
