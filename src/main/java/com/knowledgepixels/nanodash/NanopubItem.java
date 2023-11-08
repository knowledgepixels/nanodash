package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
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

	private final Map<IRI,Integer> predicateOrder = new HashMap<>();
	private Template template;

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo) {
		this(id, n, hideProvenance, hidePubinfo, null);
	}

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo, List<NanopubAction> actions) {
		super(id);

		add(new NanodashLink("nanopub-id-link", n.getUri()));
		add(new Label("nanopub-label", "\"" + n.getLabel() + "\"").setVisible(!n.getLabel().isEmpty()));

		String userString = "";
		IRI signerId = null;
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
			Nanopub np = n.getNanopub();
			if (np == null || !action.isApplicableTo(np)) continue;
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

		// Getting predicate order in assertion template in order to sort statements:
		// TODO This is just a quick-and-dirty solution. Properly trying to fill the template should be done at some point.
		// TODO We should also do this for the provenance and pubinfo graphs too.
		template = Template.getTemplate(n.getNanopub());
		if (template != null) {
			for (IRI statementId : template.getStatementIris()) {
				if (template.isGroupedStatement(statementId)) {
					for (IRI subStatementId : template.getStatementIris(statementId)) {
						processStatementForPredicateOrder(subStatementId);
					}
				} else {
					processStatementForPredicateOrder(statementId);
				}
			}
		}

		
		Template fillTemplate = template;
		if (fillTemplate == null) {
			fillTemplate = Template.getTemplate("http://purl.org/np/RAFu2BNmgHrjOTJ8SKRnKaRp-VP8AOOb7xX88ob0DZRsU");  // arbitrary triple template
		}
		PublishFormContext assertionContext = new PublishFormContext(ContextType.ASSERTION, fillTemplate.getId(), "statement", fillTemplate.getTargetNamespace(), true);
		assertionContext.initStatements();
		ValueFiller filler = new ValueFiller(n.getNanopub(), ContextType.ASSERTION);
		if (signerId != null) {
			assertionContext.getComponentModels().put(Template.CREATOR_PLACEHOLDER, Model.of(signerId.stringValue()));
		}
		filler.fill(assertionContext);
		assertionContext.finalizeStatements();
		List<StatementItem> assertionStatements = new ArrayList<>();
		for (StatementItem si : assertionContext.getStatementItems()) {
			if (!(si.isOptional() && si.hasEmptyElements())) {
				assertionStatements.add(si);
			}
		}
		add(new ListView<StatementItem>("statements", assertionStatements) {

			private static final long serialVersionUID = 1L;

			protected void populateItem(ListItem<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});

		
		StatementComparator statementComparator = null;

		if (n.getNanopub() != null) {
			statementComparator = new StatementComparator(n.getNanopub());
	
			List<StatementItem> a = new ArrayList<>(assertionStatements);
			//a.sort(statementComparator);
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
		}

		assertionPart1.add(new DataView<StatementItem>("assertion-statements1", new ListDataProvider<StatementItem>(assertionStatements1)) {

			private static final long serialVersionUID = -4523773471034490379L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
			}

		});
		add(assertionPart1);

		assertionPart2.add(new DataView<StatementItem>("assertion-statements2", new ListDataProvider<StatementItem>(assertionStatements2)) {

			private static final long serialVersionUID = -6119278916371285402L;

			@Override
			protected void populateItem(Item<StatementItem> item) {
				item.add(item.getModelObject());
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
				provenanceStatements.sort(statementComparator);
			}
		}
		provenance.add(new DataView<Statement>("provenance-statements", new ListDataProvider<Statement>(provenanceStatements)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<Statement> item) {
				Statement st = item.getModelObject();
				item.add(new TripleItem("provenance-statement", st, n.getNanopub(), Template.PROVENANCE_TEMPLATE_CLASS));
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
				pubinfoStatements.sort(statementComparator);
			}
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

	private void processStatementForPredicateOrder(IRI statementId) {
		IRI pred = template.getPredicate(statementId);
		if (template.isRestrictedChoicePlaceholder(pred)) {
			// TODO
		} else if (!template.isPlaceholder(pred)) {
			if (!predicateOrder.containsKey(pred)) predicateOrder.put(pred, predicateOrder.size());
		}
	}
	

	private class StatementComparator implements Comparator<Statement>, Serializable {

		private static final long serialVersionUID = 1L;

		private IRI introducedThing = null;
		private Nanopub np;

		public StatementComparator(Nanopub np) {
			this.np = np;
			for (Statement st : np.getPubinfo()) {
				if (st.getSubject().equals(np.getUri()) && st.getPredicate().equals(PublishForm.INTRODUCES_PREDICATE) && st.getObject().isIRI()) {
					introducedThing = (IRI) st.getObject();
					break;
				}
			}
		}

		@Override
		public int compare(Statement arg0, Statement arg1) {
			if (!arg0.getSubject().stringValue().equals(arg1.getSubject().stringValue())) {
				if (arg0.getSubject().equals(np.getUri())) {
					return -1;
				} else if (arg1.getSubject().equals(np.getUri())) {
					return 1;
				}
				if (arg0.getSubject().equals(np.getAssertionUri())) {
					return -1;
				} else if (arg1.getSubject().equals(np.getAssertionUri())) {
					return 1;
				}
				if (arg0.getSubject().equals(introducedThing)) {
					return -1;
				} else if (arg1.getSubject().equals(introducedThing)) {
					return 1;
				}
				return arg0.getSubject().toString().compareTo(arg1.getSubject().toString());
			}
			IRI pred0 = arg0.getPredicate();
			IRI pred1 = arg1.getPredicate();
			if (predicateOrder.containsKey(pred0) && predicateOrder.containsKey(pred1)) {
				int order0 = predicateOrder.get(pred0);
				int order1 = predicateOrder.get(pred1);
				if (order0 == order1) {
					return pred0.toString().compareTo(pred1.toString());
				} else {
					return order0 - order1;
				}
			} else if (predicateOrder.containsKey(pred0)) {
				return -1;
			} else if (predicateOrder.containsKey(pred1)) {
				return 1;
			} else {
				return pred0.toString().compareTo(pred1.toString());
			}
		}

	}

}
