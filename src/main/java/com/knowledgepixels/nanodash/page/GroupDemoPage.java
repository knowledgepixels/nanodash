package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * <p>GroupDemoPage class.</p>
 */
public class GroupDemoPage extends NanodashPage {

    /**
     * Constant <code>MOUNT_PATH="/groupdemo"</code>
     */
    public static final String MOUNT_PATH = "/groupdemo";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * <p>Constructor for GroupDemoPage.</p>
     *
     * @param parameters a {@link org.apache.wicket.request.mapper.parameter.PageParameters} object
     * @throws org.nanopub.extra.services.FailedApiCallException if any.
     */
    public GroupDemoPage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        add(new TitleBar("titlebar", this, null));
    }

}
