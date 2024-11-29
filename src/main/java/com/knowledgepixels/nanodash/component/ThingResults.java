package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.page.ExplorePage;

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
				String thingId = item.getModelObject().get(thingField);
				String thingLabel = item.getModelObject().get(thingField + "Label");
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
