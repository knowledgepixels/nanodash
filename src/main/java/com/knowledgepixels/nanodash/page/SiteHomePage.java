package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * The home page of an instance in site mode (issue #692): the site's space page, served at
 * the root URL. Everything is inherited from {@link SpacePage}, its markup included; the
 * space is the configured one when the URL names none, which is what
 * {@link SpacePage#resolveSpace} does in site mode. Only ever the home page (see
 * {@link com.knowledgepixels.nanodash.WicketApplication#getHomePage()}), never mounted
 * elsewhere.
 */
public class SiteHomePage extends SpacePage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the site home page.
     *
     * @param parameters the page parameters; the space {@code id} may be left out
     */
    public SiteHomePage(final PageParameters parameters) {
        super(parameters);
    }

}
