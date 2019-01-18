package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.Nanopub2Html;
import org.openrdf.model.URI;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubItem(String id, NanopubElement n) {
		super(id);

		ExternalLink link = new ExternalLink("nanopub-id-link", n.getUri());
		link.add(new Label("nanopub-id-text", n.getUri()));
		add(link);
		add(new Label("datetime", n.getCreationTime().getTime().toString()));
		String types = "";
		for (URI type : n.getTypes()) {
			types += " " + Utils.getShortNameFromURI(type).replaceFirst("Nanopub$", "");
		}
		add(new Label("types", types));
		String signatureNote = "not signed";
		if (n.seemsToHaveSignature()) {
			try {
				if (n.hasValidSignature()) {
					signatureNote = "valid signature";
				} else {
					signatureNote = "invalid signature";
				}
			} catch (Exception ex) {
				signatureNote = "malformed signature";
			}
		}
		add(new Label("notes", signatureNote));
		String html = Nanopub2Html.createHtmlString(n.getNanopub(), false, false);
		Label l = new Label("nanopub", html);
		l.setEscapeModelStrings(false);
		add(l);
	}

}
