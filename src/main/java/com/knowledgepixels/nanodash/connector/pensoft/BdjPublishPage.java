package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.ConnectorPublishPage;

public class BdjPublishPage extends ConnectorPublishPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/bdj/publish";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public BdjPublishPage(final PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected ConnectorConfig getConfig() {
		return BdjConfig.get();
	}

}
