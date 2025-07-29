package com.knowledgepixels.nanodash.connector.pensoft;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenNanopubPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Page for generating nanopublications for the Pensoft RIO journal.
 */
public class RioNanopubPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for the Pensoft RIO nanopub page.
     */
    public static final String MOUNT_PATH = "/connector/pensoft/rio/np";

    /**
     * Constructor for the RioNanopubPage.
     *
     * @param params Page parameters, typically containing the journal name.
     */
    public RioNanopubPage(final PageParameters params) {
        super(params);
        setResponsePage(GenNanopubPage.class, params.add("journal", "pensoft/rio"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
