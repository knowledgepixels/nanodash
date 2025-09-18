package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.List;

/**
 * A panel that displays a list of nanopubs.
 */
public class NanopubResults extends Panel {

    /**
     * Creates a NanopubResults panel from a list of NanopubElements.
     *
     * @param id          the component id
     * @param nanopubList the list of NanopubElements to display
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromList(String id, List<NanopubElement> nanopubList) {
        NanopubResults r = new NanopubResults(id);
        DataView<NanopubElement> dataView = new DataView<>("nanopubs", new ListDataProvider<NanopubElement>(nanopubList)) {

            @Override
            protected void populateItem(Item<NanopubElement> item) {
                item.add(new NanopubItem("nanopub", item.getModelObject()).setMinimal());
            }

        };
        dataView.setItemsPerPage(12);
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
     * Creates a NanopubResults panel from an ApiResponse.
     *
     * @param id          the component id
     * @param apiResponse the ApiResponse containing nanopub data
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, ApiResponse apiResponse) {
        return fromApiResponse(id, apiResponse, -1);
    }

    /**
     * Creates a NanopubResults panel from an ApiResponse with a limit on the number of nanopubs.
     *
     * @param id          the component id
     * @param apiResponse the ApiResponse containing nanopub data
     * @param limit       the maximum number of nanopubs to display, or -1 for no limit
     * @return a new NanopubResults panel
     */
    public static NanopubResults fromApiResponse(String id, ApiResponse apiResponse, int limit) {
        List<ApiResponseEntry> list = apiResponse.getData();
        if (limit >= 0 && list.size() > limit) {
            list = Utils.subList(list, 0, limit);
        }
        NanopubResults r = new NanopubResults(id);
        DataView<ApiResponseEntry> dataView = new DataView<ApiResponseEntry>("nanopubs", new ListDataProvider<ApiResponseEntry>(list)) {

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                item.add(new NanopubItem("nanopub", NanopubElement.get(item.getModelObject().get("np"))).setMinimal());
            }

        };
        dataView.setItemsPerPage(12);
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
        LIST("list-view"),
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
