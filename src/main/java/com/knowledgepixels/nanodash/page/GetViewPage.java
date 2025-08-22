package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Represents a page that retrieves a view based on the provided parameters.
 */
public class GetViewPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/get-view";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor that initializes the page with the given parameters.
     *
     * @param parameters the parameters to initialize the page with
     */
    public GetViewPage(final PageParameters parameters) {
        super(parameters);
        ViewPage.addNanopubItem(this, parameters);
    }

}
