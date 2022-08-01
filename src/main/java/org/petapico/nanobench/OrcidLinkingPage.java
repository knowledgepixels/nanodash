package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class OrcidLinkingPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public OrcidLinkingPage(final PageParameters parameters) {
		super();
		add(new TitleBar("titlebar"));
		if (!NanobenchSession.get().isProfileComplete()) {
			throw new RedirectToUrlException("./profile");
		}
		final NanobenchSession session = NanobenchSession.get();
		String introLink = "";
		if (session.getIntroNanopubs() != null && !session.getIntroNanopubs().isEmpty()) {
			// TODO Consider all intro nanopubs:
			introLink = session.getIntroNanopubs().values().iterator().next().getNanopub().getUri().stringValue();
		}
		add(new Label("introuri", introLink));
	}

}
