package com.knowledgepixels.nanodash.page;

import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.component.ActivityPanel;
import com.knowledgepixels.nanodash.component.ApiResultComponent;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * TestPage is a simple page to test the API response.
 */
public class TestPage extends NanodashPage {

    /**
     * The mount path for this page.
     * This is used to access he page via URL.
     */
    public static final String MOUNT_PATH = "/test";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for TestPage.
     * Initializes the page with a title bar and an activity panel.
     *
     * @param parameters Page parameters
     */
    public TestPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        final QueryRef queryRef = new QueryRef(QueryApiAccess.GET_MONTHLY_TYPE_OVERVIEW_BY_PUBKEYS, "pubkey", "1162349fdeaf431e71ab55898cb2a425b971d466150c2aa5b3c1beb498045a37");
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        if (response != null) {
            add(new ActivityPanel("activity", response));
        } else {
            add(new ApiResultComponent("activity", queryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new ActivityPanel(markupId, response);
                }
            });

        }
    }

}
