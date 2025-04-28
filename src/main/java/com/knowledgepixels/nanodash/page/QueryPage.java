package com.knowledgepixels.nanodash.page;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TitleBar;

public class QueryPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/query";

	private final Form<Void> form;
	private final List<QueryParamField> paramFields;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public QueryPage(final PageParameters parameters) {
		super(parameters);
		add(new TitleBar("titlebar", this, null));
		add(new Label("pagetitle", "Query Info | nanodash"));

		final String id = parameters.get("id").toString();
		final String queryId = parameters.get("runquery").toString();
		final HashMap<String,String> queryParams = new HashMap<>();
		for (String paramKey : parameters.getNamedKeys()) {
			if (!paramKey.startsWith("queryparam_")) continue;
			queryParams.put(paramKey.replaceFirst("queryparam_", ""), parameters.get(paramKey).toString());
		}

		GrlcQuery q;
		if (id != null) {
			q = new GrlcQuery(id);
		} else {
			q = new GrlcQuery(queryId);
		}

		add(new Label("querylabel", q.getLabel()));
		add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", q.getNanopub().getUri().stringValue())));
		add(new Label("querydesc", q.getDescription()));

		form = new Form<Void>("form") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				//paramform.getFeedbackMessages().clear();
			}

			@Override
			protected void onSubmit() {
				PageParameters params = new PageParameters();
				params.add("runquery", q.getQueryId());
				for (QueryParamField f : paramFields) {
					if (f.getValue() == null) continue;
					System.err.println(f.getParamName() + ": " + f.getValue());
					params.add("queryparam_" + f.getParamName(), f.getValue());
				}
				setResponsePage(QueryPage.class, params);
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				// ...
			}

		};
		form.setOutputMarkupId(true);

		WebMarkupContainer paramContainer = new WebMarkupContainer("params");
		
		paramFields = q.createParamFields("paramfield");
		paramContainer.add(new ListView<QueryParamField>("paramfields", paramFields) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<QueryParamField> item) {
				QueryParamField f = item.getModelObject();
				f.getModel().setObject(parameters.get("queryparam_" + f.getParamName()).toString());
				item.add(item.getModelObject());
			}

		});
		paramContainer.setVisible(!paramFields.isEmpty());
		form.add(paramContainer);

		// TODO Replace hard-coded Nanopub Query URL with dynamic solution:
		String editLink = q.getEndpoint().stringValue().replaceFirst("^.*/repo/", "https://query.petapico.org/tools/") + "/yasgui.html#query=" + URLEncoder.encode(q.getSparql(), Charsets.UTF_8);
		form.add(new ExternalLink("yasgui", editLink));

		add(form);

		if (queryId == null) {
			add(new Label("resulttable").setVisible(false));
		} else {
			add(QueryResultTable.createComponent("resulttable", queryId, queryParams));
		}
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
