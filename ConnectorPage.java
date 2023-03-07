package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class ConnectorPage extends WebPage {

	private static final long serialVersionUID = 1L;

	private final PageParameters parameters;

	public ConnectorPage(PageParameters parameters) {
		if (parameters == null) {
			this.parameters = new PageParameters();
		} else {
			this.parameters = parameters;
		}
	}

	protected abstract String getMountPath();

	protected abstract ConnectorConfig getConfig();

	public PageParameters getParams() {
		return parameters;
	}

}
