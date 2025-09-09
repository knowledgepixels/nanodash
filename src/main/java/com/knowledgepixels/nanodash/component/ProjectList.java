package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.page.ProjectPage;

/**
 * A component that displays a list of projects.
 * Each project is represented as a link to its corresponding page.
 */
public class ProjectList {

    public static MarkupContainer getListContainer(String markupId) {
        return new ItemListPanel.LazyLoad<Project>(
                markupId,
                "Projects  Project pages are still experimental:",
                "get-projects",
                new HashMap<>(),
                (resp) -> { Project.refresh(resp); return Project.getProjectList(); },
                (p) -> {
                    PageParameters params = new PageParameters();
                    params.add("id", p.getId());
                    return new BookmarkablePageLink<>("item", ProjectPage.class, params).setBody(Model.of(p.getLabel()));
                }
            ).getContainer();
    }

}
