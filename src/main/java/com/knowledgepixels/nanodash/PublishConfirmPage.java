package com.knowledgepixels.nanodash;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.KeyDeclaration;

public class PublishConfirmPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publishconfirm";

	public PublishConfirmPage(Nanopub np) {
		super();
		add(new TitleBar("titlebar"));
		PageParameters params = new PageParameters();
		params.add("id", NanodashSession.get().getUserIri());
		add(new BookmarkablePageLink<UserPage>("userlink", UserPage.class, params));
		add(new NanopubItem("nanopub", new NanopubElement(np), false, false));

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
	}

}
