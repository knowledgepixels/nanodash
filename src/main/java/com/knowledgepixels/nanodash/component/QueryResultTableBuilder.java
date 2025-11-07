package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

/**
 * Builder class for creating QueryResultTable components with various configurations.
 */
public class QueryResultTableBuilder {

    private String markupId;
    private long rowsPerPage;
    private boolean plain = false;
    private ViewDisplay viewDisplay = null;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    private QueryResultTableBuilder(String markupId, QueryRef queryRef, long rowsPerPage) {
        this.markupId = markupId;
        this.rowsPerPage = rowsPerPage;
        this.queryRef = queryRef;
    }

    /**
     * Creates a new QueryResultTableBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param rowsPerPage the number of rows per page
     * @return a new QueryResultTableBuilder instance
     */
    public static QueryResultTableBuilder create(String markupId, QueryRef queryRef, long rowsPerPage) {
        return new QueryResultTableBuilder(markupId, queryRef, rowsPerPage);
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
     * Sets the ViewDisplay for the QueryResultTable.
     *
     * @param viewDisplay the ViewDisplay object
     * @return the current QueryResultTableBuilder instance
     */
    public QueryResultTableBuilder viewDisplay(ViewDisplay viewDisplay) {
        this.viewDisplay = viewDisplay;
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
        if (space == null) {
            if (response != null) {
                ResourceView view = viewDisplay.getView();
                QueryResultTable table = new QueryResultTable(markupId, grlcQuery, response, false, viewDisplay, rowsPerPage, contextId);
                table.setContext(contextId, space);
                for (IRI actionIri : view.getActionList()) {
                    Template t = view.getTemplateForAction(actionIri);
                    if (t == null) continue;
                    String field = view.getTemplateFieldForAction(actionIri);
                    if (field == null) field = "resource";
                    String label = view.getLabelForAction(actionIri);
                    if (label == null) label = "action...";
                    PageParameters params = new PageParameters().set("template", t.getId()).set("param_" + field, id).set("context", contextId);
                    table.addButton(label, PublishPage.class, params);
                }
                return table;
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        ResourceView view = viewDisplay.getView();
                        QueryResultTable table = new QueryResultTable(markupId, grlcQuery, response, false, viewDisplay, rowsPerPage, contextId);
                        table.setContext(contextId, space);
                        for (IRI actionIri : view.getActionList()) {
                            Template t = view.getTemplateForAction(actionIri);
                            if (t == null) continue;
                            String field = view.getTemplateFieldForAction(actionIri);
                            if (field == null) field = "resource";
                            String label = view.getLabelForAction(actionIri);
                            if (label == null) label = "action...";
                            PageParameters params = new PageParameters().set("template", t.getId()).set("param_" + field, id).set("context", contextId);
                            table.addButton(label, PublishPage.class, params);
                        }
                        return table;
                    }
                };
            }
        } else {
            if (response != null) {
                return new QueryResultTable(markupId, grlcQuery, response, plain, viewDisplay, rowsPerPage, contextId);
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        return new QueryResultTable(markupId, grlcQuery, response, plain, viewDisplay, rowsPerPage, contextId);
                    }
                };
            }
        }
    }

}
