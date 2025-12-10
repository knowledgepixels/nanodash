package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for displaying query results in different formats.
 */
public abstract class QueryResult extends Panel {

    protected final List<AbstractLink> buttons = new ArrayList<>();
    protected String contextId;
    protected boolean finalized = false;
    protected final QueryRef queryRef;
    protected final ViewDisplay viewDisplay;
    protected final ApiResponse response;
    protected ProfiledResource profiledResource;
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

        add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            if (!buttons.isEmpty()) {
                add(new ButtonList("buttons", profiledResource, buttons, null, null));
            } else {
                add(new Label("buttons").setVisible(false));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    /**
     * Set the profiled resource for this component.
     *
     * @param profiledResource The profiled resource to set.
     */
    public void setProfiledResource(ProfiledResource profiledResource) {
        this.profiledResource = profiledResource;
    }

    /**
     * Set the context ID for this component.
     *
     * @param contextId The context ID to set.
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    // TODO button adding method copied and adjusted from ItemListPanel
    // TODO Improve this (member/admin) button handling:
    public void addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        if (contextId != null) {
            parameters.set("context", contextId);
        }
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
    }

    /**
     * Populate the component with the query results.
     */
    protected abstract void populateComponent();

}
