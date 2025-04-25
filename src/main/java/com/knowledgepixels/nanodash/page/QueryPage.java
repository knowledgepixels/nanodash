package com.knowledgepixels.nanodash.page;

import java.net.URLEncoder;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.component.QueryParamField;
import com.knowledgepixels.nanodash.component.TitleBar;

public class QueryPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/query";

	private final Form<Void> paramForm;
	private final List<QueryParamField> paramFields;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public QueryPage(final PageParameters parameters) {
		super(parameters);
		add(new TitleBar("titlebar", this, null));
		add(new Label("pagetitle", "Query Info | nanodash"));

		String npId = parameters.get("id").toString();
		GrlcQuery q = new GrlcQuery(npId);
		String placeholdersString = "";
		for (String p : q.getPlaceholdersList()) {
			placeholdersString += p + " ";
		}
		add(new Label("placeholders", placeholdersString));
		// TODO Replace hard-coded Nanopub Query URL with dynamic solution:
		String editLink = q.getEndpoint().stringValue().replaceFirst("^.*/repo/", "https://query.petapico.org/tools/") + "/yasgui.html#query=" + URLEncoder.encode(q.getSparql(), Charsets.UTF_8);
		add(new ExternalLink("editlink", editLink));

		paramForm = new Form<Void>("paramform") {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				//paramform.getFeedbackMessages().clear();
			}

			protected void onSubmit() {
				System.err.println("Params submitted");
				for (QueryParamField f : paramFields) {
					System.err.println(f.getValue());
				}
				// TODO
			}

			@Override
		    protected void onValidate() {
				super.onValidate();
				// ...
			}

		};
		paramForm.setOutputMarkupId(true);

		paramFields = q.createParamFields("paramfield");
		paramForm.add(new ListView<QueryParamField>("paramfields", paramFields) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<QueryParamField> item) {
				item.add(item.getModelObject());
			}

		});
		add(paramForm);
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
