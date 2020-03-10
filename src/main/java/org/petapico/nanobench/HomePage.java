package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class HomePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		add(new DataView<User>("users", new ListDataProvider<User>(User.getUsers(true))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<User> item) {
				PageParameters params = new PageParameters();
				params.add("id", item.getModelObject().getId());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", item.getModelObject().getDisplayName()));
				item.add(l);
			}

		});
	}

}
