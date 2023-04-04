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


public class ConnectorListPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connectorlist";

	private static final Map<Class<? extends WebPage>,String> connectors = new HashMap<>();

	public static void addConnector(Class<? extends WebPage> c, String label) {
		connectors.put(c, label);
	}

	public static int getConnectorCount() {
		return connectors.size();
	}

	public ConnectorListPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		List<Class<? extends WebPage>> connectorList = new ArrayList<>(connectors.keySet());
		
		add(new DataView<Class<? extends WebPage>>("connectors", new ListDataProvider<Class<? extends WebPage>>(connectorList)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Class<? extends WebPage>> item) {
				Class<? extends WebPage> c = item.getModelObject();
				BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("connectorlink", c);
				l.add(new Label("connectortext", connectors.get(c)));
				item.add(l);
			}

		});
	}

}
