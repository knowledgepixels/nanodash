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
 * Builder class for creating QueryResultNanopubSet components.
 */
public class QueryResultNanopubSetBuilder implements Serializable {

    private final String markupId;
    private final ViewDisplay viewDisplay;
    private String contextId = null;
    private final QueryRef queryRef;
    private boolean hasTitle = true;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private AbstractResourceWithProfile pageResource = null;
    private String id = null;
    private String refRoot = null;
    private Long itemsPerPage = null;

    private QueryResultNanopubSetBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
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

    public QueryResultNanopubSetBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    public QueryResultNanopubSetBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    public QueryResultNanopubSetBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Pins this set to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref.
     * Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultNanopubSetBuilder instance
     */
    public QueryResultNanopubSetBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    public QueryResultNanopubSetBuilder setItemsPerPage(long itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        return this;
    }

    /**
     * Removes the title from the QueryResultNanopubSet.
     *
     * @return the current QueryResultNanopubSetBuilder instance
     */
    public QueryResultNanopubSetBuilder noTitle() {
        this.hasTitle = false;
        return this;
    }

    /**
     * Builds the QueryResultNanopubSet component.
     *
     * @return the QueryResultNanopubSet component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        String colClass = " col-" + viewDisplay.getDisplayWidth();
        long resolvedItemsPerPage = itemsPerPage != null ? itemsPerPage : viewDisplay.getPageSize();
        if (response != null) {
            QueryResultNanopubSet queryResultNanopubSet = buildNanopubSet(markupId, response, resolvedItemsPerPage);
            queryResultNanopubSet.add(new AttributeAppender("class", colClass));
            return queryResultNanopubSet;
        } else {
            ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return buildNanopubSet(markupId, response, resolvedItemsPerPage);
                }
            };
            comp.add(new AttributeAppender("class", colClass));
            return comp;
        }
    }

    private QueryResultNanopubSet buildNanopubSet(String markupId, ApiResponse response, long resolvedItemsPerPage) {
        QueryResultNanopubSet queryResultNanopubSet = new QueryResultNanopubSet(markupId, queryRef, response, viewDisplay, resolvedItemsPerPage);
        queryResultNanopubSet.setContextId(contextId);
        queryResultNanopubSet.setPageResource(pageResource);
        queryResultNanopubSet.setResourceWithProfile(resourceWithProfile);
        queryResultNanopubSet.setRefRoot(refRoot);
        // Result actions become entries of the view's dropdown menu, which
        // populateComponent() builds — so they are added first.
        ViewActionMappings.addResultActions(queryResultNanopubSet, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
        queryResultNanopubSet.populateComponent();
        queryResultNanopubSet.setTitleVisible(hasTitle);
        return queryResultNanopubSet;
    }

}
