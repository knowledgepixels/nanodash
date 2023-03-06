package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.connector.base.TypePage;

public class DsTypePage extends TypePage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds/type";

	public DsTypePage(final PageParameters parameters) {
		super(parameters);
	}

	@Override
	protected String getLogoFileName() {
		return "DsLogo.png";
	}

	@Override
	protected String getMountPath() {
		return MOUNT_PATH;
	}

	@Override
	protected String getApiUrl() {
		return DsOverviewPage.apiUrl;
	}

	@Override
	protected Class<? extends WebPage> getNanopubPageClass() {
		return DsNanopubPage.class;
	}

}
