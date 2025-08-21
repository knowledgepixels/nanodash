package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.util.ArrayList;
import java.util.List;

public class ThingResults extends Panel {

    private static final long serialVersionUID = 1L;

    public static ThingResults fromApiResponse(String id, String thingField, ApiResponse apiResponse, int limit) {
        List<ApiResponseEntry> list = apiResponse.getData();
        if (limit > 0 && list.size() > limit) {
            List<ApiResponseEntry> shortList = new ArrayList<>();
            for (ApiResponseEntry e : list) {
                shortList.add(e);
                if (shortList.size() == limit) break;
            }
            list = shortList;
        }
        ThingResults r = new ThingResults(id);
        r.add(new DataView<ApiResponseEntry>("things", new ListDataProvider<ApiResponseEntry>(list)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                // TODO Improve label determination and move this code to a more general place?
                String thingId = item.getModelObject().get(thingField);
                String thingLabel = item.getModelObject().get(thingField + "Label");
                String npLabel = item.getModelObject().get("npLabel");
                if (thingId.matches(".*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}[^A-Za-z0-9-_].*") && (thingLabel == null || thingLabel.isBlank()) && npLabel != null && !npLabel.isBlank()) {
                    thingLabel = Utils.getShortNameFromURI(thingId) + " in '" + npLabel.replaceFirst(" - [\\s\\S]*$", "") + "'";
                }
                item.add(new NanodashLink("thing-link", thingId, null, null, false, thingLabel));
                String npId = item.getModelObject().get("np");
                item.add(new BookmarkablePageLink<Void>("nanopub-link", ExplorePage.class, new PageParameters().add("id", npId)));
            }

        });
        return r;
    }

    private ThingResults(String id) {
        super(id);
    }

}
