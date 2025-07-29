package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * <p>GroupDemoPageSoc class.</p>
 */
public class GroupDemoPageSoc extends NanodashPage {

    private static final long serialVersionUID = 1L;

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
