package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.Component;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultList components.
 */
public class QueryResultListBuilder implements Serializable {

    private String markupId;
    private QueryRef queryRef;
    private ViewDisplay viewDisplay = null;

    private QueryResultListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultListBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultListBuilder instance
     */
    public static QueryResultListBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultListBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Builds the QueryResultList component.
     *
     * @return the QueryResultList component
     */
    public Component build() {
        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        ApiResponse response = ApiCache.retrieveResponse(queryRef);
        if (response != null) {
            return new QueryResultList(markupId, grlcQuery, response, viewDisplay);
        } else {
            return new ApiResultComponent(markupId, queryRef) {
                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new QueryResultList(markupId, grlcQuery, response, viewDisplay);
                }
            };
        }
    }

}
