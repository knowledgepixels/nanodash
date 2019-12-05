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
		List<String[]> nanopubResults = new ArrayList<>();
		if (searchText != null) {
			String searchQuery = getSearchQuery(searchText);
			if (!searchQuery.isEmpty()) {
				nanopubParams.put("text", searchQuery);
				nanopubResults = ApiAccess.getAllFull("find_nanopubs_with_text", nanopubParams);
			}
		}
		Collections.sort(nanopubResults, nanopubResultComparator);

		List<NanopubElement> nanopubs = new ArrayList<>();
		for (int i = 0 ; i < 10 && i < nanopubResults.size() ; i++) {
			nanopubs.add(new NanopubElement(nanopubResults.get(i)[0]));
		}

		add(new DataView<NanopubElement>("nanopubs", new ListDataProvider<NanopubElement>(nanopubs)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<NanopubElement> item) {
				item.add(new NanopubItem("nanopub", item.getModelObject(), false));
			}

		});
	}

	private static String getSearchQuery(String searchText) {
		String searchQuery = "";
		String previous = "AND";
		String preprocessed = "";
		boolean inQuote = true;
		for (String s : searchText.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")").replaceAll("@", "").split("\"")) {
			inQuote = !inQuote;
			if (inQuote) {
				s = "\\\"" + String.join("@", s.split("[^\\p{L}0-9\\-_\\(\\)@]+")) + "\\\"";
			}
			preprocessed += s;
		}
		preprocessed = preprocessed.trim();
		for (String s : preprocessed.split(" ")) {
			if (s.matches("[0-9].*")) continue;
			if (!s.matches("AND|OR|\\(+|\\)+|\\(?NOT")) {
				if (s.toLowerCase().matches("and|or|not")) {
					// ignore lower-case and/or/not
					continue;
				}
				if (!previous.toLowerCase().matches("and|or|\\(?not")) {
					searchQuery += " AND";
				}
			}
			searchQuery += " " + s;
			previous = s;
		}
		searchQuery = searchQuery.replaceAll("@", " ").trim();
		System.err.println("QUERY: " + searchQuery);
		return searchQuery;
	}

	private static Comparator<String[]> nanopubResultComparator = new Comparator<String[]>() {
		@Override
		public int compare(String[] e1, String[] e2) {
			return e2[2].compareTo(e1[2]);
		}
	};

}
