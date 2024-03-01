package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
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

public class UserListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/userlist";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public UserListPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this));

		final List<Group> groupList = new ArrayList<Group>(Group.getGroups());
		add(new DataView<Group>("groups", new ListDataProvider<Group>(groupList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Group> item) {
				Group g = item.getModelObject();
				PageParameters params = new PageParameters();
				params.add("id", g.getIri());
				BookmarkablePageLink<GroupPage> l = new BookmarkablePageLink<GroupPage>("grouplink", GroupPage.class, params);
				l.add(new Label("linktext", g.getName()));
				item.add(l);
			}

		});

		final List<IRI> userList = User.getUsers(true);
		add(new Label("usercount", userList.size()));

		add(new DataView<IRI>("approved-users", new ListDataProvider<IRI>(userList)) {

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
		add(new ExternalLink("approve", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RA6TVVSnZChEwyxjvFDNAujk1i8sSPnQx60ZQjldtiDkw&template-version=latest", "approve somebody else"));
		add(new ExternalLink("newgroup", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RAJz6w5cvlsFGkCDtWOUXt2VwEQ3tVGtPdy3atPj_DUhk&template-version=latest", "new group"));
	}

}
