package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.GrlcQuery;

/**
 * A component that displays a list of queries.
 * Each query is linked to its corresponding QueryPage.
 */
public class QueryList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryList.class);

    /**
     * Constructor for QueryList.
     *
     * @param id   the component ID
     * @param resp the API response containing query data
     */
    public QueryList(String id, ApiResponse resp) {
        super(id);
        setOutputMarkupId(true);

        List<GrlcQuery> queries = new ArrayList<>();
        for (ApiResponseEntry e : resp.getData()) {
            try {
                queries.add(GrlcQuery.get(e.get("np")));
            } catch (Exception ex) {
                logger.error("Error processing query nanopub: {}", ex.getMessage());
            }
        }

        DataView<GrlcQuery> dataView = new DataView<>("querylist", new ListDataProvider<GrlcQuery>(queries)) {

            @Override
            protected void populateItem(Item<GrlcQuery> item) {
                item.add(new QueryItem("queryitem", item.getModelObject(), true));
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
