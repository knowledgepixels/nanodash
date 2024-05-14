package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.ios.DsConfig;
import com.knowledgepixels.nanodash.connector.pensoft.BdjConfig;
import com.knowledgepixels.nanodash.connector.pensoft.RioConfig;

public class ConnectorListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connectorlist";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private static final List<ConnectorConfig> connectors = new ArrayList<>();

	static {
		connectors.add(DsConfig.get());
		connectors.add(BdjConfig.get());
		connectors.add(RioConfig.get());
	}

	public static int getConnectorCount() {
		return connectors.size();
	}

	public ConnectorListPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, "connectors"));
		//add(new TitleBar("titlebar", this, "connectors", "http://example.com/1", "http://example.com/2"));

		add(new DataView<ConnectorConfig>("connectors", new ListDataProvider<ConnectorConfig>(connectors)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<ConnectorConfig> item) {
				ConnectorConfig c = item.getModelObject();
				BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("connectorlink", c.getOverviewPage().getClass());
				l.add(new Image("logo", new PackageResourceReference(c.getOverviewPage().getClass(), c.getLogoFileName())));
				l.add(new Label("connectortext", c.getJournalName()));
				item.add(l);
			}

		});
	}

}
