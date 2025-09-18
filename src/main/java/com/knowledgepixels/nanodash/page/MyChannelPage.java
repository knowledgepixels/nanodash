package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * This page redirects the user to their channel page if they are logged in,
 */
public class MyChannelPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/mychannel";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor that redirects the user to their channel page or profile page.
     *
     * @param parameters The page parameters, which may include user ID or other relevant data.
     */
    public MyChannelPage(final PageParameters parameters) {
        super(parameters);
        if (NanodashSession.get().getUserIri() != null) {
            parameters.add("id", NanodashSession.get().getUserIri());
            String redirectUrl = ChannelPage.MOUNT_PATH + "?" + Utils.getPageParametersAsString(parameters);
            throw new RedirectToUrlException(redirectUrl);
        } else if (NanodashPreferences.get().isOrcidLoginMode()) {
            throw new RedirectToUrlException(OrcidLoginPage.getOrcidLoginUrl(MOUNT_PATH, parameters));
        } else {
            throw new RedirectToUrlException(ProfilePage.MOUNT_PATH + "?" + Utils.getPageParametersAsString(parameters));
        }
    }

}
