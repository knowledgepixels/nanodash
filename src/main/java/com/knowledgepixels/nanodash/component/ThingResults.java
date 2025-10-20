package com.knowledgepixels.nanodash.component;

import java.util.List;

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ExplorePage;

/**
 * A panel that displays a list of things (e.g., nanopublications, entities) based on an API response.
 */
public class ThingResults extends Panel {

    /**
     * Creates a new ThingResults panel.
     *
     * @param id          the Wicket component ID
     * @param thingField  the field in the API response that contains the thing ID
     * @param apiResponse the API response containing the data
     * @return a ThingResults instance populated with the data from the API response
     */
    public static ThingResults fromApiResponse(String id, String thingField, ApiResponse apiResponse) {
        List<ApiResponseEntry> list = apiResponse.getData();
        ThingResults r = new ThingResults(id);
        DataView<ApiResponseEntry> dataView = new DataView<>("things", new ListDataProvider<ApiResponseEntry>(list)) {

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                // TODO Improve label determination and move this code to a more general place?
                String thingId = item.getModelObject().get(thingField);
                String thingLabel = item.getModelObject().get(thingField + "Label");
                String npLabel = item.getModelObject().get("npLabel");
                if (thingId.matches(".*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}[^A-Za-z0-9-_].*") && (thingLabel == null || thingLabel.isBlank()) && npLabel != null && !npLabel.isBlank()) {
                    thingLabel = Utils.getShortNameFromURI(thingId) + " in '" + npLabel.replaceFirst(" - [\\s\\S]*$", "") + "'";
                }
                item.add(new NanodashLink("thing-link", thingId, null, null, thingLabel));
                String npId = item.getModelObject().get("np");
                item.add(new BookmarkablePageLink<Void>("nanopub-link", ExplorePage.class, new PageParameters().add("id", npId)));
            }

        };
        dataView.setItemsPerPage(10);
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

    private ThingResults(String id) {
        super(id);
        setOutputMarkupId(true);
    }

}
