package org.petapico.nanobench.connector.test;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.petapico.nanobench.ExplorePage;
import org.petapico.nanobench.NanobenchSession;
import org.petapico.nanobench.NanopubElement;
import org.petapico.nanobench.NanopubItem;
import org.petapico.nanobench.TitleBar;
import org.petapico.nanobench.Utils;

public class ConnectorNanopubTestPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector/test/np";

	private static final String apiUrl = "https://grlc.petapico.org/api-git/knowledgepixels/connectortest-nanopub-api/";

	public ConnectorNanopubTestPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanobenchSession session = NanobenchSession.get();
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
