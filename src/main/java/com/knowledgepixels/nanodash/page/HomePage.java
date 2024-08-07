package com.knowledgepixels.nanodash.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.UserList;
import com.opencsv.exceptions.CsvValidationException;

public class HomePage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/";

	private static List<IRI> topUsers;
	private static Map<IRI,String> topAuthorNotes;
	private static List<IRI> topAuthors;
	private static Map<IRI,String> topUserNotes;
	private static List<NanopubElement> recentNanopubs;
	private static List<NanopubElement> latestAccepted;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public HomePage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this, null));
		final NanodashSession session = NanodashSession.get();
		String v = WicketApplication.getThisVersion();
		String lv = WicketApplication.getLatestVersion();
		if (NanodashPreferences.get().isOrcidLoginMode()) {
			add(new Label("warning", ""));
		} else if (v.endsWith("-SNAPSHOT")) {
			add(new Label("warning", "You are running a temporary snapshot version of Nanodash (" + v + "). The latest public version is " + lv + "."));
		} else if (lv != null && !v.equals(lv)) {
			add(new Label("warning", "There is a new version available: " + lv + ". You are currently using " + v + ". " +
					"Run 'update' (Unix/Mac) or 'update-under-windows.bat' (Windows) to update to the latest version, or manually download it " +
					"<a href=\"" + WicketApplication.LATEST_RELEASE_URL + "\">here</a>.").setEscapeModelStrings(false));
		} else {
			add(new Label("warning", ""));
		}
		if (NanodashPreferences.get().isReadOnlyMode()) {
			add(new Label("text", "This is a read-only instance, so you cannot publish new nanopublications here."));
		} else if (NanodashSession.get().isProfileComplete()) {
			add(new Label("text", ""));
		} else if (NanodashPreferences.get().isOrcidLoginMode() && session.getUserIri() == null) {
			String loginUrl = OrcidLoginPage.getOrcidLoginUrl(".");
			add(new Label("text", "In order to see your own nanopublications and publish new ones, <a href=\"" + loginUrl + "\">login to ORCID</a> first.").setEscapeModelStrings(false));
		} else {
			add(new Label("text", "Before you can start, you first need to <a href=\"" + ProfilePage.MOUNT_PATH + "\">complete your profile</a>.").setEscapeModelStrings(false));
		}

		setOutputMarkupId(true);

		if (topUsers != null) {
			add(new UserList("topcreators", topUsers, topUserNotes));
		} else {
			add(new AjaxLazyLoadPanel<UserList>("topcreators") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public UserList getLazyLoadComponent(String markupId) {
					refreshLists(false);
					return new UserList(markupId, topUsers, topUserNotes);
				}
	
			});
		}

		if (topAuthors != null) {
			add(new UserList("topauthors", topAuthors, topAuthorNotes));
		} else {
			add(new AjaxLazyLoadPanel<UserList>("topauthors") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public UserList getLazyLoadComponent(String markupId) {
					refreshLists(false);
					return new UserList(markupId, topAuthors, topAuthorNotes);
				}
	
			});
		}

		if (recentNanopubs != null) {
			add(new NanopubResults("mostrecent", recentNanopubs));
		} else {
			add(new AjaxLazyLoadPanel<NanopubResults>("mostrecent") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public NanopubResults getLazyLoadComponent(String markupId) {
					refreshLists(false);
					return new NanopubResults(markupId, recentNanopubs);
				}
	
			});
		}

		if (latestAccepted != null) {
			add(new NanopubResults("latestaccepted", latestAccepted));
		} else {
			add(new AjaxLazyLoadPanel<NanopubResults>("latestaccepted") {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				public NanopubResults getLazyLoadComponent(String markupId) {
					refreshLists(false);
					return new NanopubResults(markupId, latestAccepted);
				}
	
			});
		}

	}

	private static boolean refreshingLists = false;

	public static void refreshLists(boolean force) {
		if (!force && topUsers != null) return;
		if (refreshingLists) {
			while (true) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
				if (!refreshingLists) return;
			}
		}
		refreshingLists = true;
		try {
			try {
				ApiResponse resp = QueryAccess.get("RAna6AB9majJbslfFCtrZaM3_QPKzeDnOUsbGOx2LUgfE/get-top-creators-last30d", null);
				topUsers = new ArrayList<>();
				topUserNotes = new HashMap<>();
				for (ApiResponseEntry e : resp.getData()) {
					IRI userIri = Utils.vf.createIRI(e.get("userid"));
					topUsers.add(userIri);
					topUserNotes.put(userIri, "(" + e.get("count") + ")");
				}
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
			}
	
			try {
				ApiResponse resp = QueryAccess.get("RACx9IgQMsU3UZqrGlYk6efLg5jJTFldTrXypwTTJbo40/get-top-authors", null);
				topAuthors = new ArrayList<>();
				topAuthorNotes = new HashMap<>();
				for (ApiResponseEntry e : resp.getData()) {
					IRI userIri = Utils.vf.createIRI(e.get("author"));
					topAuthors.add(userIri);
					topAuthorNotes.put(userIri, "(" + e.get("npcount") + ")");
				}
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
			}
	
			try {
				ApiResponse resp = QueryAccess.get("RA7oUCHG8TEjVQpGTUN5sfu3_IQmza3aSBSCxfJdBc3Rs/get-most-recent-nanopubs", null);
				recentNanopubs = new ArrayList<>();
				for (ApiResponseEntry e : resp.getData()) {
					recentNanopubs.add(new NanopubElement(e.get("np")));
					if (recentNanopubs.size() == 5) break;
				}
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
			}
	
			try {
				ApiResponse resp = QueryAccess.get("RAL-Y2PSWEtvpAYpdx4PF3MJ3TuZB7cFskTVdPA_JCg3o/get-latest-accepted", null);
				latestAccepted = new ArrayList<>();
				for (ApiResponseEntry e : resp.getData()) {
					latestAccepted.add(new NanopubElement(e.get("np")));
					if (latestAccepted.size() == 5) break;
				}
			} catch (CsvValidationException | IOException ex) {
				ex.printStackTrace();
			}
		} finally {
			refreshingLists = false;
		}
	}

}
