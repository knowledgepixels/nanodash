package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.TitleBar;

public class UserPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/user";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	private IRI userIri;
	
	public UserPage(final PageParameters parameters) {
		super(parameters);

		if (parameters.get("id") == null) throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		userIri = Utils.vf.createIRI(parameters.get("id").toString());
		//NanodashSession session = NanodashSession.get();

		String pageType = "users";
		add(new TitleBar("titlebar", this, pageType));

		final String displayName = User.getShortDisplayName(userIri);
		add(new Label("pagetitle", displayName + " (user) | nanodash"));
		add(new Label("username", displayName));

		add(new ExternalLink("fullid", userIri.stringValue(), userIri.stringValue()));

		add(new BookmarkablePageLink<Void>("showchannel", ChannelPage.class, new PageParameters().add("id", userIri.stringValue())));
	}

	protected boolean hasAutoRefreshEnabled() {
		return true;
	}

}
