package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.ConnectorListPage;
import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.OverviewPage;
import com.knowledgepixels.nanodash.connector.ios.DsOverviewPage;

public class RioOverviewPage extends OverviewPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/rio";

	static {
		ConnectorListPage.addConnector(RioOverviewPage.class, "Nanopublishing in RIO Journal at Pensoft");
	}

	public RioOverviewPage(PageParameters params) {
		super(params);
	}

	@Override
	protected String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return RioConfig.get();
	}

}
