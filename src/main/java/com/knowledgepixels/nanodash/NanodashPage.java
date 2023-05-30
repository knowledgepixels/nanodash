package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class NanodashPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public abstract String getMountPath();

	protected NanodashPage(PageParameters parameters) {
		super(parameters);
	}

}
