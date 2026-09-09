package com.knowledgepixels.nanodash.component;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.MagicQueryParams;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;

public class QueryResultThreadBuilder implements Serializable {

    private final String markupId;
    private final ViewDisplay viewDisplay;
    private final QueryRef queryRef;
    private String contextId = null;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private AbstractResourceWithProfile pageResource = null;
    private String id = null;
    private String refRoot = null;

    private QueryResultThreadBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    public static QueryResultThreadBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultThreadBuilder(markupId, queryRef, viewDisplay);
    }

    public QueryResultThreadBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultThreadBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    public QueryResultThreadBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    public QueryResultThreadBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Pins this thread to a specific ref (root definition), so action
     * visibility is gated against that claimant's authority rather than the
     * resource's representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current builder instance
     */
    public QueryResultThreadBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, viewDisplay.getTitle(), this::buildThread);
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        return comp;
    }

    private QueryResultThread buildThread(String markupId, ApiResponse response) {
        QueryResultThread result = new QueryResultThread(markupId, queryRef, response, viewDisplay);
        result.setContextId(contextId);
        result.setPageResource(pageResource);
        result.setResourceWithProfile(resourceWithProfile);
        result.setRefRoot(refRoot);
        ViewActionMappings.addResultActions(result, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
        result.populateComponent();
        return result;
    }

}
