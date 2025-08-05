package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * This page displays the ORCID linking status and provides information about the user's local introduction.
 */
public class OrcidLinkingPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/orcidlinking";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor that initializes the page with the user's ORCID linking status.
     *
     * @param parameters The page parameters, which may include user ID or other relevant data.
     */
    public OrcidLinkingPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));
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
