package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

public class UserPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/user";

	private Model<String> progress;
	private Model<String> selected = new Model<>();
	private IRI userIri;
	private boolean added = false;
	private Map<String,String> pubKeyMap;
	private RadioChoice<String> pubkeySelection;
	
	public UserPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));

		if (parameters.get("id") == null) throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		userIri = Utils.vf.createIRI(parameters.get("id").toString());
		add(new Label("username", User.getDisplayName(userIri)));

		// TODO: Progress bar doesn't update at the moment:
		progress = new Model<>();
		final Label progressLabel = new Label("progress", progress);
//		progressLabel.setOutputMarkupId(true);
//		progressLabel.add(new AjaxSelfUpdatingTimerBehavior(Duration.milliseconds(1000)));
		add(progressLabel);

		NanodashSession session = NanodashSession.get();
		ArrayList<String> pubKeyList = new ArrayList<>();
		pubKeyMap = new HashMap<>();
		if (userIri.equals(session.getUserIri())) {
			String lKeyShort = Utils.getShortPubkeyLabel(session.getPubkeyString(), userIri);
			pubKeyList.add(lKeyShort);
			pubKeyMap.put(lKeyShort, session.getPubkeyString());
		}
		for (String pk : User.getPubkeys(userIri, null)) {
			String keyShort = Utils.getShortPubkeyLabel(pk, userIri);
			if (!pubKeyMap.containsKey(keyShort)) {
				pubKeyList.add(keyShort);
				pubKeyMap.put(keyShort, pk);
			}
		}

		pubkeySelection = new RadioChoice<String>("pubkeygroup", selected, pubKeyList);
		pubkeySelection.setDefaultModelObject(pubKeyList.get(0));
		pubkeySelection.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			private static final long serialVersionUID = -6398658082085108029L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				// TODO: implement this
				System.err.println("PUBKEY SELECTED: " + selected.getObject());
				refresh();
				setResponsePage(target.getPage());
			}

		});
		add(pubkeySelection);

		refresh();
	}

	private void refresh() {
		if (added) {
			remove("nanopubs");
		}
		added = true; 
		add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

			private static final long serialVersionUID = 1L;

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				Map<String,String> nanopubParams = new HashMap<>();
				List<ApiResponseEntry> nanopubResults = new ArrayList<>();
				nanopubParams.put("pubkey", pubKeyMap.get(pubkeySelection.getModelObject()));  // TODO: only using first public key here
				nanopubResults = ApiAccess.getRecent("find_signed_nanopubs", nanopubParams, progress).getData();
				List<NanopubElement> nanopubs = new ArrayList<>();
				while (!nanopubResults.isEmpty() && nanopubs.size() < 20) {
					ApiResponseEntry resultEntry = nanopubResults.remove(0);
					String npUri = resultEntry.get("np");
					// Hide retracted nanopublications:
					if (resultEntry.get("retracted").equals("1") || resultEntry.get("retracted").equals("true")) continue;
					// Hide superseded nanopublications:
					if (resultEntry.get("superseded").equals("1") || resultEntry.get("superseded").equals("true")) continue;
					nanopubs.add(new NanopubElement(npUri, false));
				}
				NanopubResults r = new NanopubResults(markupId, nanopubs);
				progress.setObject("");
				return r;
			}
		});
	}

//	@Override
//	public void onBeforeRender() {
//		super.onBeforeRender();
//		if (hasBeenRendered()) {
//			setResponsePage(getPageClass(), getPageParameters());
//		}
//	}

}
