package com.knowledgepixels.nanodash;

import org.apache.wicket.request.mapper.parameter.PageParameters;

public class GroupDemoPageSoc extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/groupdemo-soc";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public GroupDemoPageSoc(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this));
	}

}
