package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.CrawlerRequest;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;

/**
 * A component that retrieves and displays the result of an API call.
 * It uses AjaxLazyLoadPanel to load the content lazily and shows a loading indicator while waiting for the response.
 */
public abstract class ApiResultComponent extends ResultComponent {

    /**
     * The markup id Wicket's lazy-loading panel gives its content.
     */
    private static final String CONTENT_ID = "content";

    private final QueryRef queryRef;
    private ApiResponse response = null;
    private String loadingTitle = null;
    private boolean hideWhenEmpty = false;
    private boolean quietlyGaveUp = false;
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
     * Makes this panel leave the page once its results turn out to be empty, instead of
     * showing the view with its empty note. Set from {@link ViewDisplay#isHideWhenEmpty()}.
     *
     * @param hideWhenEmpty true to hide the panel rather than show an empty view
     */
    private void setHideWhenEmpty(boolean hideWhenEmpty) {
        this.hideWhenEmpty = hideWhenEmpty;
        // The panel hides itself in the middle of its own Ajax update, which needs
        // something to replace in the DOM.
        if (hideWhenEmpty) setOutputMarkupPlaceholderTag(true);
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
        return create(markupId, queryRef, response, loadingTitle, renderer, false);
    }

    /**
     * As {@link #create(String, QueryRef, ApiResponse, String, ApiResultRenderer)}, but for a
     * view that has nothing to say when it finds nothing: with {@code hideWhenEmpty}, an empty
     * result takes the whole panel off the page instead of leaving a titled box with an empty
     * note in it. The hiding happens here rather than inside the view, because in the lazy and
     * refreshing states it is the wrapper that owns the page's {@code <div>} — hiding only the
     * view inside it would leave that div behind, floated and padded by its {@code col-*} class.
     *
     * @param markupId      the markup ID for the component
     * @param queryRef      the query reference the response belongs to
     * @param response      the current results, or null if the cache has none to serve
     * @param loadingTitle  the view's title, shown beside the spinner while waiting for a
     *                      first result; null for views that carry no title
     * @param renderer      builds the component showing a set of results
     * @param hideWhenEmpty whether an empty result should hide the panel altogether
     * @return the component to show
     */
    public static Component create(String markupId, QueryRef queryRef, ApiResponse response, String loadingTitle, ApiResultRenderer renderer, boolean hideWhenEmpty) {
        if (response != null) {
            Component comp = renderer.render(markupId, response);
            // Warm cache: the view itself is the page's div here, so hiding it leaves no
            // markup behind at all.
            if (hideWhenEmpty && hasNothingToShow(comp)) comp.setVisible(false);
            return comp;
        }
        ApiResponse staleResponse = ApiCache.retrieveStaleResponse(queryRef);
        // An outdated empty result is not worth showing when we would hide it anyway, so such
        // a panel waits for its refresh like a cold one instead. That is what keeps a reload —
        // which sends every cached entry down the stale path — from bringing the empty boxes
        // back, while still picking up a fact that arrived since.
        if (staleResponse != null && !(hideWhenEmpty && staleResponse.getData().isEmpty())) {
            return new RefreshingResultPanel(markupId, queryRef, staleResponse, renderer, hideWhenEmpty);
        }
        if (CrawlerRequest.isCrawler()) {
            // Nothing is cached and a crawler does not wait for a poll to fill the section
            // in, so it is left out rather than shown a spinner it will index (issue #804).
            // Less than a visitor sees, never more: both are told there is nothing here yet.
            return new WebMarkupContainer(markupId).setVisible(false);
        }
        ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
            @Override
            public Component getApiResultComponent(String id, ApiResponse r) {
                return renderer.render(id, r);
            }
        };
        comp.setLoadingTitle(loadingTitle);
        comp.setHideWhenEmpty(hideWhenEmpty);
        return comp;
    }

    /**
     * Whether a rendered result component turned out to have nothing to put on the page.
     * Anything that is not a view — a loading placeholder, an error label — counts as having
     * something to show, so a panel is never hidden before its results are in.
     *
     * @param component the component holding a view's results, or standing in for them
     * @return true if it is a view with neither a row nor an action to show
     */
    static boolean hasNothingToShow(Component component) {
        return component instanceof QueryResult view && view.hasNothingToShow();
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
        if (queryFailed || response == null) {
            if (!queryFailed) logger.error("Timed out waiting for API response for {}", queryRef);
            if (hideWhenEmpty) {
                // A secondary panel that would have gone quiet anyway has nothing to gain
                // from an error box of its own.
                quietlyGaveUp = true;
                return new WebMarkupContainer(markupId).setVisible(false);
            }
            String message = queryFailed ? "API call failed." : "Loading timed out. Please reload the page.";
            return new Label(markupId, "<span class=\"negative\">" + message + "</span>").setEscapeModelStrings(false);
        }
        return getApiResultComponent(markupId, response);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Takes the panel off the page once its results are in and turn out empty. One-way
     * only: a panel the page itself hid stays hidden. While the results are still on their
     * way the content is the loading placeholder, which never counts as empty, so the panel
     * stays visible and the page's timer keeps polling it.
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (hideWhenEmpty && (quietlyGaveUp || hasNothingToShow(get(CONTENT_ID)))) setVisible(false);
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
