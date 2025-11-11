package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultTable components with various configurations.
 */
public class QueryResultTableBuilder implements Serializable {

    private String markupId;
    private boolean plain = false;
    private ViewDisplay viewDisplay = null;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    private QueryResultTableBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultTableBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display configuration
     * @return a new QueryResultTableBuilder instance
     */
    public static QueryResultTableBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultTableBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the space for the QueryResultTable.
     *
     * @param space the Space object
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder space(Space space) {
        this.space = space;
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
     * Builds the QueryResultTable component based on the configured parameters.
     *
     * @return the constructed Component
     */
    public Component build() {
        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        ApiResponse response = ApiCache.retrieveResponse(queryRef);
        if (!plain) {
            if (response != null) {
                return createComponent(grlcQuery, response);
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        return createComponent(grlcQuery, response);
                    }
                };
            }
        } else {
            if (response != null) {
                return new QueryResultTable(markupId, grlcQuery, response, plain, viewDisplay, contextId);
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        return new QueryResultTable(markupId, grlcQuery, response, plain, viewDisplay, contextId);
                    }
                };
            }
        }
    }

    private Component createComponent(GrlcQuery grlcQuery, ApiResponse response) {
        QueryResultTable table = new QueryResultTable(markupId, grlcQuery, response, false, viewDisplay, contextId);
        table.setContext(contextId, space);
        ResourceView view = viewDisplay.getView();
        if (view != null) {
            for (IRI actionIri : view.getActionList()) {
                Template t = view.getTemplateForAction(actionIri);
                if (t == null) continue;
                String targetField = view.getTemplateTargetFieldForAction(actionIri);
                if (targetField == null) targetField = "resource";
                String label = view.getLabelForAction(actionIri);
                if (label == null) label = "action...";
                PageParameters params = new PageParameters().set("template", t.getId()).set("param_" + targetField, id).set("context", contextId);
                String partField = view.getTemplatePartFieldForAction(actionIri);
                if (partField != null) {
                    // TODO Find a better way to pass the MaintainedResource object to this method:
                    MaintainedResource r = MaintainedResource.get(contextId);
                    if (r != null && r.getNamespace() != null) {
                        params.set("param_" + partField, r.getNamespace() + "<SET-SUFFIX>");
                    }
                }
                table.addButton(label, PublishPage.class, params);
            }
        }
        return table;
    }
}
