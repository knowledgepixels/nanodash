package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.MagicQueryParams;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultNetwork components.
 */
public class QueryResultNetworkBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String refRoot = null;

    private QueryResultNetworkBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultNetworkBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultNetworkBuilder instance
     */
    public static QueryResultNetworkBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultNetworkBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the context ID for the QueryResultNetwork.
     *
     * @param contextId the context ID
     * @return the current QueryResultNetworkBuilder instance
     */
    public QueryResultNetworkBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * Sets the id of the resource this view is shown for.
     *
     * @param id the resource id
     * @return the current QueryResultNetworkBuilder instance
     */
    public QueryResultNetworkBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the resource whose page this view is shown on.
     *
     * @param pageResource the page's resource
     * @return the current QueryResultNetworkBuilder instance
     */
    public QueryResultNetworkBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Pins this view to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref.
     * Used on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultNetworkBuilder instance
     */
    public QueryResultNetworkBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultNetwork component.
     *
     * @return the QueryResultNetwork component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, viewDisplay.getTitle(), this::buildNetwork, viewDisplay.isHideWhenEmpty());
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        // Exactly the cold path: with nothing cached, what goes on the page is a placeholder
        // with a spinner in it and the view follows in an Ajax response, so the scripts are
        // asked for here and the engine downloads while the query is still running rather
        // than after it. The warm path renders the view itself and the stale one renders the
        // previous results, and both carry the scripts with them already; a crawler's
        // placeholder is invisible, renders no head, and so still pays for none of it.
        // A view that turns out empty will have fetched the engine for nothing, which is one
        // download per browser per release: the response is immutably cacheable.
        if (comp instanceof ApiResultComponent) comp.add(new QueryResultNetwork.Resources());
        return comp;
    }

    private QueryResultNetwork buildNetwork(String markupId, ApiResponse response) {
        QueryResultNetwork resultNetwork = new QueryResultNetwork(markupId, queryRef, response, viewDisplay);
        resultNetwork.setPageResource(pageResource);
        resultNetwork.setContextId(contextId);
        ViewActionMappings.addResultActions(resultNetwork, viewDisplay, queryRef, id, contextId, pageResource, refRoot);
        return resultNetwork;
    }

}
