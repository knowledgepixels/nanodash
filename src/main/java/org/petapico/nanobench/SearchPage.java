package org.petapico.nanobench;

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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;

public class SearchPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private TextField<String> searchField;
	private CheckBox filterUser;
	private Model<String> progress;

	public SearchPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		final String searchText = parameters.get("query").toString();
		final Boolean filterCheck = Boolean.valueOf(parameters.get("filter").toString());
		
		final User currentUser = User.getUser(ProfilePage.getUserIri().toString());
		
		Form<?> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				String searchText = searchField.getModelObject().trim();
				Boolean filterCheck = filterUser.getModelObject();
				PageParameters params = new PageParameters();
				params.add("query", searchText);
				params.add("filter", filterCheck);
				setResponsePage(SearchPage.class, params);
			}
		};
		add(form);

		form.add(searchField = new TextField<String>("search", Model.of(searchText)));
		form.add(filterUser = new CheckBox("filter", Model.of(filterCheck)));

		// TODO: Progress bar doesn't update at the moment:
		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
		progressLabel.setOutputMarkupId(true);
		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)));
		add(progressLabel);

		if (searchText == null || searchText.isEmpty()) {
			add(new Label("nanopubs", "Enter a search term above."));
		} else {
			add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public NanopubResults getLazyLoadComponent(String markupId) {
					Map<String,String> nanopubParams = new HashMap<>();
					List<ApiResponseEntry> nanopubResults = new ArrayList<>();
					String s = searchText;
					if (s != null) {
						s = s.trim();
						if (s.matches("https?://[^\\s]+")) {
							System.err.println("URI QUERY: " + s);
							nanopubParams.put("ref", s);
							if (Boolean.TRUE.equals(filterCheck)) {
								System.err.println("Filter for PUBKEY: " + currentUser.getPubkeyString());
								nanopubParams.put("pubkey", currentUser.getPubkeyString());
							}
							try {
								// nanopubResults = ApiAccess.getAll("find_nanopubs_with_uri", nanopubParams).getData();
								nanopubResults = ApiAccess.getAll("find_signed_nanopubs_with_uri", nanopubParams).getData();
							} catch (Exception ex) {
								ex.printStackTrace();
							}
//							nanopubResults = ApiAccess.getRecent("find_nanopubs_with_uri", nanopubParams, progress);
						} else {
							String freeTextQuery = getFreeTextQuery(s);
							if (!freeTextQuery.isEmpty()) {
								System.err.println("FREE TEXT QUERY: " + freeTextQuery);
								nanopubParams.put("text", freeTextQuery);
								if (filterCheck != null && Boolean.TRUE.equals(filterCheck)) {
									System.err.println("Filter for PUBKEY: " + currentUser.getPubkeyString());
									nanopubParams.put("pubkey", currentUser.getPubkeyString());
								}
								try {
									// nanopubResults = ApiAccess.getAll("find_nanopubs_with_text", nanopubParams).getData();
									nanopubResults = ApiAccess.getAll("find_signed_nanopubs_with_text", nanopubParams).getData();
								} catch (Exception ex) {
									ex.printStackTrace();
								}
//								nanopubResults = ApiAccess.getRecent("find_nanopubs_with_text", nanopubParams, progress);
							}
						}
					}
					nanopubResults.sort(new ApiResponseEntry.DataComparator());
					List<String> nanopubIds = new ArrayList<>();
					while (!nanopubResults.isEmpty() && nanopubIds.size() < 10) {
						String npUri = nanopubResults.remove(0).get("np");
						if (!nanopubIds.contains(npUri)) nanopubIds.add(npUri);
					}
					progress.setObject("");
					if (nanopubIds.isEmpty()) progress.setObject("nothing found");
					List<NanopubElement> nanopubs = new ArrayList<>();
					for (String id : nanopubIds) nanopubs.add(new NanopubElement(id));
					return new NanopubResults(markupId, nanopubs);
				}
			});

		}
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
