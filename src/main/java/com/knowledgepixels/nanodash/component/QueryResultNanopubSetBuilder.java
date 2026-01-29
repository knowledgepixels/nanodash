package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.Component;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultNanopubSet components.
 */
public class QueryResultNanopubSetBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    private QueryResultNanopubSetBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultNanopubSetBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultNanopubSetBuilder instance
     */
    public static QueryResultNanopubSetBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultNanopubSetBuilder(markupId, queryRef, viewDisplay);
    }

    public QueryResultNanopubSetBuilder space(Space space) {
        this.space = space;
        return this;
    }

    /**
     * Sets the context ID for the QueryResultNanopubSet.
     *
     * @param contextId the context ID
     * @return the current QueryResultNanopubSetBuilder instance
     */
    public QueryResultNanopubSetBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultNanopubSetBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Builds the QueryResultNanopubSet component.
     *
     * @return the QueryResultNanopubSet component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        if (response != null) {
            return new QueryResultNanopubSet(markupId, queryRef, response, viewDisplay);
        } else {
            return new ApiResultComponent(markupId, queryRef) {
                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new QueryResultNanopubSet(markupId, queryRef, response, viewDisplay);
                }
            };
        }
    }

}
