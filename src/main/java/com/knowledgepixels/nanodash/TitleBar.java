package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id, NanodashPage page) {
		super(id);
		add(new ProfileItem("profile", page));

		WebMarkupContainer mychannel = new WebMarkupContainer("mychannel");
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
