package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class NanopubPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public NanopubPage() {
		this(new PageParameters());
	}

	public NanopubPage(PageParameters parameters) {
		super(parameters);
	}

}
