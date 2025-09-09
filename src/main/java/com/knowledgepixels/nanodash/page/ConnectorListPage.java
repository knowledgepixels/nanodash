package com.knowledgepixels.nanodash.page;

import java.util.Arrays;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

/**
 * A page that lists all available connectors.
 */
public class ConnectorListPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/connectorlist";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private static final String[] journals = new String[]{"ios/ds", "pensoft/bdj", "pensoft/rio"};

    /**
     * Constructor for the ConnectorListPage.
     *
     * @param parameters the page parameters
     */
    public ConnectorListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "connectors"));

        add(new ItemListPanel<String>(
                "journals",
                "Journals",
                Arrays.asList(journals),
                (journalId) -> {
                    String journalName = ConnectorConfig.get(journalId).getJournalName();
                    return new ItemListElement("item", GenOverviewPage.class, new PageParameters().add("journal", journalId), journalName);
                }
            ));

        add(new ItemListPanel<Project>(
                "projects",
                "Projects  Project pages are still experimental:",
                new QueryRef("get-projects"),
                (apiResponse) -> { Project.refresh(apiResponse); return Project.getProjectList(); },
                (project) -> {
                    return new ItemListElement("item", ProjectPage.class, new PageParameters().add("id", project.getId()), project.getLabel());
                }
            ));
    }

}
