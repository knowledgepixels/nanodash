package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.ConnectorPage;

public class DsOverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds";

	public static final String apiUrl = "https://grlc.petapico.org/api-git/knowledgepixels/ds-nanopub-api/";

	public static final String logofileName = "DsLogo.png";

	public DsOverviewPage(final PageParameters params) {
		super(params);
	}

	@Override
	protected String getLogoFileName() {
		return logofileName;
	}

	@Override
	protected String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected String getApiUrl() {
		return apiUrl;
	}

}
