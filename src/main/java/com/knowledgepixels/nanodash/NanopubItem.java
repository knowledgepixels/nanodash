package com.knowledgepixels.nanodash;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.action.NanopubAction;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public static SimpleDateFormat simpleDateTimeFormat = new SimpleDateFormat("d MMM yyyy, HH:mm:ss zzz");
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy");

	private IRI signerId;

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo) {
		this(id, n, hideProvenance, hidePubinfo, null);
	}

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo, List<NanopubAction> actions) {
		super(id);

		add(new NanodashLink("nanopub-id-link", n.getUri()));
		add(new Label("nanopub-label", "\"" + n.getLabel() + "\"").setVisible(!n.getLabel().isEmpty()));

		String userString = "";
		String pubkey = null;
		try {
			if (n.hasValidSignature()) {
				pubkey = n.getPubkey();
				signerId = n.getSignerId();
				userString = User.getShortDisplayNameForPubkey(pubkey);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		NanodashSession session = NanodashSession.get();
		boolean isOwnNanopub = session.getUserIri() != null && session.getPubkeyString() != null && session.getPubkeyString().equals(pubkey);
		final List<NanopubAction> actionList = new ArrayList<>();
		final Map<String,NanopubAction> actionMap = new HashMap<>();
		List<NanopubAction> allActions = new ArrayList<>();
		if (actions == null) {
			allActions.addAll(NanopubAction.defaultActions);
			allActions.addAll(NanopubAction.getActionsFromPreferences(NanodashPreferences.get()));
		} else {
			allActions.addAll(actions);
		}
		for (NanopubAction action : allActions) {
			if (isOwnNanopub && !action.isApplicableToOwnNanopubs()) continue;
			if (!isOwnNanopub && !action.isApplicableToOthersNanopubs()) continue;
			if (!action.isApplicableTo(n.getNanopub())) continue;
			actionList.add(action);
			actionMap.put(action.getLinkLabel(n.getNanopub()), action);
		}
		add(new ActionMenu("action-menu", actionList, n.getNanopub()));

		if (n.getCreationTime() != null) {
			add(new Label("datetime", simpleDateTimeFormat.format(n.getCreationTime().getTime())));
		} else {
			add(new Label("datetime", "(undated)"));
		}
		PageParameters params = new PageParameters();
		IRI uIri = User.findSingleIdForPubkey(pubkey);
		if (uIri != null) params.add("id", uIri);
		BookmarkablePageLink<UserPage> userLink = new BookmarkablePageLink<UserPage>("user-link", UserPage.class, params);
		userLink.add(new Label("user-text", userString));
		add(userLink);

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
		List<StatementItem> assertionStatements1 = new ArrayList<>();
		List<StatementItem> assertionStatements2 = new ArrayList<>();
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

		List<StatementItem> assertionStatements = new ArrayList<>();
		populateStatementItemList(ContextType.ASSERTION, n.getNanopub(), assertionStatements);

		List<StatementItem> a = new ArrayList<>(assertionStatements);
		if (a.size() > 3) {
			for (int i = 0 ; i < a.size() ; i++) {
				if (i < 2) {
					assertionStatements1.add(a.get(i));
				} else {
					assertionStatements2.add(a.get(i));
				}
			}
			showMoreLink.setVisible(true);
		} else {
			assertionStatements1 = a;
		}

		assertionPart1.add(new DataView<StatementItem>("assertion-statements1", new ListDataProvider<StatementItem>(assertionStatements1)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});
		add(assertionPart1);

		assertionPart2.add(new DataView<StatementItem>("assertion-statements2", new ListDataProvider<StatementItem>(assertionStatements2)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});
		add(assertionPart2);

		List<StatementItem> provenanceStatements = new ArrayList<>();
		WebMarkupContainer provenance = new WebMarkupContainer("provenance");
		if (hideProvenance) {
			provenance.setVisible(false);
		} else {
			populateStatementItemList(ContextType.PROVENANCE, n.getNanopub(), provenanceStatements);
		}
		provenance.add(new DataView<StatementItem>("provenance-statements", new ListDataProvider<StatementItem>(provenanceStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});
		add(provenance);

		WebMarkupContainer pubInfo = new WebMarkupContainer("pubinfo");
		List<Statement> pubinfoStatements = new ArrayList<>();
		if (hidePubinfo) {
			pubInfo.setVisible(false);
		} else {
			pubinfoStatements = new ArrayList<>(n.getNanopub().getPubinfo());
		}
		pubInfo.add(new DataView<Statement>("pubinfo-statements", new ListDataProvider<Statement>(pubinfoStatements)) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("pubinfo-statement", st, n.getNanopub(), Template.PUBINFO_TEMPLATE_CLASS));
			}

		});
		add(pubInfo);
	}

	private void populateStatementItemList(ContextType contextType, Nanopub np, List<StatementItem> list) {
		if (contextType == ContextType.ASSERTION) {
			Template t = Template.getTemplate(np);
			if (t == null) t = Template.getTemplate("http://purl.org/np/RAFu2BNmgHrjOTJ8SKRnKaRp-VP8AOOb7xX88ob0DZRsU");
			populateStatementItemList(contextType, np, t, "statement", list);
		} else if (contextType == ContextType.PROVENANCE) {
			Template t = Template.getProvenanceTemplate(np);
			if (t == null) t = Template.getTemplate("http://purl.org/np/RA3Jxq5JJjluUNEpiMtxbiIHa7Yt-w8f9FiyexEstD5R4");
			populateStatementItemList(contextType, np, t, "provenance-statement", list);
		} else if (contextType == ContextType.PUBINFO) {
//			fillTemplates = List.copyOf(Template.getPubinfoTemplates(np));
//			fallbackTemplateId = "http://purl.org/np/RAv4Knz3yIWofRt_Hpghs67iDKTAixqNthOM75OB4Ltvo";
//			elementId = "pubinfo-statement";
		} else {
			throw new IllegalArgumentException("Invalid context type: " + contextType);
		}
	}

	private void populateStatementItemList(ContextType contextType, Nanopub np, Template fillTemplate, String elementId, List<StatementItem> list) {
		PublishFormContext context = new PublishFormContext(contextType, fillTemplate.getId(), elementId, fillTemplate.getTargetNamespace(), true);
		context.initStatements();
		if (signerId != null) {
			context.getComponentModels().put(Template.CREATOR_PLACEHOLDER, Model.of(signerId.stringValue()));
		}
		ValueFiller prFiller = new ValueFiller(np, contextType);
		prFiller.fill(context);
		for (StatementItem si : context.getStatementItems()) {
			if (!(si.isOptional() && si.hasEmptyElements())) {
				list.add(si);
			}
		}
	}

}
