package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.page.QueryPage;

public class QueryList extends Panel {

	private static final long serialVersionUID = 1L;

	public QueryList(String id, List<GrlcQuery> queries) {
		super(id);
		init(queries);
	}

	public QueryList(String id, ApiResponse resp) {
		super(id);
		List<GrlcQuery> queries = new ArrayList<>();
		for (ApiResponseEntry e : resp.getData()) {
			try {
				queries.add(new GrlcQuery(e.get("np")));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		init(queries);
	}

	private void init(List<GrlcQuery> queryIds) {
		add(new DataView<GrlcQuery>("querylist", new ListDataProvider<GrlcQuery>(queryIds)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<GrlcQuery> item) {
				GrlcQuery q = item.getModelObject();
				PageParameters params = new PageParameters();
				params.add("id", q.getQueryId());
				BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("querylink", QueryPage.class, params);
				l.add(new Label("linktext", q.getLabel()));
				item.add(l);
			}

		});
	}

}
