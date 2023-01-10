package org.petapico.nanobench;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.petapico.nanobench.action.NanopubAction;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

public class NanopubItem extends Panel {
	
	private static final long serialVersionUID = -5109507637942030910L;

	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy, HH:mm:ss zzz");

	public NanopubItem(String id, final NanopubElement n, boolean hideProvenance, boolean hidePubinfo) {
		super(id);

		add(
			new BookmarkablePageLink<>("nanopub-id-link", ExplorePage.class, new PageParameters().add("id", n.getUri()))
				.add(new Label("nanopub-id-text", Utils.getShortNanopubId(n.getUri())))
		);

		String userString = "";
		String pubkey = null;
		try {
			if (n.hasValidSignature()) {
				pubkey = n.getPubkey();
				userString = User.getShortDisplayNameForPubkey(pubkey);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		NanobenchSession session = NanobenchSession.get();
		boolean isOwnNanopub = session.getUserIri() != null && session.getPubkeyString() != null && session.getPubkeyString().equals(pubkey);
		final List<NanopubAction> actionList = new ArrayList<>();
		final Map<String,NanopubAction> actionMap = new HashMap<>();
		List<NanopubAction> allActions = new ArrayList<>();
		allActions.addAll(NanopubAction.getDefaultActions());
		allActions.addAll(NanopubAction.getActionsFromPreferences(NanobenchPreferences.get()));
		for (NanopubAction action : allActions) {
			if (isOwnNanopub && !action.isApplicableToOwnNanopubs()) continue;
			if (!isOwnNanopub && !action.isApplicableToOthersNanopubs()) continue;
			Nanopub np = n.getNanopub();
			if (np == null || !action.isApplicableTo(np)) continue;
			actionList.add(action);
			actionMap.put(action.getLinkLabel(n.getNanopub()), action);
		}

		ChoiceProvider<NanopubAction> choiceProvider = new ChoiceProvider<NanopubAction>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(NanopubAction action) {
				return action.getLinkLabel(n.getNanopub()) + "...";
			}

			@Override
			public String getIdValue(NanopubAction object) {
				return object.getLinkLabel(n.getNanopub());
			}

			// Getting strange errors with Tomcat if this method is not overridden:
			@Override
			public void detach() {
			}

			@Override
			public void query(String term, int page, Response<NanopubAction> response) {
				for (NanopubAction action : actionList) {
					response.add(action);
				}
			}

			@Override
			public Collection<NanopubAction> toChoices(Collection<String> ids) {
				List<NanopubAction> list = new ArrayList<>();
				for (String s : ids) {
					list.add(actionMap.get(s));
				}
				return list;
			}

		};
		Model<NanopubAction> menuModel = new Model<>();
		Select2Choice<NanopubAction> menu = new Select2Choice<NanopubAction>("action-menu", menuModel, choiceProvider);
		menu.getSettings().setPlaceholder("");
		menu.getSettings().setWidth("20px");
		menu.getSettings().setDropdownCssClass("actionmenuresults");
		menu.getSettings().setCloseOnSelect(true);
		menu.getSettings().setMinimumResultsForSearch(-1);
		menu.getSettings().setDropdownAutoWidth(true);
		menu.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				NanopubAction action = menu.getModel().getObject();
				String url = "." + PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(action.getTemplateUri(n.getNanopub())) +
						"&" + action.getParamString(n.getNanopub()) +
						"&template-version=latest";
				throw new RedirectToUrlException(url);
			}

		});
		add(menu);

		if (n.getCreationTime() != null) {
			add(new Label("datetime", simpleDateFormat.format(n.getCreationTime().getTime())));
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
