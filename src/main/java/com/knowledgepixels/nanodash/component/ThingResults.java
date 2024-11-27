package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

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
				item.add(new NanodashLink("thing-link", item.getModelObject().get(thingField)));
				item.add(new NanodashLink("nanopub-link", item.getModelObject().get("np")));
			}

		});
		return r;
	}

	private ThingResults(String id) {
		super(id);
	}

}
