package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

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
                new QueryRef(QueryApiAccess.GET_PROJECTS),
                (apiResponse) -> {
                    Project.refresh(apiResponse);
                    return Project.getProjectList();
                },
                (project) -> {
                    return new ItemListElement("item", ProjectPage.class, new PageParameters().set("id", project.getId()), project.getLabel());
                }
        ).setDescription("These legacy project pages will be migrated into the Spaces above:"));
    }

    private void addSpacePanel(String type, boolean openEnded) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");

        PageParameters newLinkParams = new PageParameters()
                .set("param_type", KPXL_TERMS.NAMESPACE + type)
                .set("template-version", "latest")
                .set("refresh-upon-publish", "spaces")
                .set("postpub-redirect-url", MOUNT_PATH);
        if (openEnded) {
            newLinkParams.set("template", "https://w3id.org/np/RA7dQfmndqKmooQ4PlHyQsAql9i2tg_8GLHf_dqtxsGEQ");
        } else {
            newLinkParams.set("template", "https://w3id.org/np/RAaE7NP9RNIx03AHZxanFMdtUuaTfe50ns5tHhpEVloQ4");
        }

        add(new ItemListPanel<Space>(
                typePl.toLowerCase(),
                typePl,
                new QueryRef(QueryApiAccess.GET_SPACES),
                (apiResponse) -> {
                    SpaceRepository.get().refresh(apiResponse);
                    return SpaceRepository.get().findByType(KPXL_TERMS.NAMESPACE + type);
                },
                (space) -> {
                    return new ItemListElement("item", SpacePage.class, new PageParameters().set("id", space.getId()), space.getLabel());
                }
        ).addButton("+", PublishPage.class, newLinkParams));
    }

}
