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
			add(new Label("text", "Click on the menu items above to explore nanopublications. This is a read-only instance, so you cannot publish new nanopublications here."));
		} else if (NanodashSession.get().isProfileComplete()) {
			add(new Label("text", "Click on the menu items above to explore or publish nanopublications."));
		} else if (NanodashPreferences.get().isOrcidLoginMode() && session.getUserIri() == null) {
			String loginUrl = OrcidLoginPage.getOrcidLoginUrl(".");
			add(new Label("text", "In order to see your own nanopublications and publish new ones, <a href=\"" + loginUrl + "\">login to ORCID</a> first.").setEscapeModelStrings(false));
		} else {
			add(new Label("text", "Before you can start, you first need to <a href=\"" + ProfilePage.MOUNT_PATH + "\">complete your profile</a>.").setEscapeModelStrings(false));
		}

		setOutputMarkupId(true);

		add(new AjaxLazyLoadPanel<UserList>("topcreators") {

			private static final long serialVersionUID = 1L;

			@Override
			public UserList getLazyLoadComponent(String markupId) {
				List<IRI> topUsers = new ArrayList<>();
				Map<IRI,String> userNotes = new HashMap<>();
				try {
					for (ApiResponseEntry e : QueryAccess.get("RAna6AB9majJbslfFCtrZaM3_QPKzeDnOUsbGOx2LUgfE/get-top-creators-last30d", null).getData()) {
						IRI userIri = Utils.vf.createIRI(e.get("userid"));
						topUsers.add(userIri);
						userNotes.put(userIri, "(" + e.get("count") + ")");
					}
				} catch (CsvValidationException | IOException ex) {
					ex.printStackTrace();
				}
				return new UserList(markupId, topUsers, userNotes);
			}

		});

		add(new AjaxLazyLoadPanel<NanopubResults>("mostrecent") {

			private static final long serialVersionUID = 1L;

			@Override
			public NanopubResults getLazyLoadComponent(String markupId) {
				List<NanopubElement> nanopubs = new ArrayList<>();
				try {
					for (ApiResponseEntry e : QueryAccess.get("RA7oUCHG8TEjVQpGTUN5sfu3_IQmza3aSBSCxfJdBc3Rs/get-most-recent-nanopubs", null).getData()) {
						nanopubs.add(new NanopubElement(e.get("np")));
						if (nanopubs.size() == 5) break;
					}
				} catch (CsvValidationException | IOException ex) {
					ex.printStackTrace();
				}
				return new NanopubResults(markupId, nanopubs);
			}

		});

	}

}
