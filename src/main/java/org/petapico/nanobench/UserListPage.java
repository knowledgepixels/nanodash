package org.petapico.nanobench;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
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
import org.eclipse.rdf4j.model.IRI;


public class UserListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public UserListPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		add(new DataView<IRI>("approved-users", new ListDataProvider<IRI>(User.getUsers(true))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				params.add("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", User.getDisplayName(item.getModelObject())));
				item.add(l);
			}

		});

		add(new DataView<IRI>("other-users", new ListDataProvider<IRI>(User.getUsers(false))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				IRI iri = item.getModelObject();
				params.add("id", iri);
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", User.getDisplayName(iri)));
				item.add(l);
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
		add(new ExternalLink("approve", "./publish?template=http://purl.org/np/RA6TVVSnZChEwyxjvFDNAujk1i8sSPnQx60ZQjldtiDkw", "approve somebody else"));
	}

}
