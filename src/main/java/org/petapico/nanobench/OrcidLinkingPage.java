package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class OrcidLinkingPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public OrcidLinkingPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
	}

}
