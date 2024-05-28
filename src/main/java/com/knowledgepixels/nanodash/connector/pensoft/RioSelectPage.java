package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.SelectPage;

public class RioSelectPage extends SelectPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/rio/select";

	public RioSelectPage(PageParameters params) {
		super(params);
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return RioConfig.get();
	}

	@Override
	protected String[] getOptions() {
		return new String[] { "linkflowsrel", "crel", "superpattern", "aida", "biorel", "eqrel", "classdef", "inddef" };
	}

}
