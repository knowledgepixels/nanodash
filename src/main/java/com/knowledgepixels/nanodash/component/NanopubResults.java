package com.knowledgepixels.nanodash.component;

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.NanopubElement;

public class NanopubResults extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public static NanopubResults fromList(String id, List<NanopubElement> nanopubList) {
		NanopubResults r = new NanopubResults(id);
		r.add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject()).setMinimal());
			}

		});
		return r;
	}

	public static NanopubResults fromApiResponse(String id, List<ApiResponseEntry> apiResponse) {
		NanopubResults r = new NanopubResults(id);
		r.add(new DataView<ApiResponseEntry>("nanopubs", new ListDataProvider<ApiResponseEntry>(apiResponse)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				item.add(new NanopubItem("nanopub", NanopubElement.get(item.getModelObject().get("np"))).setMinimal());
			}

		});
		return r;
	}

	private NanopubResults(String id) {
		super(id);
	}

}
