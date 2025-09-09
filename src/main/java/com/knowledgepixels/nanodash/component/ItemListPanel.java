package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

public class ItemListPanel<T extends Serializable> extends Panel {

    public ItemListPanel(String id, String title, String description, List<T> items, ComponentProvider<T> compProvider) {
        super(id);
        setOutputMarkupId(true);
        add(new Label("title", title));
        add(new Label("description", description));
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

}
