package com.knowledgepixels.nanodash.connector.pensoft;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.eclipse.rdf4j.model.IRI;
import com.knowledgepixels.nanodash.ApiAccess;
import com.knowledgepixels.nanodash.ApiResponse;
import com.knowledgepixels.nanodash.ApiResponseEntry;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.TitleBar;

import com.opencsv.exceptions.CsvValidationException;

public class RioOverviewPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/rio";

	protected static final String apiUrl = "https://grlc.petapico.org/api-git/knowledgepixels/rio-nanopub-api/";

	public RioOverviewPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanodashSession session = NanodashSession.get();

		add(new Image("logo", new PackageResourceReference(this.getClass(), "RioLogo.svg")));
		IRI userIri = session.getUserIri();
		Map<String,String> params = null;
		if (userIri != null) {
			params = new HashMap<>();
			params.put("creator", userIri.stringValue());
		}

		if (userIri == null) {
			add(new WebMarkupContainer("superpattern-nps")
				.add(new ExternalLink("nplink", session.getLoginUrl(MOUNT_PATH, parameters))
					.add(new Label("nplinktext", "(login here to see your nanopublications)"))));
		} else {
			try {
				ApiResponse resp = ApiAccess.getAll(apiUrl, "get-superpattern-nanopubs", params);
		
				add(new DataView<ApiResponseEntry>("superpattern-nps", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
		
					private static final long serialVersionUID = 1L;
		
					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters();
						params.add("id", e.get("np"));
						BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("nplink", RioNanopubPage.class, params);
						l.add(new Label("nplinktext", "\"" + e.get("label") + "\", " + e.get("date").substring(0, 10)));
						item.add(l);
					}
		
				});
			} catch (IOException|CsvValidationException ex) {
				// TODO Report error somehow...
				add(new Label("superpattern-nps"));
			}
		}

		if (userIri == null) {
			add(new WebMarkupContainer("classdef-nps")
				.add(new ExternalLink("nplink", session.getLoginUrl(MOUNT_PATH, parameters))
					.add(new Label("nplinktext", "(login here to see your nanopublications)"))));
		} else {
			try {
				ApiResponse resp = ApiAccess.getAll(apiUrl, "get-classdef-nanopubs", params);
		
				add(new DataView<ApiResponseEntry>("classdef-nps", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
		
					private static final long serialVersionUID = 1L;
		
					@Override
					protected void populateItem(Item<ApiResponseEntry> item) {
						ApiResponseEntry e = item.getModelObject();
						PageParameters params = new PageParameters();
						params.add("id", e.get("np"));
						BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("nplink", RioNanopubPage.class, params);
						l.add(new Label("nplinktext", "\"" + e.get("label") + "\", " + e.get("date").substring(0, 10)));
						item.add(l);
					}
		
				});
			} catch (IOException|CsvValidationException ex) {
				// TODO Report error somehow...
				add(new Label("classdef-nps"));
			}
		}

	}

}
