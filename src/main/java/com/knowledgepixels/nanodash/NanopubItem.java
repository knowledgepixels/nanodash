package com.knowledgepixels.nanodash;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.nanopub.SimpleCreatorPattern;

import com.knowledgepixels.nanodash.action.NanopubAction;

public class NanopubItem extends Panel {

	private static final long serialVersionUID = -5109507637942030910L;

	public static SimpleDateFormat simpleDateTimeFormat = new SimpleDateFormat("d MMM yyyy, HH:mm:ss zzz");
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy");

	private boolean isInitialized = false;
	private NanopubElement n;
	private boolean hideProvenance = false;
	private boolean hidePubinfo = false;
	private boolean hideHeader = false;
	private boolean hideFooter = false;
	private boolean expanded = false;
	private List<NanopubAction> actions;
	private IRI signerId;
	private WebMarkupContainer assertionPart1, assertionPart2;
	private AjaxLink<Void> showMoreLink, showLessLink;
	private String tempalteId;

	public NanopubItem(String id, NanopubElement n, String tempalteId) {
		super(id);
		this.n = n;
		this.tempalteId = tempalteId;
	}

	public NanopubItem(String id, NanopubElement n) {
		this(id, n, null);
	}

	private void initialize() {
		if (isInitialized) return;

		String pubkey = null;
		try {
			if (n.hasValidSignature()) {
				pubkey = n.getPubkey();
				signerId = n.getSignerId();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (hideHeader) {
			add(new Label("header", "").setVisible(false));
		} else {
			WebMarkupContainer header = new WebMarkupContainer("header");
			header.add(new NanodashLink("nanopub-id-link", n.getUri()));
			header.add(new Label("nanopub-label", "\"" + n.getLabel() + "\"").setVisible(!n.getLabel().isEmpty()));
			if (actions == null || !actions.isEmpty()) {
				NanodashSession session = NanodashSession.get();
				boolean isOwnNanopub = session.getUserIri() != null && session.getPubkeyString() != null && session.getPubkeyString().equals(pubkey);
				final List<NanopubAction> actionList = new ArrayList<>();
				final Map<String,NanopubAction> actionMap = new HashMap<>();
				List<NanopubAction> allActions = new ArrayList<>();
				if (actions == null) {
					allActions.addAll(Arrays.asList(NanopubAction.defaultActions));
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
				header.add(new ActionMenu("action-menu", actionList, n.getNanopub()));
			} else {
				header.add(new Label("action-menu", "").setVisible(false));
			}
			add(header);
		}

		if (hideFooter) {
			add(new Label("footer", "").setVisible(false));
		} else {
			WebMarkupContainer footer = new WebMarkupContainer("footer");
			if (n.getCreationTime() != null) {
				footer.add(new Label("datetime", simpleDateTimeFormat.format(n.getCreationTime().getTime())));
			} else {
				footer.add(new Label("datetime", "(undated)"));
			}
			PageParameters params = new PageParameters();
			IRI uIri = User.findSingleIdForPubkey(pubkey);
			if (uIri == null) {
				Set<IRI> creators = SimpleCreatorPattern.getCreators(n.getNanopub());
				if (creators.size() == 1) uIri = creators.iterator().next();
			}
			if (uIri != null) params.add("id", uIri);
			BookmarkablePageLink<UserPage> userLink = new BookmarkablePageLink<UserPage>("user-link", UserPage.class, params);
			String userString;
			if (signerId != null) {
				userString = User.getShortDisplayName(signerId, pubkey);
			} else {
				userString = User.getShortDisplayName(uIri, pubkey);
			}
			userLink.add(new Label("user-text", userString));
			footer.add(userLink);
	
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
			footer.add(new Label("positive-notes", positiveNotes));
			footer.add(new Label("negative-notes", negativeNotes));
			add(footer);
		}

		assertionPart1 = new WebMarkupContainer("assertion-part1");
		assertionPart2 = new WebMarkupContainer("assertion-part2");
		assertionPart2.setOutputMarkupPlaceholderTag(true);
		assertionPart2.setVisible(false);
		List<StatementItem> assertionStatements1 = new ArrayList<>();
		List<StatementItem> assertionStatements2 = new ArrayList<>();
		showMoreLink = new AjaxLink<Void>("showmore"){

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
		showLessLink = new AjaxLink<Void>("showless"){

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
		showLessLink.setVisible(!expanded);
		assertionPart2.add(showLessLink);

		Template assertionTemplate = Template.getTemplate(n.getNanopub());
		if (tempalteId != null) assertionTemplate = Template.getTemplate(tempalteId);
		if (assertionTemplate == null) assertionTemplate = Template.getTemplate("http://purl.org/np/RAFu2BNmgHrjOTJ8SKRnKaRp-VP8AOOb7xX88ob0DZRsU");
		List<StatementItem> assertionStatements = new ArrayList<>();
		ValueFiller assertionFiller = new ValueFiller(n.getNanopub(), ContextType.ASSERTION, false);
		TemplateContext context = new TemplateContext(ContextType.ASSERTION, assertionTemplate.getId(), "assertion-statement", n.getNanopub());
		populateStatementItemList(context, assertionFiller, assertionStatements);

		assertionPart2.add(new DataView<Statement>("unused-assertion-statements", new ListDataProvider<Statement>(assertionFiller.getUnusedStatements())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				item.add(new TripleItem("unused-assertion-statement", item.getModelObject(), n.getNanopub(), null));
			}

		});
		if (!assertionFiller.getUnusedStatements().isEmpty()) showMoreLink.setVisible(!expanded);

		List<StatementItem> a = new ArrayList<>(assertionStatements);
		if (a.size() > 3) {
			for (int i = 0 ; i < a.size() ; i++) {
				if (i < 2) {
					assertionStatements1.add(a.get(i));
				} else {
					assertionStatements2.add(a.get(i));
				}
			}
			showMoreLink.setVisible(!expanded);
		} else {
			assertionStatements1 = a;
		}
		assertionPart1.add(createStatementView("assertion-statements1", assertionStatements1));
		add(assertionPart1);
		assertionPart2.add(createStatementView("assertion-statements2", assertionStatements2));
		assertionPart2.setVisible(expanded);
		add(assertionPart2);

		WebMarkupContainer provenance = new WebMarkupContainer("provenance");
		if (hideProvenance) {
			provenance.setVisible(false);
		} else {
			Template provenanceTemplate = Template.getProvenanceTemplate(n.getNanopub());
			if (provenanceTemplate == null) provenanceTemplate = Template.getTemplate("http://purl.org/np/RA3Jxq5JJjluUNEpiMtxbiIHa7Yt-w8f9FiyexEstD5R4");
			List<StatementItem> provenanceStatements = new ArrayList<>();
			ValueFiller provenanceFiller = new ValueFiller(n.getNanopub(), ContextType.PROVENANCE, false);
			TemplateContext prContext = new TemplateContext(ContextType.PROVENANCE, provenanceTemplate.getId(), "provenance-statement", n.getNanopub());
			populateStatementItemList(prContext, provenanceFiller, provenanceStatements);
			provenance.add(createStatementView("provenance-statements", provenanceStatements));
			provenance.add(new DataView<Statement>("unused-provenance-statements", new ListDataProvider<Statement>(provenanceFiller.getUnusedStatements())) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void populateItem(Item<Statement> item) {
					item.add(new TripleItem("unused-provenance-statement", item.getModelObject(), n.getNanopub(), null));
				}

			});
		}
		add(provenance);

		WebMarkupContainer pubInfo = new WebMarkupContainer("pubinfo");
		if (hidePubinfo) {
			pubInfo.setVisible(false);
		} else {
			ValueFiller pubinfoFiller = new ValueFiller(n.getNanopub(), ContextType.PUBINFO, false);
			List<String> pubinfoTemplateIds = new ArrayList<>();
			for (IRI iri : Template.getPubinfoTemplateIds(n.getNanopub())) pubinfoTemplateIds.add(iri.stringValue());
			pubinfoTemplateIds.add("https://w3id.org/np/RARJj78P72NR5edKOnu_f4ePE9NYYuW2m2pM-fEoobMBk"); // nanopub label
			pubinfoTemplateIds.add("https://w3id.org/np/RA8iXbwvOC7BwVHuvAhFV235j2582SyAYJ2sfov19ZOlg"); // nanopub type
			pubinfoTemplateIds.add("https://w3id.org/np/RALmzqHlrRfeTD8ESZdKFyDNYY6eFuyQ8GAe_4N5eVytc"); // timestamp
			pubinfoTemplateIds.add("https://w3id.org/np/RADVnztsdSc36ffAXTxiIdXpYEMLiJrRENqaJ2Qn2LX3Y"); // templates
			pubinfoTemplateIds.add("https://w3id.org/np/RAvqXPNKPf56b2226oqhKzARyvIhnpnTrRpLGC1cYweMw"); // introductions
			pubinfoTemplateIds.add("https://w3id.org/np/RAgIlomuR39mN-Z39bbv59-h2DgQBnLyNdL22YmOJ_VHM"); // labels from APIs
			pubinfoTemplateIds.add("https://w3id.org/np/RAFqeX7LWdwsVtJ8SMvvYPX1iGTwFkKcSnfbZGDnjeG10"); // signature
			pubinfoTemplateIds.add("https://w3id.org/np/RAE-zsHxw2VoE6emhSY_Fkr5p_li5Qb8FrREqUwdWdzyM"); // generic
			List<TemplateContext> contexts = new ArrayList<>();
			List<TemplateContext> genericContexts = new ArrayList<>();
			for (String s : pubinfoTemplateIds) {
				TemplateContext piContext = new TemplateContext(ContextType.PUBINFO, s, "pubinfo-statement", n.getNanopub());
				if (piContext.willMatchAnyTriple()) {
					genericContexts.add(piContext);
				} else if (piContext.getTemplateId().equals("https://w3id.org/np/RAE-zsHxw2VoE6emhSY_Fkr5p_li5Qb8FrREqUwdWdzyM")) {
					// TODO: This is a work-around; check why this template doesn't give true to willMatchAnyTriple()
					genericContexts.add(piContext);
				} else {
					contexts.add(piContext);
				}
			}
			contexts.addAll(genericContexts);  // make sure the generic one are at the end
			List<WebMarkupContainer> elements = new ArrayList<>();
			for (TemplateContext piContext : contexts) {
				WebMarkupContainer pubInfoElement = new WebMarkupContainer("pubinfo-element");
				List<StatementItem> pubinfoStatements = new ArrayList<>();
				populateStatementItemList(piContext, pubinfoFiller,  pubinfoStatements);
				if (!pubinfoStatements.isEmpty()) {
					pubInfoElement.add(createStatementView("pubinfo-statements", pubinfoStatements));
					elements.add(pubInfoElement);
				}
			}
			pubInfo.add(new DataView<WebMarkupContainer>("pubinfo-elements", new ListDataProvider<WebMarkupContainer>(elements)) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				protected void populateItem(Item<WebMarkupContainer> item) {
					item.add(item.getModelObject());
				}
	
			});
		}
		add(pubInfo);

		isInitialized = true;
	}

	public NanopubItem expand() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		expanded = true;
		return this;
	}

	public NanopubItem setExpanded(boolean expanded) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		this.expanded = expanded;
		return this;
	}

	public NanopubItem hideProvenance() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		hideProvenance = true;
		return this;
	}

	public NanopubItem setProvenanceHidden(boolean hideProvenance) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		this.hideProvenance = hideProvenance;
		return this;
	}

	public NanopubItem hidePubinfo() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		hidePubinfo = true;
		return this;
	}

	public NanopubItem setPubinfoHidden(boolean hidePubinfo) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		this.hidePubinfo = hidePubinfo;
		return this;
	}

	public NanopubItem hideHeader() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		hideHeader = true;
		return this;
	}

	public NanopubItem setHeaderHidden(boolean hideHeader) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		this.hideHeader = hideHeader;
		return this;
	}

	public NanopubItem hideFooter() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		hideFooter = true;
		return this;
	}

	public NanopubItem setFooterHidden(boolean hideFooter) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		this.hideFooter = hideFooter;
		return this;
	}

	public NanopubItem addActions(NanopubAction... a) {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		if (actions == null) actions = new ArrayList<>();
		for (NanopubAction na : a) {
			actions.add(na);
		}
		return this;
	}

	public NanopubItem noActions() {
		if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
		actions = new ArrayList<>();
		return this;
	}
	
	@Override
	protected void onBeforeRender() {
		initialize();
		super.onBeforeRender();
	}

	private void populateStatementItemList(TemplateContext context, ValueFiller filler, List<StatementItem> list) {
		context.initStatements();
		if (signerId != null) {
			context.getComponentModels().put(Template.CREATOR_PLACEHOLDER, Model.of(signerId.stringValue()));
		}
		filler.fill(context);
		for (StatementItem si : context.getStatementItems()) {
			if (si.isMatched()) {
				list.add(si);
			}
		}
	}

	private DataView<StatementItem> createStatementView(String elementId, List<StatementItem> list) {
		return new DataView<StatementItem>(elementId, new ListDataProvider<StatementItem>(list)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
			}

		};
	}

}
