package com.knowledgepixels.nanodash.component;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

public class InstanceResults extends Panel {
	
	private static final long serialVersionUID = 1L;

	public static InstanceResults fromApiResponse(String id, ApiResponse apiResponse) {
		List<ApiResponseEntry> list = apiResponse.getData();
		InstanceResults r = new InstanceResults(id);
		r.add(new DataView<ApiResponseEntry>("instances", new ListDataProvider<ApiResponseEntry>(list)) {	

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				item.add(new NanodashLink("instance-link", item.getModelObject().get("instance")));
				item.add(new NanodashLink("nanopub-link", item.getModelObject().get("np")));
			}

		});
		return r;
	}

	private InstanceResults(String id) {
		super(id);
	}

}
