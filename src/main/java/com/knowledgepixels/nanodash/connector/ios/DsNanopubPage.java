package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenNanopubPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Page for generating nanopublications for the iOS Data Science journal.
 */
public class DsNanopubPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for the iOS Data Science nanopub page.
     */
    public static final String MOUNT_PATH = "/connector/ios/ds/np";

    /**
     * Constructor for the iOS Data Science nanopub page.
     *
     * @param params Page parameters containing any necessary data for the page.
     */
    public DsNanopubPage(final PageParameters params) {
        super(params);
        setResponsePage(GenNanopubPage.class, params.add("journal", "ios/ds"));
    }

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
