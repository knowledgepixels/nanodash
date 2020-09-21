package org.petapico.nanobench;

import java.net.URLEncoder;

import org.apache.commons.codec.Charsets;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.Nanopub;

public class NanobenchLink extends Panel {
	
	private static final long serialVersionUID = 1L;

	public NanobenchLink(String id, String uri, Nanopub np) {
		super(id);
		try {
			if (np != null && uri.equals(np.getUri().stringValue())) {
				ExternalLink link = new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), "this");
				link.add(new AttributeAppender("style", "background: #666; color: #fff; padding: 0 5px; border-radius: 7px;"));
				add(link);
				add(new Label("iri", uri));
			} else if (np != null && uri.equals(np.getAssertionUri().stringValue())) {
				ExternalLink link = new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), "this assertion");
				link.add(new AttributeAppender("class", " nanopub-assertion "));
				link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
				add(link);
				add(new Label("iri", uri));
			} else if (uri.equals(Nanopub.HAS_ASSERTION_URI.stringValue())) {
				ExternalLink link = new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), "assertion");
				link.add(new AttributeAppender("class", " nanopub-assertion "));
				link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
				add(link);
				add(new Label("iri", uri));
			} else if (uri.equals(Nanopub.HAS_PROVENANCE_URI.stringValue())) {
				ExternalLink link = new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), "provenance");
				link.add(new AttributeAppender("class", " nanopub-provenance "));
				link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
				add(link);
				add(new Label("iri", uri));
			} else if (uri.equals(Nanopub.HAS_PUBINFO_URI.stringValue())) {
				ExternalLink link = new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), "pubinfo");
				link.add(new AttributeAppender("class", " nanopub-pubinfo "));
				link.add(new AttributeAppender("style", "padding: 0 5px; border-radius: 7px; border-width: 1px; border-color: #666; border-style: solid;"));
				add(link);
				add(new Label("iri", uri));
			} else {
				add(new ExternalLink("link", "./explore?id=" + URLEncoder.encode(uri, Charsets.UTF_8.toString()), IriItem.getShortNameFromURI(uri)));
				add(new Label("iri", uri));
			}
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public NanobenchLink(String id, String uri) {
		this(id, uri, null);
	}

}
