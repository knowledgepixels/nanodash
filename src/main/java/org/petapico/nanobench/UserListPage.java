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
import org.eclipse.rdf4j.model.IRI;


public class UserListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public UserListPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		add(new DataView<IRI>("approved-users", new ListDataProvider<IRI>(UserNew.getUsers(true))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				params.add("id", item);
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", UserNew.getDisplayName(item.getModelObject())));
				item.add(l);
			}

		});

		add(new DataView<IRI>("other-users", new ListDataProvider<IRI>(UserNew.getUsers(false))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				IRI iri = item.getModelObject();
				params.add("id", iri);
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", UserNew.getDisplayName(iri)));
				item.add(l);
				WebMarkupContainer actionLinks = new WebMarkupContainer("actions");
				item.add(actionLinks);
				// TODO Reactivate Approve link
//				IRI userIri = NanobenchSession.get().getUserIri();
//				if (userIri != null && !userIri.equals(iri)) {
//					actionLinks.add(new ExternalLink("approve-link", "./publish?" +
//								"template=http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg" +
//								"&param_nanopub=" + Utils.urlEncode(u.getIntropubIri().stringValue()))
//							.add(new Label("approve-label", "approve...")));
//				} else {
					actionLinks.add(new ExternalLink("approve-link", ".").add(new Label("approve-label", "")));  // Hide approve link
					actionLinks.setVisible(false);
//				}
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
