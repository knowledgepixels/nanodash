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
 * Builder class for creating QueryResultPlainParagraph components.
 */
public class QueryResultPlainParagraphBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String refRoot = null;

    private QueryResultPlainParagraphBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
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

    public QueryResultPlainParagraphBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Pins this paragraph to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref. Used
     * on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultPlainParagraphBuilder instance
     */
    public QueryResultPlainParagraphBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultPlainParagraph component.
     *
     * @return the QueryResultPlainParagraph component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, viewDisplay.getTitle(), this::buildPlainParagraph);
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        return comp;
    }

    private QueryResultPlainParagraph buildPlainParagraph(String markupId, ApiResponse response) {
        QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
        resultPlainParagraph.setPageResource(pageResource);
        resultPlainParagraph.setContextId(contextId);
        ViewActionMappings.addResultActions(resultPlainParagraph, viewDisplay, queryRef, id, contextId, pageResource, refRoot);
        return resultPlainParagraph;
    }

}
