package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id, NanodashPage page) {
		super(id);
		add(new ProfileItem("profile", page));

		add(new WebMarkupContainer("mychannel").setVisible(!NanodashPreferences.get().isReadOnlyMode()));
		add(new WebMarkupContainer("connectors").setVisible(ConnectorListPage.getConnectorCount() > 0));
		add(new WebMarkupContainer("publish").setVisible(!NanodashPreferences.get().isReadOnlyMode()));
	}

}
