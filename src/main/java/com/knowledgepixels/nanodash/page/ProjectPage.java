package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TemplateItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;

/**
 * The ProjectPage class represents a project page in the Nanodash application.
 */
public class ProjectPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/project";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Project object with the data shown on this page.
     */
    private Project project;

    /**
     * Constructor for the ProjectPage.
     *
     * @param parameters the page parameters
     * @throws org.nanopub.extra.services.FailedApiCallException if the API call fails
     */
    public ProjectPage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        String projectId = parameters.get("id").toString();
        if (Space.get(projectId) != null) {
            throw new RestartResponseException(SpacePage.class, parameters);
        }
        project = Project.get(projectId);
        Nanopub np = project.getRootNanopub();

        add(new TitleBar("titlebar", this, null));

        add(new Label("pagetitle", project.getLabel() + " (project) | nanodash"));
        add(new Label("projectname", project.getLabel()));
        add(new ExternalLink("id", project.getId(), project.getId()));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", np.getUri())));
        add(new Label("description", "<span class=\"internal\">" + Utils.sanitizeHtml(project.getDescription()) + "</span>").setEscapeModelStrings(false));

        final PageParameters params = new PageParameters();
        if (project.getDefaultProvenance() != null) {
            params.add("prtemplate", project.getDefaultProvenance().stringValue());
        }
        List<Pair<String, List<Template>>> templateLists = new ArrayList<>();
        List<String> templateTagList = new ArrayList<>(project.getTemplateTags());
        Collections.sort(templateTagList);
        List<Template> templates = new ArrayList<>(project.getTemplates());
        for (String tag : templateTagList) {
            for (Template t : project.getTemplatesPerTag().get(tag)) {
                if (templates.contains(t)) templates.remove(t);
            }
            templateLists.add(Pair.of(tag, project.getTemplatesPerTag().get(tag)));
        }
        if (!templates.isEmpty()) {
            String l = templateLists.isEmpty() ? "Templates" : "Other Templates";
            templateLists.add(Pair.of(l, templates));
        }
        add(new DataView<Pair<String, List<Template>>>("template-lists", new ListDataProvider<>(templateLists)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<Pair<String, List<Template>>> item) {
                item.add(new ItemListPanel<Template>(
                        "templates",
                        item.getModelObject().getLeft(),
                        item.getModelObject().getRight(),
                        (template) -> new TemplateItem("item", template, params)
                    ));
            }

        });
        add(new ItemListPanel<Template>(
                "templates",
                "Templates",
                templates,
                (template) -> new TemplateItem("item", template, params)
            ));

        add(new ItemListPanel<IRI>(
                "owners",
                "Owners",
                () -> project.isDataInitialized(),
                () -> project.getOwners(),
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new ItemListPanel<IRI>(
                "members",
                "Members",
                () -> project.isDataInitialized(),
                () -> project.getMembers(),
                (userIri) -> {
                    return new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri));
                }
            ));

        add(new DataView<IRI>("queries", new ListDataProvider<IRI>(project.getQueryIds())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<IRI> item) {
                String queryId = QueryApiAccess.getQueryId(item.getModelObject());
                item.add(QueryResultTable.createComponent("query", queryId, false));
            }

        });
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
