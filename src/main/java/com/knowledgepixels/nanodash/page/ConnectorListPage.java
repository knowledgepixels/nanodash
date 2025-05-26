package com.knowledgepixels.nanodash.page;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.ProjectList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

public class ConnectorListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connectorlist";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private static final String[] journals = new String[] { "ios/ds", "pensoft/bdj", "pensoft/rio" };

	public static int getConnectorCount() {
		return journals.length;
	}

	public ConnectorListPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, "connectors"));

		add(new DataView<String>("connectors", new ListDataProvider<String>(Arrays.asList(journals))) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<String> item) {
				String journalId = item.getModelObject();
				ConnectorConfig config = ConnectorConfig.get(journalId);
				BookmarkablePageLink<Void> l = new BookmarkablePageLink<>("connectorlink", GenOverviewPage.class, new PageParameters().add("journal", journalId));
				l.add(new Image("logo", new PackageResourceReference(config.getClass(), config.getLogoFileName())));
				l.add(new Label("connectortext", config.getJournalName()));
				item.add(l);
			}

		});

		final HashMap<String,String> noParams = new HashMap<>();
		final String queryName = "get-projects";
		ApiResponse qResponse = ApiCache.retrieveResponse(queryName, noParams);
		if (qResponse != null) {
			add(new ProjectList("projects", qResponse));
		} else {
			add(new ApiResultComponent("projects", queryName, noParams) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new ProjectList(markupId, response);
				}
			});

		}
	}

}
