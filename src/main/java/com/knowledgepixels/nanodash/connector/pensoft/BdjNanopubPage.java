package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenNanopubPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Page for generating nanopubs for the Biodiversity Data Journal (BDJ).
 */
public class BdjNanopubPage extends ConnectorPage {

    /**
     * Mount path for the BDJ nanopub page.
     */
    public static final String MOUNT_PATH = "/connector/pensoft/bdj/np";

    /**
     * Constructor for the BDJ nanopub page.
     *
     * @param params Page parameters containing any necessary data for the page.
     */
    public BdjNanopubPage(final PageParameters params) {
        super(params);
        setResponsePage(GenNanopubPage.class, params.add("journal", "pensoft/bdj"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
