package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;

public class FcOverviewPage extends OverviewPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/fc";

	static {
		//ConnectorListPage.addConnector(FcOverviewPage.class, "Nanopublishing in FAIR Connect journal at IOS Press");
	}

	public FcOverviewPage(PageParameters params) {
		super(params);
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return FcConfig.get();
	}

}
