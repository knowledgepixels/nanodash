package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.ProjectPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

/**
 * A component that displays a list of projects.
 * Each project is represented as a link to its corresponding page.
 */
public class ProjectList extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for ProjectList.
     *
     * @param id   the Wicket component ID
     * @param resp the ApiResponse containing project data
     */
    public ProjectList(String id, ApiResponse resp) {
        super(id);
        add(new DataView<ApiResponseEntry>("projectlist", new ListDataProvider<ApiResponseEntry>(resp.getData())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                ApiResponseEntry e = item.getModelObject();
                PageParameters params = new PageParameters();
                params.add("id", e.get("project"));
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("projectlink", ProjectPage.class, params);
                l.add(new Label("linktext", e.get("label")));
                item.add(l);
            }

        });
    }

}
