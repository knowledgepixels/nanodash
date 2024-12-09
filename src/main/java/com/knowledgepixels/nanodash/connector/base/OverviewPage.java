package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class OverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public OverviewPage() {
		this(new PageParameters());
	}

	public OverviewPage(PageParameters parameters) {
		super(parameters);
	}

}
