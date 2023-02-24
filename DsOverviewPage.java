package com.knowledgepixels.nanodash.connector.ios;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import com.knowledgepixels.nanodash.TitleBar;

public class DsOverviewPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/ios/ds";

	public static final String apiUrl = "https://grlc.petapico.org/api-git/knowledgepixels/ds-nanopub-api/";

	public DsOverviewPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		add(new Image("logo", new PackageResourceReference(this.getClass(), "DsLogo.png")));

	}

}
