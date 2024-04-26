package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponseEntry;

public class UserPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/user";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private Model<String> selected = new Model<>();
	private IRI userIri;
	private boolean added = false;
	private Map<String,String> pubKeyMap;
	private RadioChoice<String> pubkeySelection;
	
	public UserPage(final PageParameters parameters) {
		super(parameters);

		if (parameters.get("id") == null) throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		userIri = Utils.vf.createIRI(parameters.get("id").toString());
		NanodashSession session = NanodashSession.get();

		String pageType = "users";
		if (session.getUserIri() != null && userIri.equals(session.getUserIri())) pageType = "mychannel";
		add(new TitleBar("titlebar", this, pageType));

		final String displayName = User.getDisplayName(userIri);
		add(new Label("pagetitle", displayName + " (user) | nanodash"));
		add(new Label("username", displayName));

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
				nanopubResults = ApiAccess.getRecent("find_signed_nanopubs", nanopubParams).getData();
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
