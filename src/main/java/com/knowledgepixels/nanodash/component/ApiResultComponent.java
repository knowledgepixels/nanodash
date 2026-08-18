package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;

/**
 * A component that retrieves and displays the result of an API call.
 * It uses AjaxLazyLoadPanel to load the content lazily and shows a loading indicator while waiting for the response.
 */
public abstract class ApiResultComponent extends ResultComponent {

    private final QueryRef queryRef;
    private ApiResponse response = null;
    private String loadingTitle = null;
    private static final Logger logger = LoggerFactory.getLogger(ApiResultComponent.class);

    /**
     * Constructor for ApiResultComponent using a QueryRef object.
     *
     * @param id       the component id
     * @param queryRef the QueryRef object containing the query name and parameters
     */
    public ApiResultComponent(String id, QueryRef queryRef) {
        super(id);
        this.queryRef = queryRef;
    }

    /**
     * Sets the title to show while waiting, so the loading state looks like the loaded one
     * with the spinner beside its title (see {@link LoadingResultPanel}). Without one, only
     * the spinner is shown.
     *
     * @param loadingTitle the view's title, or null
     */
    public final void setLoadingTitle(String loadingTitle) {
        this.loadingTitle = loadingTitle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component getDefaultLoadingComponent(String id) {
        if (loadingTitle == null || loadingTitle.isBlank()) {
            return super.getDefaultLoadingComponent(id);
        }
        return new LoadingResultPanel(id, loadingTitle);
    }

    /**
     * Builds the component for a query's results in whichever state its cache is in: the
     * results themselves when they are current, the previous ones plus a spinner while a
     * refresh runs (see {@link RefreshingResultPanel}), and the title plus a spinner when
     * nothing is cached yet. The returned component is the one to style and add, whichever
     * state it is.
     *
     * @param markupId     the markup ID for the component
     * @param queryRef     the query reference the response belongs to
     * @param response     the current results, or null if the cache has none to serve
     * @param loadingTitle the view's title, shown beside the spinner while waiting for a
     *                     first result; null for views that carry no title
     * @param renderer     builds the component showing a set of results
     * @return the component to show
     */
    public static Component create(String markupId, QueryRef queryRef, ApiResponse response, String loadingTitle, ApiResultRenderer renderer) {
        if (response != null) {
            return renderer.render(markupId, response);
        }
        ApiResponse staleResponse = ApiCache.retrieveStaleResponse(queryRef);
        if (staleResponse != null) {
            return new RefreshingResultPanel(markupId, queryRef, staleResponse, renderer);
        }
        ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
            @Override
            public Component getApiResultComponent(String id, ApiResponse r) {
                return renderer.render(id, r);
            }
        };
        comp.setLoadingTitle(loadingTitle);
        return comp;
    }

    // How long to keep waiting for a first result before giving up on it.
    private static final long LAZY_LOAD_TIMEOUT_MS = 60_000;

    private final long deadline = System.currentTimeMillis() + LAZY_LOAD_TIMEOUT_MS;
    private boolean queryFailed = false;

    /**
     * {@inheritDoc}
     * <p>
     * Only builds the component; the waiting is done by the polling in
     * {@link #isContentReady()}, which returns true exactly when there is something to build
     * from — or to report.
     */
    @Override
    public Component getLazyLoadComponent(String markupId) {
        if (queryFailed) {
            return new Label(markupId, "<span class=\"negative\">API call failed.</span>").setEscapeModelStrings(false);
        }
        if (response == null) {
            logger.error("Timed out waiting for API response for {}", queryRef);
            return new Label(markupId, "<span class=\"negative\">Loading timed out. Please reload the page.</span>").setEscapeModelStrings(false);
        }
        return getApiResultComponent(markupId, response);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Asks the cache — which answers without going to the network — and reports "ready" once
     * it has the results, the query has failed, or the wait has gone on long enough. The
     * panel's timer calls this about once a second, so waiting costs nothing while it lasts:
     * this used to be a sleep loop inside {@link #getLazyLoadComponent(String)}, which held a
     * request thread for up to a minute per view still loading.
     */
    @Override
    protected boolean isContentReady() {
        if (response != null || queryFailed) return true;
        try {
            response = ApiCache.retrieveResponseAsync(queryRef);
        } catch (Exception ex) {
            logger.error("Query failed for {}: {}", queryRef.getAsUrlString(), ex.getMessage());
            queryFailed = true;
            return true;
        }
        return response != null || System.currentTimeMillis() > deadline;
    }

    /**
     * Abstract method to be implemented by subclasses to provide the component that displays the API result.
     *
     * @param markupId the markup ID for the component
     * @param response the API response to display
     * @return a Component that displays the API result
     */
    // TODO Use lambda instead of abstract method?
    public abstract Component getApiResultComponent(String markupId, ApiResponse response);

}
