package org.petapico.nanobench;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.Charsets;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.petapico.nanobench.action.NanopubAction;

import net.trustyuri.TrustyUriUtils;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm:ss zzz");

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo) {
		super(id);

		ExternalLink link = new ExternalLink("nanopub-id-link", "./explore?id=" + URLEncoder.encode(n.getUri(), Charsets.UTF_8));
		link.add(new Label("nanopub-id-text", TrustyUriUtils.getArtifactCode(n.getUri()).substring(0, 10)));
		add(link);
		if (n.getCreationTime() != null) {
			add(new Label("datetime", simpleDateFormat.format(n.getCreationTime().getTime())));
		} else {
			add(new Label("datetime", "(undated)"));
		}
		String userString = "";
		User user = null;
		try {
			if (n.hasValidSignature()) {
				user = User.getUserForPubkey(n.getPubkey());
				if (user != null) {
					userString = user.getShortDisplayName();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		PageParameters params = new PageParameters();
		if (user != null) {
			params.add("id", user.getId());
		}
		BookmarkablePageLink<UserPage> userLink = new BookmarkablePageLink<UserPage>("user-link", UserPage.class, params);
		userLink.add(new Label("user-text", userString));
		add(userLink);

		List<MarkupContainer> actionLinks = new ArrayList<>();
		IRI userIri = NanobenchSession.get().getUserIri();
		boolean isOwnNanopub = userIri != null && user != null && userIri.equals(user.getId());
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
				if (!n.hasValidSignature()) {
					negativeNotes = "- invalid signature";
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				negativeNotes = "- malformed or legacy signature";
			}
		}
		if (n.isRetracted()) {
			positiveNotes = "";
			negativeNotes = "- retracted";
		}
		add(new Label("positive-notes", positiveNotes));
		add(new Label("negative-notes", negativeNotes));

		WebMarkupContainer assertionPart1 = new WebMarkupContainer("assertion-part1");
		WebMarkupContainer assertionPart2 = new WebMarkupContainer("assertion-part2");
		assertionPart2.setOutputMarkupPlaceholderTag(true);
		assertionPart2.setVisible(false);
		List<Statement> assertionStatements1 = new ArrayList<>();
		List<Statement> assertionStatements2 = new ArrayList<>();
		AjaxLink<Void> showMoreLink = new AjaxLink<Void>("showmore"){

			private static final long serialVersionUID = 7877892803130782900L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				assertionPart2.setVisible(true);
				target.add(assertionPart2);
				setVisible(false);
				target.add(this);
			}

		};
		showMoreLink.setOutputMarkupPlaceholderTag(true);
		showMoreLink.setVisible(false);
		AjaxLink<Void> showLessLink = new AjaxLink<Void>("showless"){

			private static final long serialVersionUID = 7877892803130782900L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				assertionPart2.setVisible(false);
				target.add(assertionPart2);
				showMoreLink.setVisible(true);
				target.add(showMoreLink);
			}

		};
		assertionPart1.add(showMoreLink);
		assertionPart2.add(showLessLink);

		if (n.getNanopub() != null) {
			ArrayList<Statement> a = new ArrayList<>(n.getNanopub().getAssertion());
			if (a.size() > 10) {
				for (int i = 0 ; i < a.size() ; i++) {
					if (i < 5) {
						assertionStatements1.add(a.get(i));
					} else {
						assertionStatements2.add(a.get(i));
					}
				}
				showMoreLink.setVisible(true);
			} else {
				assertionStatements1 = a;
			}
		}

		assertionPart1.add(new DataView<Statement>("assertion-statements1", new ListDataProvider<Statement>(assertionStatements1)) {

			private static final long serialVersionUID = -4523773471034490379L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("assertion-statement1", st, n.getNanopub()));
			}

		});
		add(assertionPart1);

		assertionPart2.add(new DataView<Statement>("assertion-statements2", new ListDataProvider<Statement>(assertionStatements2)) {

			private static final long serialVersionUID = -6119278916371285402L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("assertion-statement2", st, n.getNanopub()));
			}

		});
		add(assertionPart2);

		WebMarkupContainer provenance = new WebMarkupContainer("provenance");
		List<Statement> provenanceStatements = new ArrayList<>();
		if (hideProvenance) {
			provenance.setVisible(false);
		} else {
			if (n.getNanopub() != null) {
				provenanceStatements = new ArrayList<>(n.getNanopub().getProvenance());
			}
		}
		provenance.add(new DataView<Statement>("provenance-statements", new ListDataProvider<Statement>(provenanceStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("provenance-statement", st, n.getNanopub()));
			}

		});
		add(provenance);

		WebMarkupContainer pubInfo = new WebMarkupContainer("pubinfo");
		List<Statement> pubinfoStatements = new ArrayList<>();
		if (hidePubinfo) {
			pubInfo.setVisible(false);
		} else {
			if (n.getNanopub() != null) {
				pubinfoStatements = new ArrayList<>(n.getNanopub().getPubinfo());
			}
		}
		pubInfo.add(new DataView<Statement>("pubinfo-statements", new ListDataProvider<Statement>(pubinfoStatements)) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("pubinfo-statement", st, n.getNanopub()));
			}

		});
		add(pubInfo);
	}

}
