package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
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
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.MethodResultComponent;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TemplateResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.UserList;
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

        project = Project.get(parameters.get("id").toString());
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
        add(new DataView<Pair<String, List<Template>>>("template-lists", new ListDataProvider<Pair<String, List<Template>>>(templateLists)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<Pair<String, List<Template>>> item) {
                item.add(new Label("label", item.getModelObject().getLeft()));
                item.add(TemplateResults.fromList("templates", item.getModelObject().getRight(), params));
            }

        });
        add(TemplateResults.fromList("templates", templates, params));

        if (project.isDataInitialized()) {
            add(new UserList("owners", project.getOwners(), true));
        } else {
            add(new MethodResultComponent<Project,List<IRI>>("owners", project, Project::isDataInitialized, Project::getOwners) {
                @Override
                public Component getResultComponent(String markupId, List<IRI> result) {
                    return new UserList(markupId, result, true);
                }
            });
        }

        if (project.isDataInitialized()) {
            add(new UserList("members", project.getMembers(), true));
        } else {
            add(new MethodResultComponent<Project,List<IRI>>("members", project, Project::isDataInitialized, Project::getMembers) {
                @Override
                public Component getResultComponent(String markupId, List<IRI> result) {
                    return new UserList(markupId, result, true);
                }
            });
        }

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
