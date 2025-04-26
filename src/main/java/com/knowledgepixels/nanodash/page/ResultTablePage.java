package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TitleBar;

public class ResultTablePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/resulttable";

	private final String query;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public ResultTablePage(final PageParameters parameters) {
		super(parameters);

		query = parameters.get("query").toString();
		add(new TitleBar("titlebar", this, null));
		final String shortName = query.replaceFirst("^.*/", "");
		add(new Label("queryname", shortName));
		add(new Label("pagetitle", shortName + " (result table) | nanodash"));

		add(QueryResultTable.createComponent("table", query));
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
