package org.petapico.nanobench;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class UserListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public UserListPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		add(new DataView<User>("approved-users", new ListDataProvider<User>(User.getUsers(true))) {

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

		add(new DataView<User>("other-users", new ListDataProvider<User>(User.getUsers(false))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<User> item) {
				PageParameters params = new PageParameters();
				User u = item.getModelObject();
				params.add("id", u.getId());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", u.getDisplayName()));
				item.add(l);
				WebMarkupContainer actionLinks = new WebMarkupContainer("actions");
				item.add(actionLinks);
				if (ProfilePage.getUserIri() != null && !ProfilePage.getUserIri().equals(u.getId())) {
					actionLinks.add(new ExternalLink("approve-link", "./publish?" +
								"template=http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg" +
								"&param_nanopub=" + Utils.urlEncode(u.getIntropubIri().stringValue()))
							.add(new Label("approve-label", "approve...")));
				} else {
					actionLinks.add(new ExternalLink("approve-link", ".").add(new Label("approve-label", "")));  // Hide approve link
					actionLinks.setVisible(false);
				}
			}

		});

		add(new Link<String>("refresh") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				User.refreshUsers();
				throw new RestartResponseException(UserListPage.class);
			}

		});
	}

}
