package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id) {
		super(id);
		add(new ProfileItem("profile"));

		WebMarkupContainer mychannel = new WebMarkupContainer("mychannel");
		if (NanodashSession.get().getUserIri() != null) {
			PageParameters params = new PageParameters();
			params.add("id", NanodashSession.get().getUserIri());
			mychannel.add(new BookmarkablePageLink<UserPage>("mychannellink", UserPage.class, params));
		} else {
			mychannel.add(new BookmarkablePageLink<UserPage>("mychannellink", ProfilePage.class));
		}
		mychannel.setVisible(!NanodashPreferences.get().isReadOnlyMode());
		add(mychannel);

		WebMarkupContainer connectors = new WebMarkupContainer("connectors");
		connectors.setVisible(ConnectorListPage.getConnectorCount() > 0);
		add(connectors);

		WebMarkupContainer publish = new WebMarkupContainer("publish");
		publish.setVisible(!NanodashPreferences.get().isReadOnlyMode());
		add(publish);
	}

}
