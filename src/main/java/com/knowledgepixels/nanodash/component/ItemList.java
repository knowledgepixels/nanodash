package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.ItemListPanel.ComponentProvider;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import java.io.Serializable;
import java.util.List;

/**
 * A reusable Wicket panel that displays a paginated list of items.
 *
 * @param <T> the type of items in the list, must be Serializable
 */
public class ItemList<T extends Serializable> extends Panel {

    /**
     * Creates an ItemList panel.
     *
     * @param markupId     the markup ID for the panel
     * @param items        the list of items to display
     * @param compProvider a provider that generates a component for each item
     */
    public ItemList(String markupId, List<T> items, ComponentProvider<T> compProvider) {
        super(markupId);
        setOutputMarkupId(true);

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

}
