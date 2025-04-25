package com.knowledgepixels.nanodash.page;

import java.net.URLEncoder;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.component.TitleBar;

public class QueryPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/query";

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
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
