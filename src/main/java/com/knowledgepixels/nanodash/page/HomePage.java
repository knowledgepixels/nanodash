package com.knowledgepixels.nanodash.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.TitleBar;
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

		List<IRI> topUsers = new ArrayList<>();
		try {
			for (ApiResponseEntry e : QueryAccess.get("RAna6AB9majJbslfFCtrZaM3_QPKzeDnOUsbGOx2LUgfE/get-top-creators-last30d", null).getData()) {
				topUsers.add(Utils.vf.createIRI(e.get("userid")));
			}
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		add(new DataView<IRI>("topcreators", new ListDataProvider<IRI>(topUsers)) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(Item<IRI> item) {
				PageParameters params = new PageParameters();
				params.add("id", item.getModelObject());
				BookmarkablePageLink<UserPage> l = new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params);
				l.add(new Label("linktext", User.getDisplayName(item.getModelObject())));
				item.add(l);
			}

		});
	}

}
