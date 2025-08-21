package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class MyChannelPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    public static final String MOUNT_PATH = "/mychannel";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

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
