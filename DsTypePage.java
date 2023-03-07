package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class DsTypePage extends TypePage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/type";

	public DsTypePage() {
	}

	public DsTypePage(PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return DsConfig.get();
	}

}
