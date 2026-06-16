package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
