package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.OverviewPage;


public class ConnectorListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connectorlist";

	private static final Map<Class<? extends OverviewPage>,String> connectors = new HashMap<>();

	public static void addConnector(Class<? extends OverviewPage> c, String label) {
		connectors.put(c, label);
	}

	public static int getConnectorCount() {
		return connectors.size();
	}

	public ConnectorListPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		List<Class<? extends OverviewPage>> connectorList = new ArrayList<>(connectors.keySet());
		
		add(new DataView<Class<? extends OverviewPage>>("connectors", new ListDataProvider<Class<? extends OverviewPage>>(connectorList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Class<? extends OverviewPage>> item) {
				Class<? extends OverviewPage> c = item.getModelObject();
				BookmarkablePageLink<OverviewPage> l = new BookmarkablePageLink<OverviewPage>("connectorlink", c);
				l.add(new Label("connectortext", connectors.get(c)));
				item.add(l);
			}

		});
	}

}
