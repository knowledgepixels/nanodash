package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.ExplorePage;

public class ReactionList extends Panel {

	private static final long serialVersionUID = 1L;

	public ReactionList(String id, ApiResponse resp, Nanopub np) {
		super(id);

		add(new DataView<ApiResponseEntry>("reactions", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ApiResponseEntry> item) {
				ApiResponseEntry e = item.getModelObject();
				PageParameters params = new PageParameters();
				if (e.get("pub").equals(np.getUri().stringValue())) {
					item.add(new Label("reactionnote"));
				} else {
					item.add(new Label("reactionnote", "On earlier version:"));
				}
				item.add(new Label("reactiontext", "\"" + e.get("text") + "\" (" + e.get("reltext") + " the nanopublication above)"));
				params.add("id", e.get("np"));
				BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("reactionlink", ExplorePage.class, params);
				String username = User.getShortDisplayName(null, e.get("pubkey"));
				l.add(new Label("reactionlinktext", "by " + username + " on " + e.get("date").substring(0, 10)));
				item.add(l);
			}

		});
	}

}
