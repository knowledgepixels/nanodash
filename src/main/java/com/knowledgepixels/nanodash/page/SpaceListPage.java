package com.knowledgepixels.nanodash.page;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.QueryResultTableBuilder;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * The Spaces page: a single sortable table of all known spaces, including type.
 */
public class SpaceListPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/spaces";

    private static final String ALL_SPACES_VIEW = "https://w3id.org/np/RANB1bHSVXw8W0aFTvPURWpq960qJm9jEsI1rUDxXwMDY/all-spaces-view";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the SpaceListPage.
     *
     * @param parameters the page parameters
     */
    public SpaceListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "connectors"));

        View allSpacesView = View.get(ALL_SPACES_VIEW);
        QueryRef queryRef = new QueryRef(allSpacesView.getQuery().getQueryId());
        add(QueryResultTableBuilder.create("spaces", queryRef, new ViewDisplay(allSpacesView)).build());
    }

}
