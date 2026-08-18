package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryResultComponentFactory;
import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for displaying query results in different formats.
 */
public abstract class QueryResult extends Panel {

    /**
     * A view-level action, shown as a top entry of the view's dropdown menu.
     */
    public record MenuAction(String label, Class<? extends NanodashPage> pageClass, PageParameters params) implements Serializable {
    }

    protected final List<MenuAction> menuActions = new ArrayList<>();
    protected String contextId;
    protected String partId;
    protected String postPublishTab;
    // The ref (root definition) this view is pinned to (?root=), used to scope per-entry
    // action visibility to the claimant being viewed. Null = the resource's representative
    // ref. See docs/space-ref-identity.md.
    protected String refRoot;
    protected boolean finalized = false;
    protected final QueryRef queryRef;
    protected final ViewDisplay viewDisplay;
    protected final ApiResponse response;
    protected AbstractResourceWithProfile resourceWithProfile;
    protected AbstractResourceWithProfile pageResource;
    protected boolean showViewDisplayMenu = true;
    protected final GrlcQuery grlcQuery;

    /**
     * Constructor for QueryResult.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    public QueryResult(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId);
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
        this.response = response;
        this.grlcQuery = GrlcQuery.get(queryRef);

        // Every view carries an id in the rendered page, so that it can be replaced on its
        // own over Ajax — which is how "refresh now" updates one view without re-rendering
        // the page. Without it the replacement would be written under an id the browser has
        // never seen, leaving the old markup (and its now-removed links) in place.
        setOutputMarkupId(true);

        // The spinner shown beside the title while this view's results are being brought up
        // to date; hidden until someone turns it on (see RefreshingResultPanel). It lives in
        // the title row, right after the title, where the row's own layout keeps it clear of
        // everything — see the .refresh-spinner rules in style.css.
        refreshIndicator = new WebMarkupContainer("refresh-indicator");
        refreshIndicator.setOutputMarkupPlaceholderTag(true);
        refreshIndicator.setVisible(false);
        add(refreshIndicator);
    }

    private final WebMarkupContainer refreshIndicator;

    /**
     * Shows or hides the spinner beside this view's title.
     *
     * @param refreshing true while the view's results are being brought up to date
     */
    public void setRefreshing(boolean refreshing) {
        refreshIndicator.setVisible(refreshing);
    }

    /**
     * The spinner component itself, so a caller that turns it off over Ajax can repaint just
     * that instead of the whole view.
     *
     * @return the refresh indicator
     */
    public Component getRefreshIndicator() {
        return refreshIndicator;
    }

    /**
     * Builds this view again from the current state of the cache, as a component that can
     * take this one's place. Used to refresh a single view where it stands (see the "refresh
     * now" entry of its menu) instead of re-rendering the page around it.
     * <p>
     * With the view's results just marked as outdated, the rebuild comes back as the results
     * that are on screen now plus a spinner, which swaps in the new ones by itself once the
     * query has run.
     *
     * @param markupId the id the replacement must take, i.e. that of the component it replaces
     * @return the replacement component
     */
    public Component rebuild(String markupId) {
        // The part id is only set when it differs from the context; otherwise the view is
        // shown for the context resource itself.
        String id = partId != null ? partId : contextId;
        Component rebuilt = QueryResultComponentFactory.build(markupId, queryRef, viewDisplay,
                resourceWithProfile, id, contextId, refRoot);
        if (rebuilt != null) rebuilt.setOutputMarkupId(true);
        return rebuilt;
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            // View-level actions used to render as a button strip in the header here;
            // they now live as the top entries of the view's dropdown menu instead.
            add(new Label("buttons").setVisible(false));
            if (showViewDisplayMenu) {
                if (viewDisplay.getNanopubId() != null || !menuActions.isEmpty()) {
                    add(new ViewDisplayMenu("np", viewDisplay, queryRef, pageResource, menuActions));
                } else {
                    add(new Label("np").setVisible(false));
                }
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    /**
     * The view-level actions to render as top entries of the view's dropdown menu.
     *
     * @return the collected view-level menu actions
     */
    public List<MenuAction> getMenuActions() {
        return menuActions;
    }

    /**
     * Set the resource with profile for this component.
     *
     * @param resourceWithProfile The resource with profile to set.
     */
    public void setResourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
    }

    public void setPageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
    }

    /**
     * Set the context ID for this component.
     *
     * @param contextId The context ID to set.
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * Set the part ID when this view is shown on a part page (e.g. paper collection).
     * Used for redirect-after-publish to return to the part page.
     *
     * @param partId The part ID to set, or null when on the main context page.
     */
    public void setPartId(String partId) {
        this.partId = partId;
    }

    /**
     * Set the tab to return to after publishing one of this view's action
     * buttons (e.g. {@code "about"} so a space's About-tab views send the user
     * back to About instead of the default Content tab). Null leaves the
     * post-publish redirect on its default tab.
     *
     * @param postPublishTab the tab name, or null for the default
     */
    public void setPostPublishTab(String postPublishTab) {
        this.postPublishTab = postPublishTab;
    }

    /**
     * Set the ref (root definition) this view is pinned to, so per-entry action visibility
     * is gated against that claimant's authority rather than the resource's representative
     * ref. Null = representative ref. See docs/space-ref-identity.md.
     *
     * @param refRoot the ref's root nanopub, or null
     */
    public void setRefRoot(String refRoot) {
        this.refRoot = refRoot;
    }

    /**
     * @return the tab to return to after publishing via an action button, or null for the default
     */
    public String getPostPublishTab() {
        return postPublishTab;
    }

    // A view-level action button; collected here and rendered as a top entry of the
    // view's dropdown menu (see ViewDisplayMenu).
    public void addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        if (contextId != null) {
            parameters.set("context", contextId);
        }
        menuActions.add(new MenuAction(label, pageClass, parameters));
    }

    /**
     * The navigation context to stamp on links in result cells: this view's context
     * resource if bound to one, else the page's navigation context. Only usable at
     * render time (needs the page).
     *
     * @return the context id, or null if neither is set
     */
    private String renderContextId() {
        if (contextId != null) return contextId;
        if (getPage() instanceof NanodashPage nanodashPage) return nanodashPage.getContextId();
        return null;
    }

    /**
     * The {@code &context=...} URL suffix for template/query links in result cells.
     * Empty string when no context is set. Only usable at render time (needs the page).
     *
     * @return the context URL parameter suffix, possibly empty
     */
    protected String templateLinkContextParam() {
        String ctx = renderContextId();
        return ctx == null ? "" : "&context=" + Utils.urlEncode(ctx);
    }

    private static final Pattern INTERNAL_HREF_PATTERN = Pattern.compile("href=\"(/[^\"]*)\"");

    /**
     * Appends the navigation context to app-internal links ({@code href="/..."}) inside
     * sanitized result-cell HTML, so ready-made links coming from the query data itself
     * (e.g. template or query links emitted by the SPARQL) also lead back to the
     * current context. Links already carrying a context are left alone.
     *
     * @param sanitizedHtml the sanitized cell HTML, or null
     * @return the HTML with context-enriched internal links
     */
    protected String withContextInHtmlLinks(String sanitizedHtml) {
        String ctx = renderContextId();
        if (ctx == null || sanitizedHtml == null) return sanitizedHtml;
        String encodedCtx = Utils.urlEncode(ctx);
        Matcher m = INTERNAL_HREF_PATTERN.matcher(sanitizedHtml);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String url = m.group(1);
            String replacement = m.group();
            // The sanitizer escapes "=" as "&#61;", so check both spellings.
            if (!url.contains("context=") && !url.contains("context&#61;")) {
                String separator = url.contains("?") ? "&amp;" : "?";
                replacement = "href=\"" + url + separator + "context=" + encodedCtx + "\"";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Whether all result rows fit on the first page, so no pagination is needed
     * and the filter textfield can be hidden. Also true when the page size is
     * unlimited ({@code < 1}).
     *
     * @return true if all entries fit on the first page
     */
    protected boolean fitsOnFirstPage() {
        int pageSize = viewDisplay.getPageSize();
        return pageSize < 1 || response.getData().size() <= pageSize;
    }

    /**
     * Whether the empty state should point the viewer to the view-level actions:
     * the underlying response (not just a filtered view of it) has no rows, and
     * there is at least one action the viewer is entitled to.
     *
     * @return true if the empty-state call-to-action buttons should show
     */
    protected boolean hasEmptyStateActions() {
        return response.getData().isEmpty() && !menuActions.isEmpty();
    }

    /**
     * Populate the component with the query results.
     */
    protected abstract void populateComponent();

}
