package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.KeyDeclaration;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;

public class PublishConfirmPage extends NanodashPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/publishconfirm";

	private final Nanopub np;

	@Override
	public String getMountPath() {
		return MOUNT_PATH;
	}

	public PublishConfirmPage(Nanopub np, PageParameters params) {
		super(params);
		this.np = np;

		add(new TitleBar("titlebar", this, "publish"));

		add(new BookmarkablePageLink<Void>("userlink", UserPage.class, new PageParameters().add("id", NanodashSession.get().getUserIri())));
		add(new NanopubItem("nanopub", NanopubElement.get(np)));

		final NanodashSession session = NanodashSession.get();
		boolean hasKnownOwnLocalIntro = session.getLocalIntroCount() > 0;
		boolean someIntroJustNowPublished = Utils.usesPredicateInAssertion(np, KeyDeclaration.DECLARED_BY);
		if (someIntroJustNowPublished) NanodashSession.get().setIntroPublishedNow();
		boolean lastIntroPublishedMoreThanFiveMinsAgo = session.getTimeSinceLastIntroPublished() > 5 * 60 * 1000;
		if (!hasKnownOwnLocalIntro && session.hasIntroPublished()) User.refreshUsers();
		add(new WebMarkupContainer("missing-intro-warning").setVisible(!hasKnownOwnLocalIntro && lastIntroPublishedMoreThanFiveMinsAgo));

		if (Utils.isNanopubOfClass(np, Template.ASSERTION_TEMPLATE_CLASS)) {
			add(new WebMarkupContainer("use-template").add(
					new BookmarkablePageLink<Void>("template-link", PublishPage.class, new PageParameters().add("template", np.getUri())))
				);
		} else {
			add(new WebMarkupContainer("use-template").add(new Label("template-link")).setVisible(false));
		}

		PageParameters plainLinkParams = new PageParameters();
		plainLinkParams.add("template", params.get("template"));
		if (!params.get("template-version").isEmpty()) {
			plainLinkParams.add("template-version", params.get("template-version"));
		}
		add(new BookmarkablePageLink<Void>("publish-another-link", PublishPage.class, plainLinkParams));

		PageParameters linkParams = new PageParameters(params);
		linkParams.remove("supersede");
		linkParams.remove("supersede-a");
		boolean publishAnotherFilledLinkVisible = false;
		for (NamedPair n : linkParams.getAllNamed()) {
			if (n.getKey().equals("template")) continue;
			if (n.getKey().equals("template-version")) continue;
			publishAnotherFilledLinkVisible = true;
		}
		if (publishAnotherFilledLinkVisible) {
			add(new BookmarkablePageLink<Void>("publish-another-filled-link", PublishPage.class, linkParams));
		} else {
			add(new Label("publish-another-filled-link", "").setVisible(false));
		}
	}

	@Override
	protected void onBeforeRender() {
		if (!getPageParameters().get("postpub-redirect-url").isNull()) {
			String forwardUrl = getPageParameters().get("postpub-redirect-url").toString();
			if (forwardUrl.contains("?")) {
				// TODO: Add here URI of created nanopublication too?
				throw new RedirectToUrlException(forwardUrl);
			} else {
				String paramString = Utils.getPageParametersAsString(new PageParameters().add("id", np.getUri()));
				throw new RedirectToUrlException(forwardUrl + "?" + paramString);
			}
		}
		super.onBeforeRender();
	}

}
