package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Overview page for the Biodiversity Data Journal (BDJ) connector.
 */
public class BdjOverviewPage extends ConnectorPage {

    /**
     * Mount path for the BDJ connector.
     */
    public static final String MOUNT_PATH = "/connector/pensoft/bdj";

    /**
     * Constructor for the BDJ overview page.
     *
     * @param params Page parameters to initialize the page.
     */
    public BdjOverviewPage(PageParameters params) {
        super(params);
        setResponsePage(GenOverviewPage.class, params.add("journal", "pensoft/bdj"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
