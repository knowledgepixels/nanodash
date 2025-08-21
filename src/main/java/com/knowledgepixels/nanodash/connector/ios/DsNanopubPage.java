package com.knowledgepixels.nanodash.connector.ios;

import com.knowledgepixels.nanodash.connector.ConnectorPage;
import com.knowledgepixels.nanodash.connector.GenNanopubPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class DsNanopubPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    public static final String MOUNT_PATH = "/connector/ios/ds/np";

    public DsNanopubPage(final PageParameters params) {
        super(params);
        setResponsePage(GenNanopubPage.class, params.add("journal", "ios/ds"));
    }

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
