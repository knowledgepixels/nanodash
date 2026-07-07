package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.behavior.AttributeAppender;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.Component;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultItemList components.
 */
public class QueryResultItemListBuilder implements Serializable {

    private final String markupId;
    private final ViewDisplay viewDisplay;
    private String contextId = null;
    private final QueryRef queryRef;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private AbstractResourceWithProfile pageResource = null;
    private String id = null;
    private String refRoot = null;

    private QueryResultItemListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    public static QueryResultItemListBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultItemListBuilder(markupId, queryRef, viewDisplay);
    }

    public QueryResultItemListBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultItemListBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    public QueryResultItemListBuilder id(String id) {
        this.id = id;
        return this;
    }

    public QueryResultItemListBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Pins this list to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref.
     * Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultItemListBuilder instance
     */
    public QueryResultItemListBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        String colClass = " col-" + viewDisplay.getDisplayWidth();
        if (response != null) {
            Component result = buildItemList(markupId, response);
            result.add(new AttributeAppender("class", colClass));
            return result;
        } else {
            ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                @Override
                public Component getApiResultComponent(String id, ApiResponse r) {
                    return buildItemList(id, r);
                }
            };
            comp.add(new AttributeAppender("class", colClass));
            return comp;
        }
    }

    private QueryResultItemList buildItemList(String markupId, ApiResponse response) {
        QueryResultItemList result = new QueryResultItemList(markupId, queryRef, response, viewDisplay);
        result.setContextId(contextId);
        result.setPageResource(pageResource);
        result.setResourceWithProfile(resourceWithProfile);
        result.setRefRoot(refRoot);
        ViewActionMappings.addResultActions(result, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
        return result;
    }
}
