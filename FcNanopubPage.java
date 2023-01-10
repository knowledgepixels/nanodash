package org.petapico.nanobench.connector.ios;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.petapico.nanobench.NanobenchSession;
import org.petapico.nanobench.NanopubElement;
import org.petapico.nanobench.NanopubItem;
import org.petapico.nanobench.TitleBar;
import org.petapico.nanobench.Utils;

public class FcNanopubPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector-ios-fc-np";

	public FcNanopubPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanobenchSession session = NanobenchSession.get();
		session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);

		final String ref = parameters.get("id").toString();

		try {
			Nanopub np = Utils.getAsNanopub(ref);
			add(new NanopubItem("nanopub", new NanopubElement(np), false, true));
			String uri = np.getUri().stringValue();
			String shortId = "np:" + Utils.getShortNanopubId(uri);
			add(new ExternalLink("np-link", uri, uri));
			add(new ExternalLink("word-np-link", uri, shortId));
			add(new Label("latex-np-uri", uri));
			add(new Label("latex-np-label", shortId.replace("_", "\\_")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
