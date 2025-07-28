package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.ThingListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Page to display a list of things (instances) for a given IRI.
 */
public class ThingListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/thinglist";

    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the ThingListPage.
     *
     * @param parameters Page parameters containing the IRI reference and mode.
     */
    public ThingListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        final String ref = parameters.get("ref").toString();
        final String shortName = IriItem.getShortNameFromURI(ref);
        final String mode = parameters.get("mode").toString();

        add(new Label("pagetitle", shortName + " (instances) | nanodash"));
        add(new Label("heading", shortName));

        add(new ExternalLink("urilink", ref, ref));

        add(ThingListPanel.createComponent("list", ThingListPanel.Mode.valueOf(mode.toUpperCase()), ref, null, 0));
    }

}
