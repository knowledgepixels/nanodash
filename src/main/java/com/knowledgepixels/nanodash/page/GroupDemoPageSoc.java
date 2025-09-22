package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * <p>GroupDemoPageSoc class.</p>
 */
public class GroupDemoPageSoc extends NanodashPage {

    /**
     * Constant <code>MOUNT_PATH="/groupdemo-soc"</code>
     */
    public static final String MOUNT_PATH = "/groupdemo-soc";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * <p>Constructor for GroupDemoPageSoc.</p>
     *
     * @param parameters a {@link org.apache.wicket.request.mapper.parameter.PageParameters} object
     */
    public GroupDemoPageSoc(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));
    }

}
