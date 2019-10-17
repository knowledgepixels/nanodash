package org.petapico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class FreeTextSearchPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public FreeTextSearchPage(final PageParameters parameters) {
		String searchText = parameters.get("query").toString();
		add(new Label("searchtext", searchText));

		Map<String,String> nanopubParams = new HashMap<>();
		nanopubParams.put("text", searchText);
		List<String> nanopubUris = ApiAccess.getAll("find_nanopubs_with_text", nanopubParams, 0);

		List<NanopubElement> nanopubs = new ArrayList<>();
		for (int i = 0 ; i < 10 && i < nanopubUris.size() ; i++) {
			nanopubs.add(new NanopubElement(nanopubUris.get(i)));
		}

		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject(), false));
			}

		});
	}

}
