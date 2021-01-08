package org.petapico.nanobench;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.Statement;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public NanopubItem(String id, final NanopubElement n, boolean hidePubinfo) {
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

		if (ProfilePage.getUserIri() != null && user != null && ProfilePage.getUserIri().equals(user.getId())) {
			// Own nanopublication
			add(new ExternalLink("retract-link", "./publish?" +
						"template=http://purl.org/np/RAvySE8-JDPqaPnm_XShAa-aVuDZ2iW2z7Oc1Q9cfvxZE" +
						"&param_nanopubToBeRetracted=" + urlEncode(n.getUri()))
					.add(new Label("retract-label", "retract")));
			add(new ExternalLink("approve-link", ".").add(new Label("approve-label", "")));  // Hide approve/disapprove link
		} else {
			// Somebody else's nanopublication
			add(new ExternalLink("retract-link", ".").add(new Label("retract-label", "")));  // Hide retract link
			add(new ExternalLink("approve-link", "./publish?" +
					"template=http://purl.org/np/RAsmppaxXZ613z9olynInTqIo0oiCelsbONDi2c5jlEMg" +
					"&param_nanopub=" + urlEncode(n.getUri()))
				.add(new Label("approve-label", "approve/disapprove")));
		}
		add(new ExternalLink("comment-link", "./publish?" +
				"template=http://purl.org/np/RAqfUmjV05ruLK3Efq2kCODsHfY16LJGO3nAwDi5rmtv0" +
				"&param_thing=" + urlEncode(n.getUri())));

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

//		String html = Nanopub2Html.createHtmlString(n.getNanopub(), false, false);
//		if (hidePubinfo) {
//			// Hide pubinfo graph:
//			html = html.replaceFirst("<div class=\"nanopub-pubinfo\"", "<div class=\"nanopub-pubinfo\" style=\"display: none;\"");
//		}
//		Label l = new Label("nanopub", html);
//		l.setEscapeModelStrings(false);
//		add(l);

		List<Statement> assertionStatements = new ArrayList<>(n.getNanopub().getAssertion());
		add(new DataView<Statement>("assertion-statements", new ListDataProvider<Statement>(assertionStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("assertion-statement", st, n.getNanopub()));
			}

		});
		List<Statement> provenanceStatements = new ArrayList<>(n.getNanopub().getProvenance());
		add(new DataView<Statement>("provenance-statements", new ListDataProvider<Statement>(provenanceStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("provenance-statement", st, n.getNanopub()));
			}

		});
		List<Statement> pubinfoStatements = new ArrayList<>(n.getNanopub().getPubinfo());
		add(new DataView<Statement>("pubinfo-statements", new ListDataProvider<Statement>(pubinfoStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("pubinfo-statement", st, n.getNanopub()));
			}

		});
	}

	private static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		return "";
	}

}
