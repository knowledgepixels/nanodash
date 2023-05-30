package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class GroupListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/grouplist";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GroupListPage(final PageParameters parameters) {
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

		add(new Link<String>("refresh") {

			private static final long serialVersionUID = 1L;

			@Override
			public MarkupContainer setDefaultModel(IModel<?> arg0) {
				return null;
			}

			@Override
			public void onClick() {
				Group.refreshGroups();
				throw new RestartResponseException(GroupListPage.class);
			}

		});
	}

}
