package org.petapico.nanobench.connector.ios;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.petapico.nanobench.NanobenchSession;
import org.petapico.nanobench.NanopubElement;
import org.petapico.nanobench.NanopubItem;
import org.petapico.nanobench.TitleBar;
import org.petapico.nanobench.Utils;

public class FcConnectorNanopubPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector-ios-fc-np";

	public FcConnectorNanopubPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanobenchSession session = NanobenchSession.get();
		session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);

		final String ref = parameters.get("id").toString();

		try {
			Nanopub np = Utils.getAsNanopub(ref);
			add(new NanopubItem("nanopub", new NanopubElement(np), false, false));
			add(new ExternalLink("np-link", np.getUri().stringValue(), np.getUri().stringValue()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
