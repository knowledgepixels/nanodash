package com.knowledgepixels.nanodash.connector.pensoft;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.knowledgepixels.nanodash.ConnectorListPage;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;

public class BdjOverviewPage extends OverviewPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/bdj";

	static {
		ConnectorListPage.addConnector(BdjOverviewPage.class, "Nanopublishing in Biodiversity Data Journal (BDJ) at Pensoft");
	}

	public BdjOverviewPage(PageParameters params) {
		super(params);

		try {
			ApiResponse resp = callApi("get-biodiv-nanopubs", new HashMap<>());

			ArrayList<ApiResponseEntry> respList = new ArrayList<>();
			for (ApiResponseEntry a : resp.getData()) {
				if (respList.size() == 10) break;
				respList.add(a);
			}
	
			add(new DataView<ApiResponseEntry>("candidates", new ListDataProvider<ApiResponseEntry>(respList)) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				protected void populateItem(Item<ApiResponseEntry> item) {
					ApiResponseEntry e = item.getModelObject();
					PageParameters params = new PageParameters().add("id", e.get("np")).add("mode", "candidate");
					BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("candidatelink", getConfig().getNanopubPage().getClass(), params);
					l.add(new Label("candidatelinktext", "\"" +  e.get("label") + "\""));
					item.add(l);
					String username = User.getShortDisplayName(null, e.get("pubkey"));
					item.add(new Label("candidatenote", "by " + username + " on " + e.get("date").substring(0, 10)));
				}
	
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return BdjConfig.get();
	}

}
