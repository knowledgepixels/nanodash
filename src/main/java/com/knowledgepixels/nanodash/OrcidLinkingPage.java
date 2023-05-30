package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class OrcidLinkingPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/orcidlinking";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public OrcidLinkingPage(final PageParameters parameters) {
		super(parameters);

		add(new TitleBar("titlebar", this));
		if (!NanodashSession.get().isProfileComplete()) {
			throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		}
		final NanodashSession session = NanodashSession.get();
		String introUri;
		if (session.getLocalIntroCount() == 0) {
			introUri = "(no introduction with local key found; first resolve this on <a href=\"" + ProfilePage.MOUNT_PATH + "\">your profile page</a>)";
		} else if (session.getLocalIntroCount() > 1) {
			introUri = "(several introductions with local key found; first resolve this on <a href=\"" + ProfilePage.MOUNT_PATH + "\">your profile page</a>)";
		} else {
			introUri = "<code>" + session.getLocalIntro().getNanopub().getUri() + "</code>";
		}
		add(new Label("introuri", introUri).setEscapeModelStrings(false));
	}

}
