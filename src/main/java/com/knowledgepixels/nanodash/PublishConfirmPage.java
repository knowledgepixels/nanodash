package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.KeyDeclaration;

public class PublishConfirmPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publishconfirm";

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public PublishConfirmPage(Nanopub np, PageParameters params) {
		super(params);
		add(new TitleBar("titlebar", this));

		if (!getPageParameters().get("postpub-redirect-url").isNull()) {
			String forwardUrl = getPageParameters().get("postpub-redirect-url").toString();
			String paramString = Utils.getPageParametersAsString(new PageParameters().add("id", np.getUri()));
			throw new RedirectToUrlException(forwardUrl + "?" + paramString);
		}

		add(new BookmarkablePageLink<UserPage>("userlink", UserPage.class, new PageParameters().add("id", NanodashSession.get().getUserIri())));
		add(new NanopubItem("nanopub", new NanopubElement(np), false, false).expanded());

		final NanodashSession session = NanodashSession.get();
		boolean hasKnownOwnLocalIntro = session.getLocalIntroCount() > 0;
		boolean someIntroJustNowPublished = Utils.usesPredicateInAssertion(np, KeyDeclaration.DECLARED_BY);
		if (someIntroJustNowPublished) NanodashSession.get().setIntroPublishedNow();
		boolean lastIntroPublishedMoreThanFiveMinsAgo = session.getTimeSinceLastIntroPublished() > 5 * 60 * 1000;
		if (!hasKnownOwnLocalIntro && session.hasIntroPublished()) User.refreshUsers();
		add(new WebMarkupContainer("missing-intro-warning").setVisible(!hasKnownOwnLocalIntro && lastIntroPublishedMoreThanFiveMinsAgo));

		if (Utils.isNanopubOfClass(np, Template.ASSERTION_TEMPLATE_CLASS)) {
			add(new WebMarkupContainer("use-template").add(
					new BookmarkablePageLink<WebPage>("template-link", PublishPage.class, new PageParameters().add("template", np.getUri())))
				);
		} else {
			add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
		}
		add(new BookmarkablePageLink<WebPage>("create-another-link", PublishPage.class, getPageParameters()));
	}

}
