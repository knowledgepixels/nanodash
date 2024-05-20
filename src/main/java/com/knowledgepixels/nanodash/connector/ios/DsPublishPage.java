package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPublishPage;

public class DsPublishPage extends ConnectorPublishPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/publish";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public DsPublishPage(final PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected ConnectorConfig getConfig() {
		return DsConfig.get();
	}

}
