package com.knowledgepixels.nanodash.component;

import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.UserPage;

public class UserList extends Panel {

	private static final long serialVersionUID = 1L;

	public UserList(String id, List<IRI> users, Map<IRI,String> notes) {
		super(id);

		add(new DataView<IRI>("userlist", new ListDataProvider<IRI>(users)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				IRI userIri = item.getModelObject();
				PageParameters params = new PageParameters();
				params.add("id", userIri);
				BookmarkablePageLink<Void> l = new BookmarkablePageLink<Void>("userlink", UserPage.class, params);
				if (!User.isUser(userIri)) {
					l = new BookmarkablePageLink<Void>("userlink", ExplorePage.class, params);
				}
				l.add(new Label("linktext", User.getShortDisplayName(userIri)));
				item.add(l);
				if (notes != null && notes.containsKey(userIri)) {
					item.add(new Label("notes", notes.get(userIri)));
				} else {
					item.add(new Label("notes"));
				}
			}

		});
	}

	public UserList(String id, List<IRI> users) {
		this(id, users, null);
	}

}
