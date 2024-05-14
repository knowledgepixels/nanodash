package com.knowledgepixels.nanodash;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	private String highlight;

	public TitleBar(String id, NanodashPage page, String highlight, String... pathUrls) {
		super(id);
		this.highlight = highlight;
		add(new ProfileItem("profile", page));

		createContainer("mychannel").setVisible(!NanodashPreferences.get().isReadOnlyMode());
		createContainer("users");
		createContainer("connectors").setVisible(ConnectorListPage.getConnectorCount() > 0);
		createContainer("publish").setVisible(!NanodashPreferences.get().isReadOnlyMode());
		createContainer("search");
		
		WebMarkupContainer titlePath = new WebMarkupContainer("titlepath");
		titlePath.setVisible(pathUrls.length > 0);
		if (pathUrls.length > 0) {
			titlePath.add(new ExternalLink("firstpathelement", pathUrls[0]));
			List<String> morePathElements = Arrays.asList(pathUrls).subList(1, pathUrls.length);
			titlePath.add(new DataView<String>("morepathelements", new ListDataProvider<String>(morePathElements)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(Item<String> item) {
					item.add(new ExternalLink("furtherpathelement", item.getModelObject(), "Element"));
				}

			});
		} else {
			titlePath.setVisible(false);
		}
		add(titlePath);
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
