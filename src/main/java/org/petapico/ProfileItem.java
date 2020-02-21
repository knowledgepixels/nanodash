package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ProfileItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ProfileItem(String id) {
		super(id);
		IRI userIri = ProfilePage.getUserIri();
		BookmarkablePageLink<ProfilePage> l = new BookmarkablePageLink<ProfilePage>("profilelink", ProfilePage.class);
		if (userIri != null) {
			User user = User.getUser(userIri.stringValue());
			if (user != null) {
				l.add(new Label("profiletext", user.getDisplayName()));
			} else {
				l.add(new Label("profiletext", userIri));
			}
		} else {
			l.add(new Label("profiletext", "incomplete profile"));
		}
		add(l);
	}

}
