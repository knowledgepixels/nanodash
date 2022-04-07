package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class HomePage extends WebPage {

	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		final NanobenchSession session = NanobenchSession.get();
		String v = WicketApplication.getThisVersion();
		String lv = WicketApplication.getLatestVersion();
		if (v.endsWith("-SNAPSHOT")) {
			add(new Label("warning", "You are running a temporary snapshot version of Nanobench (" + v + "). The latest public version is " + lv + "."));
		} else if (lv != null && !v.equals(lv)) {
			add(new Label("warning", "There is a new version available: " + lv + ". You are currently using " + v + ". " +
					"Run 'update' (Unix/Mac) or 'update-under-windows.bat' (Windows) to update to the latest version, or manually download it " +
					"<a href=\"" + WicketApplication.LATEST_RELEASE_URL + "\">here</a>.").setEscapeModelStrings(false));
		} else {
			add(new Label("warning", ""));
		}
		if (NanobenchPreferences.get().isReadOnlyMode()) {
			add(new Label("text", "Click on the menu items above to explore nanopublications. This is a read-only instance, so you cannot publish new nanopublications here."));
		} else if (NanobenchSession.get().isProfileComplete()) {
			add(new Label("text", "Click on the menu items above to explore or publish nanopublications."));
		} else if (NanobenchPreferences.get().isOrcidLoginMode() && session.getUser() == null) {
			add(new Label("text", "In order to see your own nanopublications and publish new ones, you need to <a href=\"" + OrcidLoginPage.getOrcidLoginUrl() + "\">login to ORCID</a> first.").setEscapeModelStrings(false));
		} else {
			add(new Label("text", "Before you can start, you first need to <a href=\"./profile\">complete your profile</a>.").setEscapeModelStrings(false));
		}
	}

}
