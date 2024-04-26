package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

public class GroupPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/group";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GroupPage(final PageParameters parameters) {
		super(parameters);

		User.ensureLoaded();
		add(new TitleBar("titlebar", this, "users"));
		final String groupId = parameters.get("id").toString();
		final Group group = Group.get(groupId);

		add(new Label("pagetitle", group.getName() + " (group) | nanodash"));
		add(new Label("groupname", group.getName()));
		add(new ExternalLink("groupid", groupId, groupId));


		PageParameters params = new PageParameters().add("id", group.getOwner());
		BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("ownerlink", UserPage.class, params);
		l.add(new Label("linktext", User.getDisplayName(group.getOwner())));
		add(l);

		add(new DataView<IRI>("members", new ListDataProvider<IRI>(group.getMembers())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters().add("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("memberlink", UserPage.class, params);
				l.add(new Label("linktext", User.getDisplayName(item.getModelObject())));
				item.add(l);
			}

		});

		add(new ExternalLink("update", PublishPage.MOUNT_PATH + "?template=http://purl.org/np/RAJz6w5cvlsFGkCDtWOUXt2VwEQ3tVGtPdy3atPj_DUhk" +
				"&supersede=" + group.getNanopub().getUri().stringValue() +
				"&template-version=latest", "update").setVisible(group.getOwnerPubkey().equals(NanodashSession.get().getPubkeyString())));
	}

	@Override
	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
