package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.ItemListPanel.ComponentProvider;
import com.knowledgepixels.nanodash.FilteredListDataProvider;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

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
        this(markupId, items, compProvider, null);
    }

    /**
     * Creates an ItemList panel with optional filter.
     *
     * @param markupId        the markup ID for the panel
     * @param items           the list of items to display
     * @param compProvider    a provider that generates a component for each item
     * @param filterTextGetter function to get searchable text from each item, or null for no filter (must be serializable for Wicket page serialization)
     */
    public ItemList(String markupId, List<T> items, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter) {
        this(markupId, items, compProvider, filterTextGetter, null);
    }

    /**
     * Creates an ItemList panel with filter in external panel (filter field is in parent; filterModel is shared).
     */
    public ItemList(String markupId, List<T> items, ComponentProvider<T> compProvider, FilteredListDataProvider.SerializableFunction<T, String> filterTextGetter, IModel<String> filterModel) {
        super(markupId);
        setOutputMarkupId(true);

        final DataView<T> dataView;
        if (filterTextGetter != null) {
            IModel<String> effectiveFilterModel = filterModel != null ? filterModel : Model.of("");
            FilteredListDataProvider<T> filteredDataProvider = filterModel != null
                    ? new FilteredListDataProvider<>(items, filterTextGetter, filterModel)
                    : new FilteredListDataProvider<>(items, filterTextGetter);
            dataView = new DataView<T>("itemlist", filteredDataProvider) {
                @Override
                protected void populateItem(Item<T> item) {
                    item.add(compProvider.apply(item.getModelObject()));
                }
            };
            if (filterModel == null) {
                TextField<String> filterField = new TextField<>("filter", effectiveFilterModel);
                filterField.setOutputMarkupId(true);
                filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        filteredDataProvider.setFilterText(effectiveFilterModel.getObject());
                        WebMarkupContainer nav = (WebMarkupContainer) ItemList.this.get("navigation");
                        nav.setVisible(dataView.getPageCount() > 1);
                        target.add(ItemList.this);
                    }
                });
                add(filterField);
            } else {
                add(new TextField<>("filter", Model.of("")).setVisible(false));
            }
        } else {
            dataView = new DataView<T>("itemlist", new ListDataProvider<T>(items)) {
                @Override
                protected void populateItem(Item<T> item) {
                    item.add(compProvider.apply(item.getModelObject()));
                }
            };
            add(new TextField<>("filter", Model.of("")).setVisible(false));
        }

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
