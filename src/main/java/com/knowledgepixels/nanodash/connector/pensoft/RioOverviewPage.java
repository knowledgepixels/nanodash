package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

public class RioOverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/rio";

	public RioOverviewPage(PageParameters params) {
		super(params);
		setResponsePage(GenOverviewPage.class, params.add("journal", "pensoft/rio"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

}
