package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * ErrorPage is a Wicket page that serves as a generic error page.
 */
public class ErrorPage extends NanodashPage {

    /**
     * The mount path for the error page.
     */
    public static final String MOUNT_PATH = "/error";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Default constructor for ErrorPage.
     * Initializes the page with the provided parameters.
     *
     * @param parameters Page parameters to initialize the page.
     */
    public ErrorPage(final PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this, null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureResponse(WebResponse response) {
        super.configureResponse(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVersioned() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isErrorPage() {
        return true;
    }

}
