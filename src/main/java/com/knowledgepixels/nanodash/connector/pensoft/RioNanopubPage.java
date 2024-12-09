package com.knowledgepixels.nanodash.connector.pensoft;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.gen.GenNanopubPage;

public class RioNanopubPage extends NanopubPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/pensoft/rio/np";

	public RioNanopubPage(final PageParameters params) {
		super(params);
		setResponsePage(GenNanopubPage.class, params.add("journal", "pensoft/rio"));
	}

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected ConnectorConfig getConfig() {
		return RioConfig.get();
	}

}
