package com.knowledgepixels.nanodash;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	private String highlight;

	public TitleBar(String id, NanodashPage page, String highlight) {
		super(id);
		this.highlight = highlight;
		add(new ProfileItem("profile", page));

		createContainer("mychannel").setVisible(!NanodashPreferences.get().isReadOnlyMode());
		createContainer("users");
		createContainer("connectors").setVisible(ConnectorListPage.getConnectorCount() > 0);
		createContainer("publish").setVisible(!NanodashPreferences.get().isReadOnlyMode());
		createContainer("search");
	}

	private WebMarkupContainer createContainer(String id) {
		WebMarkupContainer c = new WebMarkupContainer(id);
		if(id.equals(highlight)) {
			c.add(new AttributeAppender("class", "selected"));
		}
		add(c);
		return c;
	}

}
