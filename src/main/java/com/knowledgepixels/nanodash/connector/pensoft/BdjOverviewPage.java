package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPage;
import com.knowledgepixels.nanodash.connector.gen.GenOverviewPage;

public class BdjOverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/bdj";

	public BdjOverviewPage(PageParameters params) {
		super(params);
		setResponsePage(GenOverviewPage.class, params.add("journal", "pensoft/bdj"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return BdjConfig.get();
	}

}
