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
    private AbstractResourceWithProfile pageResource = null;

    private QueryResultItemListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
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
        return this;
    }

    public QueryResultItemListBuilder id(String id) {
        return this;
    }

    public QueryResultItemListBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        String colClass = " col-" + viewDisplay.getDisplayWidth();
        if (response != null) {
            QueryResultItemList result = new QueryResultItemList(markupId, queryRef, response, viewDisplay);
            result.setContextId(contextId);
            result.setPageResource(pageResource);
            result.add(new AttributeAppender("class", colClass));
            return result;
        } else {
            ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                @Override
                public Component getApiResultComponent(String id, ApiResponse r) {
                    QueryResultItemList result = new QueryResultItemList(id, queryRef, r, viewDisplay);
                    result.setContextId(contextId);
                    result.setPageResource(pageResource);
                    return result;
                }
            };
            comp.add(new AttributeAppender("class", colClass));
            return comp;
        }
    }
}
