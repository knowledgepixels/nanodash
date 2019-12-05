package org.petapico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class FreeTextSearchPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private TextField<String> searchField;

	public FreeTextSearchPage(final PageParameters parameters) {
		String searchText = parameters.get("query").toString();
		
		Form<?> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				String searchText = searchField.getModelObject().trim();
				PageParameters params = new PageParameters();
				params.add("query", searchText);
				setResponsePage(FreeTextSearchPage.class, params);
			}
		};
		add(form);

		form.add(searchField = new TextField<String>("search", Model.of(searchText)));

		Map<String,String> nanopubParams = new HashMap<>();
		List<Map<String,String>> nanopubResults = new ArrayList<>();
		if (searchText != null) {
			searchText = searchText.trim();
			if (searchText.matches("https?://[^\\s]+")) {
				System.err.println("URI QUERY: " + searchText);
				nanopubParams.put("ref", searchText);
				nanopubResults = ApiAccess.getAllFull("find_nanopubs_with_uri", nanopubParams);
			} else {
				String freeTextQuery = getFreeTextQuery(searchText);
				if (!freeTextQuery.isEmpty()) {
					System.err.println("FREE TEXT QUERY: " + freeTextQuery);
					nanopubParams.put("text", freeTextQuery);
					nanopubResults = ApiAccess.getAllFull("find_nanopubs_with_text", nanopubParams);
				}
			}
		}
		Collections.sort(nanopubResults, nanopubResultComparator);
		List<NanopubElement> nanopubs = new ArrayList<>();
		Map<String,Boolean> nanopubUris = new HashMap<>();
		while (!nanopubResults.isEmpty() && nanopubs.size() < 10) {
			String npUri = nanopubResults.remove(0).get("np");
			if (nanopubUris.containsKey(npUri)) continue;
			nanopubUris.put(npUri, true);
			nanopubs.add(new NanopubElement(npUri));
		}

		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject(), false));
			}

		});
	}

	private static String getFreeTextQuery(String searchText) {
		String freeTextQuery = "";
		String previous = "AND";
		String preprocessed = "";
		boolean inQuote = true;
		for (String s : searchText.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")").replaceAll("@", "").split("\"")) {
			inQuote = !inQuote;
			if (inQuote) {
				s = "\\\"" + String.join("@", s.split("[^\\p{L}0-9\\-_]+")) + "\\\"";
			}
			preprocessed += s;
		}
		preprocessed = preprocessed.trim();
		for (String s : preprocessed.split("[^\\p{L}0-9\\-_\\(\\)@\\\"\\\\]+")) {
			if (s.matches("[0-9].*")) continue;
			if (!s.matches("AND|OR|\\(+|\\)+|\\(?NOT")) {
				if (s.toLowerCase().matches("and|or|not")) {
					// ignore lower-case and/or/not
					continue;
				}
				if (!previous.matches("AND|OR|\\(?NOT")) {
					freeTextQuery += " AND";
				}
			}
			freeTextQuery += " " + s.toLowerCase();
			previous = s;
		}
		freeTextQuery = freeTextQuery.replaceAll("@", " ").trim();
		return freeTextQuery;
	}

	private static Comparator<Map<String,String>> nanopubResultComparator = new Comparator<Map<String,String>>() {
		@Override
		public int compare(Map<String,String> e1, Map<String,String> e2) {
			return e2.get("date").compareTo(e1.get("date"));
		}
	};

}
