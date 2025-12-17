package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.Component;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultPlainParagraph components.
 */
public class QueryResultPlainParagraphBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    private QueryResultPlainParagraphBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultPlainParagraphBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultPlainParagraphBuilder instance
     */
    public static QueryResultPlainParagraphBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultPlainParagraphBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the context ID for the QueryResultPlainParagraph.
     *
     * @param contextId the context ID
     * @return the current QueryResultPlainParagraphBuilder instance
     */
    public QueryResultPlainParagraphBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultPlainParagraphBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Builds the QueryResultPlainParagraph component.
     *
     * @return the QueryResultPlainParagraph component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        if (response != null) {
            QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
            resultPlainParagraph.setContextId(contextId);
            return resultPlainParagraph;
        } else {
            if (space != null) {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                        resultPlainParagraph.setProfiledResource(space);
                        resultPlainParagraph.setContextId(contextId);
                        return resultPlainParagraph;
                    }
                };
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                        resultPlainParagraph.setContextId(contextId);
                        return resultPlainParagraph;
                    }
                };
            }
        }
    }

}
