package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.TitleBar;

public abstract class OverviewPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public OverviewPage(PageParameters parameters) {
		super(parameters);
		if (parameters == null) return;

		add(new TitleBar("titlebar"));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));
	}

}
