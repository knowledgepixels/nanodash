package org.petapico;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class FreeTextSearchPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private TextField<String> searchField;
	private Model<String> progress;
	private boolean nanopubsReady = false;

	public FreeTextSearchPage(final PageParameters parameters) {

		final String searchText = parameters.get("query").toString();
		
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

		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
		progressLabel.setOutputMarkupId(true);
		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.ofMillis(1000)));
		add(progressLabel);

		final List<NanopubElement> nanopubs = new ArrayList<>();

		add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isContentReady() {
				return nanopubsReady;
			};

			@Override
			protected Duration getUpdateInterval() {
				return Duration.ofMillis(1000);
			};

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				progress.setObject("");
				return new NanopubResults(markupId, nanopubs);
			}
		});
		

		Thread loadContent = new Thread() {
			@Override
			public void run() {
				Map<String,String> nanopubParams = new HashMap<>();
				List<Map<String,String>> nanopubResults = new ArrayList<>();
				String s = searchText;
				if (s != null) {
					s = s.trim();
					if (s.matches("https?://[^\\s]+")) {
						System.err.println("URI QUERY: " + s);
						nanopubParams.put("ref", s);
						nanopubResults = ApiAccess.getRecent("find_nanopubs_with_uri", nanopubParams, progress);
					} else {
						String freeTextQuery = getFreeTextQuery(s);
						if (!freeTextQuery.isEmpty()) {
							System.err.println("FREE TEXT QUERY: " + freeTextQuery);
							nanopubParams.put("text", freeTextQuery);
							nanopubResults = ApiAccess.getRecent("find_nanopubs_with_text", nanopubParams, progress);
						}
					}
				}
				while (!nanopubResults.isEmpty() && nanopubs.size() < 10) {
					String npUri = nanopubResults.remove(0).get("np");
					nanopubs.add(new NanopubElement(npUri));
				}
				nanopubsReady = true;
			}
		};
		loadContent.start();
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

}
