package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;

/**
 * A component that retrieves and displays the result of an API call.
 * It uses AjaxLazyLoadPanel to load the content lazily and shows a loading indicator while waiting for the response.
 */
public abstract class ApiResultComponent extends ResultComponent {

    private final QueryRef queryRef;
    private ApiResponse response = null;
    private static final Logger logger = LoggerFactory.getLogger(ApiResultComponent.class);

    /**
     * Constructor for ApiResultComponent using a QueryRef object.
     *
     * @param id       the component id
     * @param queryRef the QueryRef object containing the query name and parameters
     */
    public ApiResultComponent(String id, QueryRef queryRef) {
        super(id);
        this.queryRef = queryRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getLazyLoadComponent(String markupId) {
        while (true) {
            if (!ApiCache.isRunning(queryRef)) {
                try {
                    response = ApiCache.retrieveResponse(queryRef);
                    if (response != null) break;
                } catch (Exception ex) {
                    return new Label(markupId, "<span class=\"negative\">API call failed.</span>").setEscapeModelStrings(false);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting for API response", ex);
            }
        }
        return getApiResultComponent(markupId, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isContentReady() {
        return response != null || !ApiCache.isRunning(queryRef);
    }

    /**
     * Abstract method to be implemented by subclasses to provide the component that displays the API result.
     *
     * @param markupId the markup ID for the component
     * @param response the API response to display
     * @return a Component that displays the API result
     */
    // TODO Use lambda instead of abstract method?
    public abstract Component getApiResultComponent(String markupId, ApiResponse response);

}
