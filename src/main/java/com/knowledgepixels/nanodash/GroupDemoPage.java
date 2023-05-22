package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class GroupDemoPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/groupdemo";

	public GroupDemoPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
	}

}
