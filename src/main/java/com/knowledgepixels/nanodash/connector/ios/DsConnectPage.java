package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.connector.base.ConnectPage;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;

public class DsConnectPage extends ConnectPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/connect";

	public DsConnectPage(Nanopub np, PageParameters params) {
		super(np, params);
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
