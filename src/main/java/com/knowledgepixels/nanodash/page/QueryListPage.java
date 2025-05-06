package com.knowledgepixels.nanodash.page;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.QueryList;
import com.knowledgepixels.nanodash.component.TitleBar;

public class QueryListPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/queries";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private TextField<String> searchField;

	public QueryListPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, "search"));

		final String searchText = parameters.get("query").toString();
		
		Form<?> form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			protected void onSubmit() {
				String searchText = searchField.getModelObject().trim();
				PageParameters params = new PageParameters();
				params.add("query", searchText);
				setResponsePage(SearchPage.class, params);
			}
		};
		add(form);

		form.add(searchField = new TextField<String>("search", Model.of(searchText)));

		final HashMap<String,String> noParams = new HashMap<>();
		final String queryName = "get-queries";
		ApiResponse qResponse = ApiCache.retrieveResponse(queryName, noParams);
		if (qResponse != null) {
			add(new QueryList("queries", qResponse));
		} else {
			add(new ApiResultComponent("queries", queryName, noParams) {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getApiResultComponent(String markupId, ApiResponse response) {
					return new QueryList(markupId, response);
				}
			});

		}
	}

}
