package org.petapico.nanobench;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.Nanopub2Html;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubItem(String id, NanopubElement n) {
		super(id);

		ExternalLink link = new ExternalLink("nanopub-id-link", n.getUri());
		link.add(new Label("nanopub-id-text", n.getUri()));
		add(link);
		if (n.getCreationTime() != null) {
			add(new Label("datetime", n.getCreationTime().getTime().toString()));
		} else {
			add(new Label("datetime", "(undated)"));
		}
		String userString = "";
		User user = null;
		try {
			if (n.hasValidSignature()) {
				user = User.getUserForPubkey(n.getPubkey());
				if (user != null) {
					userString = user.getDisplayName();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		add(new Label("user", userString));

		ExternalLink retractLink = new ExternalLink("retract-link", "./publish?" +
				"template=http://purl.org/np/RAvySE8-JDPqaPnm_XShAa-aVuDZ2iW2z7Oc1Q9cfvxZE&" +
				"param_nanopubToBeRetracted=" + URLEncoder.encode(n.getUri(), StandardCharsets.UTF_8));
		if (ProfilePage.getUserIri() != null && user != null && ProfilePage.getUserIri().equals(user.getId())) {
			retractLink.add(new Label("retract-label", "retract"));
		} else {
			retractLink.add(new Label("retract-label", ""));
		}
		add(retractLink);

		ExternalLink commentLink = new ExternalLink("comment-link", "./publish?" +
				"template=http://purl.org/np/RAqfUmjV05ruLK3Efq2kCODsHfY16LJGO3nAwDi5rmtv0&" +
				"param_thing=" + URLEncoder.encode(n.getUri(), StandardCharsets.UTF_8));
//		commentLink.add(new Label("comment-label", "comment"));
		add(commentLink);

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
				ex.printStackTrace();
				negativeNotes = "malformed or legacy signature";
			}
		}
		if (n.isRetracted()) {
			positiveNotes = "";
			negativeNotes = "retracted";
		}
		add(new Label("positive-notes", positiveNotes));
		add(new Label("negative-notes", negativeNotes));
		String html = Nanopub2Html.createHtmlString(n.getNanopub(), false, false);
		// Hide pubinfo graph:
		html = html.replaceFirst("<div class=\"nanopub-pubinfo\"", "<div class=\"nanopub-pubinfo\" style=\"display: none;\"");
		Label l = new Label("nanopub", html);
		l.setEscapeModelStrings(false);
		add(l);
	}

}
