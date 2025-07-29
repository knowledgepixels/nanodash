package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Overview page for the Pensoft RIO journal.
 */
public class RioOverviewPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for the Pensoft RIO journal overview page.
     */
    public static final String MOUNT_PATH = "/connector/pensoft/rio";

    /**
     * Constructor for the RioOverviewPage.
     *
     * @param params Page parameters, typically containing the journal identifier.
     */
    public RioOverviewPage(PageParameters params) {
        super(params);
        setResponsePage(GenOverviewPage.class, params.add("journal", "pensoft/rio"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
