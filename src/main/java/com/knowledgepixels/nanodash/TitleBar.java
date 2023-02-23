package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TitleBar extends Panel {

	private static final long serialVersionUID = 1L;

	public TitleBar(String id) {
		super(id);
		add(new ProfileItem("profile"));

		WebMarkupContainer mychannel = new WebMarkupContainer("mychannel");
		if (NanobenchSession.get().getUserIri() != null) {
			PageParameters params = new PageParameters();
			params.add("id", NanobenchSession.get().getUserIri());
			mychannel.add(new BookmarkablePageLink<UserPage>("mychannellink", UserPage.class, params));
		} else {
			mychannel.add(new BookmarkablePageLink<UserPage>("mychannellink", ProfilePage.class));
		}
		mychannel.setVisible(!NanobenchPreferences.get().isReadOnlyMode());
		add(mychannel);

		WebMarkupContainer publish = new WebMarkupContainer("publish");
		publish.setVisible(!NanobenchPreferences.get().isReadOnlyMode());
		add(publish);
	}

}
