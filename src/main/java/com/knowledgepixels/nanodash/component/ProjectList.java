package com.knowledgepixels.nanodash.component;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Project;
import com.knowledgepixels.nanodash.page.ProjectPage;

/**
 * A component that displays a list of projects.
 * Each project is represented as a link to its corresponding page.
 */
public class ProjectList extends Panel {

    private static final long serialVersionUID = 1L;

    public static MarkupContainer getListContainer(String markupId) {
        final HashMap<String, String> noParams = new HashMap<>();
        final String queryName = "get-projects";
        ApiResponse qResponse = ApiCache.retrieveResponse(queryName, noParams);
        if (qResponse != null) {
            return new ProjectList(markupId, qResponse);
        } else {
            return new ApiResultComponent(markupId, queryName, noParams) {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ProjectList(markupId, response);
                }
            };

        }
    }

    /**
     * Constructor for ProjectList.
     *
     * @param id   the Wicket component ID
     * @param resp the ApiResponse containing project data
     */
    public ProjectList(String id, ApiResponse resp) {
        super(id);
        setOutputMarkupId(true);

        Project.refresh(resp);
        DataView<Project> dataView = new DataView<Project>("projectlist", new ListDataProvider<Project>(Project.getProjectList())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<Project> item) {
                Project p = item.getModelObject();
                PageParameters params = new PageParameters();
                params.add("id", p.getId());
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("projectlink", ProjectPage.class, params);
                l.add(new Label("linktext", p.getLabel()));
                item.add(l);
            }

        };
        dataView.setItemsPerPage(10);
        dataView.setOutputMarkupId(true);
        add(dataView);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);
        add(navigation);
    }

}
