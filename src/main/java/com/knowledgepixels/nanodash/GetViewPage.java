package com.knowledgepixels.nanodash;

import org.apache.wicket.request.mapper.parameter.PageParameters;

public class GetViewPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/get-view";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GetViewPage(final PageParameters parameters) {
		super(parameters);
		ViewPage.addNanopubItem(this, parameters);
	}

}
