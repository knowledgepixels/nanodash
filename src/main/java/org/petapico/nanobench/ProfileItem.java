package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ProfileItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileItem(String id) {
		super(id);
		NanobenchSession session = NanobenchSession.get();
		NanobenchPreferences prefs = NanobenchPreferences.get();
		IRI userId = session.getUserIri();
		if (prefs.isOrcidLoginMode() && userId == null) {
			ExternalLink l = new ExternalLink("profilelink", OrcidLoginPage.getOrcidLoginUrl());
			l.add(new Label("profiletext", "Login with ORCID"));
			add(l);
		} else if (prefs.isReadOnlyMode()) {
			BookmarkablePageLink<HomePage> l = new BookmarkablePageLink<>("profilelink", HomePage.class);
			l.add(new Label("profiletext", ""));
			add(l);
		} else {
			BookmarkablePageLink<ProfilePage> l = new BookmarkablePageLink<ProfilePage>("profilelink", ProfilePage.class);
			if (userId != null) {
				l.add(new Label("profiletext", User.getDisplayName(userId)));
			} else {
				l.add(new Label("profiletext", "incomplete profile"));
			}
			add(l);
		}
	}

}
