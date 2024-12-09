package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPage;
import com.knowledgepixels.nanodash.connector.gen.GenOverviewPage;

public class DsOverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds";

	public DsOverviewPage(PageParameters params) {
		super(params);
		setResponsePage(GenOverviewPage.class, params.add("journal", "ios/ds"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return DsConfig.get();
	}

}
