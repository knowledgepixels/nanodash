package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.page.ProjectPage;

/**
 * A component that displays a list of projects.
 * Each project is represented as a link to its corresponding page.
 */
public class ProjectList {

    public static MarkupContainer getListContainer(String markupId) {
        final HashMap<String, String> noParams = new HashMap<>();
        final String queryName = "get-projects";
        ApiResponse qResponse = ApiCache.retrieveResponse(queryName, noParams);
        if (qResponse != null) {
            return createComponent(markupId, qResponse);
        } else {
            return new ApiResultComponent(markupId, queryName, noParams) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return createComponent(markupId, response);
                }
            };

        }
    }

    private static ItemListPanel<Project> createComponent(String id, ApiResponse resp) {
        Project.refresh(resp);
        return new ItemListPanel<Project>(id, "Projects", "Project pages are still experimental:", Project.getProjectList(), (p) -> {
                PageParameters params = new PageParameters();
                params.add("id", p.getId());
                return new BookmarkablePageLink<>("item", ProjectPage.class, params).setBody(Model.of(p.getLabel()));
            });
    }

}
