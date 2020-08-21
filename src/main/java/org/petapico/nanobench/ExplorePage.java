package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ExplorePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public ExplorePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final String id = parameters.get("id").toString();
		ExternalLink link = new ExternalLink("urilink", id);
		link.add(new Label("urilinktext", id));
		add(link);

		Map<String,String> nanopubParams = new HashMap<>();
		List<Map<String,String>> nanopubResults = new ArrayList<>();
		nanopubParams.put("ref", id);
		try {
			nanopubResults = ApiAccess.getAll("get_uri_usage", nanopubParams);
			int subjCount = Integer.valueOf(nanopubResults.get(0).get("subj"));
			int relCount = Integer.valueOf(nanopubResults.get(0).get("pred"));
			int objCount = Integer.valueOf(nanopubResults.get(0).get("obj"));
			int classCount = Integer.valueOf(nanopubResults.get(0).get("class"));
			int indCount = subjCount + objCount - classCount;
			add(new Label("indcount", indCount));
			add(new Label("classcount", classCount));
			add(new Label("relcount", relCount));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
