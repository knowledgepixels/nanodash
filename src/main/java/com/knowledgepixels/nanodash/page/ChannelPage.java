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

public class ChannelPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/channel";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private Model<ArrayList<String>> selected = new Model<>();
	private IRI userIri;
	private boolean added = false;
	private Map<String,String> pubKeyMap;
	private CheckBoxMultipleChoice<String> pubkeySelection;
	
	public ChannelPage(final PageParameters parameters) {
		super(parameters);

		if (parameters.get("id") == null) throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		userIri = Utils.vf.createIRI(parameters.get("id").toString());
		NanodashSession session = NanodashSession.get();

		String pageType = "users";
		if (session.getUserIri() != null && userIri.equals(session.getUserIri())) pageType = "mychannel";
		add(new TitleBar("titlebar", this, pageType));

		final String displayName = User.getShortDisplayName(userIri);
		add(new Label("pagetitle", displayName + " (channel) | nanodash"));
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

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

	private synchronized void refresh() {
		if (added) {
			remove("nanopubs");
		}
		added = true;
		final String pubkeyHashes = getPubkeyHashesString();
		if (hasCachedNanopubList(pubkeyHashes)) {
			add(new NanopubResults("nanopubs", cachedNanopubLists.get(pubkeyHashes)));
			if (System.currentTimeMillis() - lastRefresh.get(pubkeyHashes) > 60 * 1000 && !isAlreadyRunning(pubkeyHashes)) {
				refreshStart.put(pubkeyHashes, System.currentTimeMillis());
				new Thread() {

					@Override
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
						try {
							updateNanopubList(pubkeyHashes);
						} finally {
							refreshStart.remove(pubkeyHashes);
						}
					}

				}.start();
			}
		} else {
			final boolean alreadyRunning = isAlreadyRunning(pubkeyHashes);
			refreshStart.put(pubkeyHashes, System.currentTimeMillis());
			add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public NanopubResults getLazyLoadComponent(String markupId) {
					if (alreadyRunning) {
						while (true) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException ex) {
								ex.printStackTrace();
							}
							if (!refreshStart.containsKey(pubkeyHashes)) break;
						}
						return new NanopubResults(markupId, cachedNanopubLists.get(pubkeyHashes));
					} else {
						NanopubResults nr = null;
						try {
							updateNanopubList(pubkeyHashes);
							nr = new NanopubResults(markupId, cachedNanopubLists.get(pubkeyHashes));
						} finally {
							refreshStart.remove(pubkeyHashes);
						}
						return nr;
					}
				}
	
				@Override
				protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
					super.onContentLoaded(content, target);
					if (target.get() != null) target.get().appendJavaScript("updateElements();");
				}
	
			});
		}
	}

	private static boolean isAlreadyRunning(String pubkeyHashes) {
		if (!refreshStart.containsKey(pubkeyHashes)) return false;
		return System.currentTimeMillis() - refreshStart.get(pubkeyHashes) < 60 * 1000;
	}

	private static void updateNanopubList(String pubkeyHashes) {
		List<NanopubElement> nanopubs = getNanopubList(pubkeyHashes);
		cachedNanopubLists.put(pubkeyHashes, nanopubs);
		lastRefresh.put(pubkeyHashes, System.currentTimeMillis());
	}

	private static List<NanopubElement> getNanopubList(String pubkeyHashes) {
		List<NanopubElement> nanopubs = new ArrayList<>();
		try {
			Map<String,String> nanopubParams = new HashMap<>();
			List<ApiResponseEntry> nanopubResults = new ArrayList<>();
			nanopubParams.put("pubkeyhashes", pubkeyHashes);
			nanopubResults = QueryAccess.get("RAaLOqOwHVAfH8PK4AzHz5UF-P4vTnd-QnmH4w9hxTo3Y/get-latest-nanopubs-from-pubkeys", nanopubParams).getData();
			while (!nanopubResults.isEmpty() && nanopubs.size() < 20) {
				ApiResponseEntry resultEntry = nanopubResults.remove(0);
				String npUri = resultEntry.get("np");
				nanopubs.add(new NanopubElement(npUri));
			}
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		return nanopubs;
	}

	private String getPubkeyHashesString() {
		String pubkeyHashes = "";
		for (String s : selected.getObject()) {
			pubkeyHashes += " " + Utils.createSha256HexHash(pubKeyMap.get(s));
		}
		if (!pubkeyHashes.isEmpty()) pubkeyHashes = pubkeyHashes.substring(1);
		return pubkeyHashes;
	}

	private static boolean hasCachedNanopubList(String pubkeyHashes) {
		if (!cachedNanopubLists.containsKey(pubkeyHashes)) return false;
		return System.currentTimeMillis() - lastRefresh.get(pubkeyHashes) < 24 * 60 * 60 * 1000;
	}

	private transient static Map<String,List<NanopubElement>> cachedNanopubLists = new HashMap<>();
	private transient static Map<String,Long> lastRefresh = new HashMap<>();
	private transient static Map<String,Long> refreshStart = new HashMap<>();

//	@Override
//	public void onBeforeRender() {
//		super.onBeforeRender();
//		if (hasBeenRendered()) {
//			setResponsePage(getPageClass(), getPageParameters());
//		}
//	}

}
