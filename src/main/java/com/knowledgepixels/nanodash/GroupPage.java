package com.knowledgepixels.nanodash;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

public class GroupPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/group";

	public GroupPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		final String groupId = parameters.get("id").toString();
		final Group group = Group.get(groupId);

		add(new Label("groupname", group.getName()));

		add(new DataView<IRI>("members", new ListDataProvider<IRI>(group.getMembers())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				params.add("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("memberlink", UserPage.class, params);
				l.add(new Label("linktext", User.getDisplayName(item.getModelObject())));
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
				Group.refreshGroups();
				throw new RestartResponseException(GroupPage.class);
			}

		});
	}

}