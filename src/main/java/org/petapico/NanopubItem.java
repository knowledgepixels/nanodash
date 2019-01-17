package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.Nanopub;
import org.nanopub.Nanopub2Html;
import org.nanopub.SimpleTimestampPattern;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.server.GetNanopub;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubItem(String id, String uri) {
		super(id);

		ExternalLink link = new ExternalLink("nanopub-id-link", uri);
		link.add(new Label("nanopub-id-text", uri));
		add(link);
		Nanopub np = GetNanopub.get(uri);
		add(new Label("datetime", SimpleTimestampPattern.getCreationTime(np).getTime().toString()));
		String signatureNote = "not signed";
		if (SignatureUtils.seemsToHaveSignature(np)) {
			try {
				if (SignatureUtils.hasValidSignature(SignatureUtils.getSignatureElement(np))) {
					signatureNote = "valid signature";
				} else {
					signatureNote = "invalid signature";
				}
			} catch (Exception ex) {
				signatureNote = "malformed signature";
			}
		}
		add(new Label("notes", signatureNote));
		String html = Nanopub2Html.createHtmlString(np, false, false);
		Label l = new Label("nanopub", html);
		l.setEscapeModelStrings(false);
		add(l);
	}

}
