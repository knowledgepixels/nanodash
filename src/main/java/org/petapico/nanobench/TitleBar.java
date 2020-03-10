package org.petapico.nanobench;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id) {
		super(id);
		add(new ProfileItem("profile"));
		if (ProfilePage.getUserIri() != null) {
			PageParameters params = new PageParameters();
			params.add("id", ProfilePage.getUserIri());
			add(new BookmarkablePageLink<UserPage>("mychannellink", UserPage.class, params));
		} else {
			add(new BookmarkablePageLink<UserPage>("mychannellink", ProfilePage.class));
		}
	}

}
