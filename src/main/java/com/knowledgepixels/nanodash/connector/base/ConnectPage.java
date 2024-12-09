package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

public abstract class ConnectPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public ConnectPage(Nanopub np, PageParameters parameters) {
		super(parameters);
	}

}
