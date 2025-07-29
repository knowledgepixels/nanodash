package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.ExploreDataTable;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Page to display a reference table for a given IRI.
 */
public class ReferenceTablePage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/reftable";

    private final String ref;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the ReferenceTablePage.
     *
     * @param parameters Page parameters containing the IRI reference.
     */
    public ReferenceTablePage(final PageParameters parameters) {
        super(parameters);

        ref = parameters.get("id").toString();
        add(new TitleBar("titlebar", this, null));
        final String shortName = IriItem.getShortNameFromURI(ref);
        add(new Label("termname", shortName));
        add(new ExternalLink("urilink", ref, ref));
        add(new Label("pagetitle", shortName + " (references) | nanodash"));

        add(ExploreDataTable.createComponent("table", ref, 0));
    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
