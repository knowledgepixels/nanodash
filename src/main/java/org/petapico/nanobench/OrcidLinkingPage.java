package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;


public class OrcidLinkingPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/orcidlinking";

	public OrcidLinkingPage(final PageParameters parameters) {
		super();
		add(new TitleBar("titlebar"));
		if (!NanobenchSession.get().isProfileComplete()) {
			throw new RedirectToUrlException(ProfilePage.MOUNT_PATH);
		}
		final NanobenchSession session = NanobenchSession.get();
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
