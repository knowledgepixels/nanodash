package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultSvg components.
 */
public class QueryResultSvgBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String refRoot = null;

    private QueryResultSvgBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultSvgBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultSvgBuilder instance
     */
    public static QueryResultSvgBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultSvgBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the context ID for the QueryResultSvg.
     *
     * @param contextId the context ID
     * @return the current QueryResultSvgBuilder instance
     */
    public QueryResultSvgBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultSvgBuilder id(String id) {
        this.id = id;
        return this;
    }

    public QueryResultSvgBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Pins this view to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref.
     * Used on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultSvgBuilder instance
     */
    public QueryResultSvgBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultSvg component.
     *
     * @return the QueryResultSvg component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, viewDisplay.getTitle(), this::buildSvg);
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        return comp;
    }

    private QueryResultSvg buildSvg(String markupId, ApiResponse response) {
        QueryResultSvg resultSvg = new QueryResultSvg(markupId, queryRef, response, viewDisplay);
        resultSvg.setPageResource(pageResource);
        resultSvg.setContextId(contextId);
        ViewActionMappings.addResultActions(resultSvg, viewDisplay, queryRef, id, contextId, pageResource, refRoot);
        return resultSvg;
    }

}
