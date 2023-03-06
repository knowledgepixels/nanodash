package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.NanopubPage;
import com.knowledgepixels.nanodash.connector.base.TypePage;

public class DsNanopubPage extends NanopubPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/np";

	public DsNanopubPage(final PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected Class<? extends TypePage> getTypePageClass() {
		return DsTypePage.class;
	}

	@Override
	protected String getTypePageMountPath() {
		return DsTypePage.MOUNT_PATH;
	}

	@Override
	protected String getLogoFileName() {
		return DsOverviewPage.logofileName;
	}

	@Override
	protected String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected String getApiUrl() {
		return DsOverviewPage.apiUrl;
	}

	

}
