package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;

public class ItemListPanel<T extends Serializable> extends Panel {

    public ItemListPanel(String id, String title, List<T> items, ComponentProvider<T> compProvider) {
        super(id);
        setOutputMarkupId(true);
        if (title.contains("  ")) {
            add(new Label("description", title.replaceFirst("^.*  ", "")));
            title = title.replaceFirst("  .*$", "");
        } else {
            add(new Label("description").setVisible(false));
        }
        add(new Label("title", title));
        add(new Label("button").setVisible(false));

        DataView<T> dataView = new DataView<T>("itemlist", new ListDataProvider<T>(items)) {

            @Override
            protected void populateItem(Item<T> item) {
                item.add(compProvider.apply(item.getModelObject()));
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

    public static interface ComponentProvider<T> extends Function<T, Component>, Serializable {
    }

    public static interface ApiResultListProvider<T> extends Function<ApiResponse, List<T>>, Serializable {
    }

    public static class LazyLoad<T extends Serializable> implements Serializable {

        private String markupId;
        private String queryName;
        private HashMap<String, String> params;
        private ApiResultListProvider<T> resultListProvider;
        private String title;
        private ComponentProvider<T> compProvider;

        public LazyLoad(String markupId, String title, String queryName, HashMap<String, String> params, ApiResultListProvider<T> resultListProvider, ComponentProvider<T> compProvider) {
            this.markupId = markupId;
            this.queryName = queryName;
            this.params = params;
            this.resultListProvider = resultListProvider;
            this.title = title;
            this.compProvider = compProvider;
        }

        public MarkupContainer getContainer() {
            ApiResponse qResponse = ApiCache.retrieveResponse(queryName, params);
            if (qResponse != null) {
                return new ItemListPanel<T>(markupId, title, resultListProvider.apply(qResponse), compProvider);
            } else {
                return new ApiResultComponent(markupId, queryName, params) {

                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        return new ItemListPanel<T>(markupId, title, resultListProvider.apply(response), compProvider);
                    }
                };

            }
        }
        
    }

}
