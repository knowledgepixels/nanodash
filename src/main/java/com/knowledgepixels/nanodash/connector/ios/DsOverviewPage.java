package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Overview page for iOS Data Source (DS) connector.
 */
public class DsOverviewPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for the iOS Data Source connector overview page.
     */
    public static final String MOUNT_PATH = "/connector/ios/ds";

    /**
     * Constructor for the iOS Data Source overview page.
     *
     * @param params Page parameters to initialize the page.
     */
    public DsOverviewPage(PageParameters params) {
        super(params);
        setResponsePage(GenOverviewPage.class, params.add("journal", "ios/ds"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
