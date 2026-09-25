package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.SiteMode;
import com.knowledgepixels.nanodash.component.ResultComponent;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * What a site shows while its space is not loaded yet (issue #692): right after a start, the
 * space repository can be a few seconds away from knowing the site's space, and the space page
 * cannot be built without it. This page shows the wait indicator and sends the browser back to
 * the home page shortly after, until the space is there. A request thread never waits for the
 * repository itself.
 */
public class SiteLoadingPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/site-loading";

    /**
     * How long the browser waits before asking for the home page again, in seconds.
     */
    private static final int RETRY_SECONDS = 3;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the loading page.
     *
     * @param parameters the page parameters (ignored)
     */
    public SiteLoadingPage(final PageParameters parameters) {
        super(parameters);
        // Nothing to wait for: the space arrived in the meantime, or this is not a site at all.
        if (!SiteMode.isEnabled() || SiteMode.getSpace() != null) {
            throw new RedirectToUrlException(SiteHomePage.MOUNT_PATH);
        }
        add(new TitleBar("titlebar", this));
        add(new Label("pagetitle", SiteMode.getName()));
        add(new Label("wait", ResultComponent.getSectionWaitHtml()).setEscapeModelStrings(false));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sends the browser back to the home page after a moment.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(StringHeaderItem.forString(
                "<meta http-equiv=\"refresh\" content=\"" + RETRY_SECONDS + ";url=" + SiteHomePage.MOUNT_PATH + "\" />\n"));
    }

}
