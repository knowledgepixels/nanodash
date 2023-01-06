package org.petapico.nanobench.connector.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.petapico.nanobench.ApiAccess;
import org.petapico.nanobench.ApiResponse;
import org.petapico.nanobench.ApiResponseEntry;
import org.petapico.nanobench.NanobenchSession;
import org.petapico.nanobench.TitleBar;

import com.opencsv.exceptions.CsvValidationException;

public class ConnectorTestPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector-test";

	private static final String apiUrl = "https://grlc.petapico.org/api-git/knowledgepixels/connectortest-nanopub-api/";

	public ConnectorTestPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanobenchSession session = NanobenchSession.get();
		session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);

		Map<String,String> params = new HashMap<>();
		params.put("creator", session.getUserIri().stringValue());
		try {
			ApiResponse resp = ApiAccess.getAll(apiUrl, "get-formalization-nanopubs", params);
	
			add(new DataView<ApiResponseEntry>("formalization-nps", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				protected void populateItem(Item<ApiResponseEntry> item) {
					ApiResponseEntry e = item.getModelObject();
					PageParameters params = new PageParameters();
					params.add("id", e.get("np"));
					BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("nplink", ConnectorNanopubTestPage.class, params);
					l.add(new Label("nplinktext", "\"" + e.get("label") + "\""));
					item.add(l);
				}
	
			});
		} catch (IOException|CsvValidationException ex) {
			// TODO Report error somehow...
			add(new Label("formalization-nps"));
		}
	}

}
