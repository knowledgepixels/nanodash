package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.component.menu.EntryActionMenu;

/**
 * A panel that displays a list of nanopubs.
 */
public class NanopubResults extends Panel {

    /**
     * Provider of per-card entry-action links (a view's {@code gen:ViewEntryAction}s
     * applied to the card's result row); each link must use the markup id {@code "link"}.
     */
    public interface EntryActions extends Serializable {

        /**
         * Builds the entry-action links for one result row.
         *
         * @param row the result row backing the card
         * @return the links (never null; empty for no menu)
         */
        List<AbstractLink> build(ApiResponseEntry row);

    }

    /**
     * Creates a NanopubResults panel from a list of NanopubElements.
     *
     * @param id          the component id
     * @param nanopubList the list of NanopubElements to display
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromList(String id, List<NanopubElement> nanopubList, long itemPerPage) {
        NanopubResults r = new NanopubResults(id);
        DataView<NanopubElement> dataView = new DataView<>("nanopubs", new ListDataProvider<NanopubElement>(nanopubList)) {

            @Override
            protected void populateItem(Item<NanopubElement> item) {
                item.add(new NanopubItem("nanopub", item.getModelObject()).setMinimal());
                item.add(new Label("entry-actions").setVisible(false));
            }

        };
        dataView.setItemsPerPage(itemPerPage);
        dataView.setOutputMarkupId(true);
        r.add(dataView);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);
        r.add(navigation);

        return r;
    }

    /**
     * Creates a NanopubResults panel from an ApiResponse with a limit on the number of nanopubs.
     *
     * @param id          the component id
     * @param apiResponse the ApiResponse containing nanopub data
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, ApiResponse apiResponse, long itemPerPage) {
        return fromApiResponse(id, apiResponse.getData(), itemPerPage);
    }

    public static NanopubResults fromApiResponse(String id, List<ApiResponseEntry> data, long itemPerPage) {
        return fromApiResponse(id, data, itemPerPage, null);
    }

    /**
     * As {@link #fromApiResponse(String, List, long)}, with a per-card dropdown of
     * entry-action links built from each card's result row.
     *
     * @param id           the component id
     * @param data         the result rows (one card per row, from its "np" column)
     * @param itemPerPage  the number of cards per page
     * @param entryActions the per-card entry-action provider, or null for none
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, List<ApiResponseEntry> data, long itemPerPage, EntryActions entryActions) {
        NanopubResults r = new NanopubResults(id);
        DataView<ApiResponseEntry> dataView = new DataView<ApiResponseEntry>("nanopubs", new ListDataProvider<ApiResponseEntry>(data)) {

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                item.add(new NanopubItem("nanopub", NanopubElement.get(item.getModelObject().get("np"))).setMinimal());
                List<AbstractLink> links = (entryActions == null) ? List.of() : entryActions.build(item.getModelObject());
                Component actionsComponent = links.isEmpty()
                        ? new Label("entry-actions").setVisible(false)
                        : new EntryActionMenu("entry-actions", links);
                item.add(actionsComponent);
            }

        };
        dataView.setItemsPerPage(itemPerPage);
        dataView.setOutputMarkupId(true);
        r.add(dataView);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);
        r.add(navigation);

        return r;
    }

    private NanopubResults(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    /**
     * Enumeration that defines the possible view modes of the nanopub results.
     */
    public enum ViewMode {
        /**
         * List view mode, displaying nanopubs in a vertical list.
         */
        LIST("list-view"),
        /**
         * Grid view mode, displaying nanopubs in a grid layout.
         */
        GRID("grid-view");

        private final String value;

        ViewMode(String value) {
            this.value = value;
        }

        /**
         * Retrieves the string value associated with the view mode.
         *
         * @return the string value of the view mode
         */
        public String getValue() {
            return value;
        }
    }

}
