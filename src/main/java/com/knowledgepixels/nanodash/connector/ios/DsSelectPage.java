package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.SelectPage;

public class DsSelectPage extends SelectPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/select";

	public DsSelectPage(PageParameters params) {
		super(params);
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return DsConfig.get();
	}

	@Override
	protected String[] getOptions() {
		return new String[] { "linkflowsrel", "crel", "superpattern", "aida", "biorel", "ml", "classdef", "inddef" };
	}

}
