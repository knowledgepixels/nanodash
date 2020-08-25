package org.petapico.nanobench;

import java.net.URLEncoder;

import org.apache.commons.codec.Charsets;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;

public class Link extends Panel {
	
	private static final long serialVersionUID = 1L;

	public Link(String id, String uri) {
		super(id);
		add(new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8), IriItem.getShortNameFromURI(uri)));
		add(new Label("iri", uri));
	}

}
