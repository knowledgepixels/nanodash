package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultList components.
 */
public class QueryResultListBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String postPublishTab = null;
    private String refRoot = null;

    private QueryResultListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
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

    public QueryResultListBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    /**
     * Sets the context ID for the QueryResultList.
     *
     * @param contextId the context ID
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultListBuilder id(String id) {
        this.id = id;
        return this;
    }

    public QueryResultListBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Sets the tab to return to after publishing one of this view's action
     * buttons (e.g. {@code "about"}). Null leaves the post-publish redirect on
     * its default tab.
     *
     * @param postPublishTab the tab name, or null for the default
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder postPublishTab(String postPublishTab) {
        this.postPublishTab = postPublishTab;
        return this;
    }

    /**
     * Pins this list to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref. Used
     * on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultList component.
     *
     * @return the QueryResultList component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, viewDisplay.getTitle(), this::buildList);
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        return comp;
    }

    private QueryResultList buildList(String markupId, ApiResponse response) {
        QueryResultList resultList = new QueryResultList(markupId, queryRef, response, viewDisplay);
        resultList.setResourceWithProfile(resourceWithProfile);
        resultList.setPageResource(pageResource);
        resultList.setContextId(contextId);
        resultList.setPostPublishTab(postPublishTab);
        resultList.setRefRoot(refRoot);
        ViewActionMappings.addResultActions(resultList, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
        return resultList;
    }

}
