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
 * Builder class for creating QueryResultTable components with various configurations.
 */
public class QueryResultTableBuilder implements Serializable {

    private String markupId;
    private boolean plain = false;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private String id = null;
    private String postPublishTab = null;
    private String refRoot = null;

    private QueryResultTableBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultTableBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display object
     * @return a new QueryResultTableBuilder instance
     */
    public static QueryResultTableBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultTableBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the resource with profile for the QueryResultTable.
     *
     * @param resourceWithProfile the ResourceWithProfile object
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    /**
     * Sets the ID for the QueryResultTable.
     *
     * @param id the ID string
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets whether the table should be plain.
     *
     * @param plain true for plain table, false otherwise
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder plain(boolean plain) {
        this.plain = plain;
        return this;
    }

    /**
     * Sets the context ID for the QueryResultTable.
     *
     * @param contextId the context ID string
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * Sets the tab to return to after publishing via one of the table's action
     * buttons (so the user stays on, e.g., the About tab).
     *
     * @param postPublishTab the tab name, or null for the default
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder postPublishTab(String postPublishTab) {
        this.postPublishTab = postPublishTab;
        return this;
    }

    /**
     * Pins this table to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref. Used
     * on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultTable component based on the configured parameters.
     *
     * @return the constructed Component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        String colClass = " col-" + viewDisplay.getDisplayWidth();
        if (resourceWithProfile != null) {
            if (response != null) {
                QueryResultTable table = new QueryResultTable(markupId, queryRef, response, viewDisplay, false);
                table.setContextId(contextId);
                table.setPostPublishTab(postPublishTab);
                table.setRefRoot(refRoot);
                if (id != null && contextId != null && !id.equals(contextId)) {
                    table.setPartId(id);
                }
                table.setResourceWithProfile(resourceWithProfile);
                table.setPageResource(resourceWithProfile);
                ViewActionMappings.addResultActions(table, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
                table.add(new AttributeAppender("class", colClass));
                return table;
            } else {
                ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultTable table = new QueryResultTable(markupId, queryRef, response, viewDisplay, false);
                        table.setContextId(contextId);
                        table.setPostPublishTab(postPublishTab);
                        table.setRefRoot(refRoot);
                        if (id != null && contextId != null && !id.equals(contextId)) {
                            table.setPartId(id);
                        }
                        table.setResourceWithProfile(resourceWithProfile);
                        table.setPageResource(resourceWithProfile);
                        ViewActionMappings.addResultActions(table, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
                        return table;
                    }
                };
                comp.add(new AttributeAppender("class", colClass));
                return comp;
            }
        } else {
            if (response != null) {
                QueryResultTable table = new QueryResultTable(markupId, queryRef, response, viewDisplay, plain);
                table.setContextId(contextId);
                table.setPostPublishTab(postPublishTab);
                table.setRefRoot(refRoot);
                ViewActionMappings.addResultActions(table, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
                table.add(new AttributeAppender("class", colClass));
                return table;
            } else {
                ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultTable table = new QueryResultTable(markupId, queryRef, response, viewDisplay, plain);
                        table.setContextId(contextId);
                        table.setPostPublishTab(postPublishTab);
                        table.setRefRoot(refRoot);
                        ViewActionMappings.addResultActions(table, viewDisplay, queryRef, id, contextId, resourceWithProfile, refRoot);
                        return table;
                    }
                };
                comp.add(new AttributeAppender("class", colClass));
                return comp;
            }
        }
    }

}