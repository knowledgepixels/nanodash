package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.knowledgepixels.nanodash.TitleBar;

public abstract class ConnectorPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public ConnectorPage() {
	}

	public ConnectorPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));
	}
	protected abstract String getMountPath();

	protected abstract ConnectorConfig getConfig();

}
