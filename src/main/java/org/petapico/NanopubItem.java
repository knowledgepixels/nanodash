package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub2Html;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubItem(String id, NanopubElement n, boolean markAsRetracted) {
		super(id);

		ExternalLink link = new ExternalLink("nanopub-id-link", n.getUri());
		link.add(new Label("nanopub-id-text", n.getUri()));
		add(link);
		if (n.getCreationTime() != null) {
			add(new Label("datetime", n.getCreationTime().getTime().toString()));
		} else {
			add(new Label("datetime", "(undated)"));
		}
		String types = "";
		for (IRI type : n.getTypes()) {
			types += " " + Utils.getShortNameFromURI(type).replaceFirst("Nanopub$", "");
		}
		add(new Label("types", types));
		String positiveNotes = "";
		String negativeNotes = "";
		if (n.seemsToHaveSignature()) {
			try {
				if (n.hasValidSignature()) {
					positiveNotes = "valid signature";
				} else {
					negativeNotes = "invalid signature";
				}
			} catch (Exception ex) {
				negativeNotes = "malformed or legacy signature";
			}
		}
		if (markAsRetracted) {
			positiveNotes = "";
			negativeNotes = "retracted";
		}
		add(new Label("positive-notes", positiveNotes));
		add(new Label("negative-notes", negativeNotes));
		String html = Nanopub2Html.createHtmlString(n.getNanopub(), false, false);
		Label l = new Label("nanopub", html);
		l.setEscapeModelStrings(false);
		add(l);
	}

}
