package com.knowledgepixels.nanodash.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.opencsv.exceptions.CsvValidationException;

public class UserPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/user";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private Model<ArrayList<String>> selected = new Model<>();
	private IRI userIri;
	private boolean added = false;
	private Map<String,String> pubKeyMap;
	private CheckBoxMultipleChoice<String> pubkeySelection;
	
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
			String lKeyShort = Utils.getShortPubkeyLocationLabel(session.getPubkeyString(), userIri);
			pubKeyList.add(lKeyShort);
			pubKeyMap.put(lKeyShort, session.getPubkeyString());
		}
		for (String pk : User.getPubkeys(userIri, null)) {
			String keyShort = Utils.getShortPubkeyLocationLabel(pk, userIri);
			if (!pubKeyMap.containsKey(keyShort)) {
				pubKeyList.add(keyShort);
				pubKeyMap.put(keyShort, pk);
			}
		}

		pubkeySelection = new CheckBoxMultipleChoice<String>("pubkeygroup", selected, pubKeyList);
		pubkeySelection.setDefaultModelObject(new ArrayList<String>(pubKeyList));
		pubkeySelection.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			private static final long serialVersionUID = -6398658082085108029L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				System.err.print("PUBKEYS SELECTED:");
				for (String s : selected.getObject()) {
					System.err.print(" " + Utils.createSha256HexHash(pubKeyMap.get(s)));
				}
				System.err.println();
				refresh();
				setResponsePage(target.getPage());
				target.appendJavaScript("updateElements();");
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
				List<NanopubElement> nanopubs = new ArrayList<>();
				try {
					Map<String,String> nanopubParams = new HashMap<>();
					List<ApiResponseEntry> nanopubResults = new ArrayList<>();
					String pubkeyHashes = "";
					for (String s : selected.getObject()) {
						pubkeyHashes += " " + Utils.createSha256HexHash(pubKeyMap.get(s));
					}
					if (!pubkeyHashes.isEmpty()) pubkeyHashes = pubkeyHashes.substring(1);
					nanopubParams.put("pubkeyhashes", pubkeyHashes);
					nanopubResults = QueryAccess.get("RAaLOqOwHVAfH8PK4AzHz5UF-P4vTnd-QnmH4w9hxTo3Y/get-latest-nanopubs-from-pubkeys", nanopubParams).getData();
					while (!nanopubResults.isEmpty() && nanopubs.size() < 20) {
						ApiResponseEntry resultEntry = nanopubResults.remove(0);
						String npUri = resultEntry.get("np");
						nanopubs.add(new NanopubElement(npUri, false));
					}
				} catch (CsvValidationException | IOException ex) {
					ex.printStackTrace();
				}
				NanopubResults r = new NanopubResults(markupId, nanopubs);
				return r;
			}

			@Override
			protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
				super.onContentLoaded(content, target);
				if (target.get() != null) target.get().appendJavaScript("updateElements();");
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
