package com.knowledgepixels.nanodash.connector.test;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.NanopubItem;
import com.knowledgepixels.nanodash.TitleBar;
import com.knowledgepixels.nanodash.Utils;

public class ConnectorNanopubTestPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/test/np";

	public ConnectorNanopubTestPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanodashSession session = NanodashSession.get();
		session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);

		final String ref = parameters.get("id").toString();

		try {
			Nanopub np = Utils.getAsNanopub(ref);
			add(new NanopubItem("nanopub", new NanopubElement(np), false, false));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
