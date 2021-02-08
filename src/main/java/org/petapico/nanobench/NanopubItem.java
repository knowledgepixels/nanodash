package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.petapico.nanobench.action.NanopubAction;

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

		List<MarkupContainer> actionLinks = new ArrayList<>();
		boolean isOwnNanopub = ProfilePage.getUserIri() != null && user != null && ProfilePage.getUserIri().equals(user.getId());
		List<NanopubAction> actions = new ArrayList<>();
		actions.addAll(NanopubAction.getDefaultActions());
		actions.addAll(NanopubAction.getActionsFromPreferences(NanobenchPreferences.get()));
		for (NanopubAction action : actions) {
			if (isOwnNanopub && !action.isApplicableToOwnNanopubs()) continue;
			if (!isOwnNanopub && !action.isApplicableToOthersNanopubs()) continue;
			Nanopub np = n.getNanopub();
			if (np == null || !action.isApplicableTo(np)) continue;
			String linkUrl = "./publish?template=" + Utils.urlEncode(action.getTemplateUri(np)) + "&" + action.getParamString(np);
			actionLinks.add(
				new ExternalLink("action-link", linkUrl).add(new Label("action-link-label", action.getLinkLabel(np) + "..."))
			);
		}

		add(new DataView<MarkupContainer>("action-links", new ListDataProvider<MarkupContainer>(actionLinks)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<MarkupContainer> item) {
				item.add(item.getModelObject());
			}

		});

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

		List<Statement> assertionStatements = new ArrayList<>();
		if (n.getNanopub() != null) {
			assertionStatements = new ArrayList<>(n.getNanopub().getAssertion());
		}
		add(new DataView<Statement>("assertion-statements", new ListDataProvider<Statement>(assertionStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("assertion-statement", st, n.getNanopub()));
			}

		});
		List<Statement> provenanceStatements = new ArrayList<>();
		if (n.getNanopub() != null) {
			provenanceStatements = new ArrayList<>(n.getNanopub().getProvenance());
		}
		add(new DataView<Statement>("provenance-statements", new ListDataProvider<Statement>(provenanceStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("provenance-statement", st, n.getNanopub()));
			}

		});
		List<Statement> pubinfoStatements = new ArrayList<>();
		if (n.getNanopub() != null) {
			pubinfoStatements = new ArrayList<>(n.getNanopub().getPubinfo());
		}
		add(new DataView<Statement>("pubinfo-statements", new ListDataProvider<Statement>(pubinfoStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("pubinfo-statement", st, n.getNanopub()));
			}

		});
	}

}
