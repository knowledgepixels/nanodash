package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.FilteredListDataProvider;
import com.knowledgepixels.nanodash.GrlcQuery;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A component that displays a list of queries.
 * Each query is linked to its corresponding QueryPage.
 */
public class QueryList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryList.class);

    private Model<String> filterModel = Model.of("");
    private FilteredListDataProvider<GrlcQuery> filteredDataProvider;
    private DataView<GrlcQuery> dataView;

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

        add(new Label("title", "Other Queries"));

        filteredDataProvider = new FilteredListDataProvider<>(queries, GrlcQuery::getLabel);
        dataView = new DataView<>("querylist", filteredDataProvider) {

            @Override
            protected void populateItem(Item<GrlcQuery> item) {
                item.add(new QueryItem("queryitem", item.getModelObject(), null, true));
            }

        };
        dataView.setItemsPerPage(10);
        dataView.setOutputMarkupId(true);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                filteredDataProvider.setFilterText(filterModel.getObject());
                WebMarkupContainer nav = (WebMarkupContainer) get("navigation");
                nav.setVisible(dataView.getPageCount() > 1);
                target.add(QueryList.this);
            }
        });
        add(filterField);
        add(new WebMarkupContainer("buttons"));
        add(dataView);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);
        add(navigation);
    }

}
