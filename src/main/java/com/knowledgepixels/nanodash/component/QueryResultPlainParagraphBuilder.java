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
 * Builder class for creating QueryResultPlainParagraph components.
 */
public class QueryResultPlainParagraphBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    // This method is the result of refactoring and copying code from other classes done
    // by Cursor. This should in general be aligned and refactored more with the other classes.
    private void addResultButtons(QueryResultPlainParagraph resultPlainParagraph) {
        ResourceView view = viewDisplay.getView();
        if (view == null) return;
        for (IRI actionIri : view.getViewResultActionList()) {
            Template t = view.getTemplateForAction(actionIri);
            if (t == null) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            PageParameters params = new PageParameters().set("template", t.getId())
                    .set("param_" + targetField, id)
                    .set("context", contextId)
                    .set("template-version", "latest");
            if (id != null && contextId != null && !id.equals(contextId)) {
                params.set("part", id);
            }
            String partField = view.getTemplatePartFieldForAction(actionIri);
            if (partField != null) {
                // TODO Find a better way to pass the MaintainedResource object to this method:
                MaintainedResource r = MaintainedResource.get(contextId);
                if (r != null && r.getNamespace() != null) {
                    params.set("param_" + partField, r.getNamespace() + "<SET-SUFFIX>");
                }
            }
            String queryMapping = view.getTemplateQueryMapping(actionIri);
            if (queryMapping != null && queryMapping.contains(":")) {
                params.set("values-from-query", queryRef.getAsUrlString());
                params.set("values-from-query-mapping", queryMapping);
            }
            params.set("refresh-upon-publish", queryRef.getAsUrlString());
            resultPlainParagraph.addButton(label, PublishPage.class, params);
        }
    }

    private QueryResultPlainParagraphBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultPlainParagraphBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultPlainParagraphBuilder instance
     */
    public static QueryResultPlainParagraphBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultPlainParagraphBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the context ID for the QueryResultPlainParagraph.
     *
     * @param contextId the context ID
     * @return the current QueryResultPlainParagraphBuilder instance
     */
    public QueryResultPlainParagraphBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultPlainParagraphBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Builds the QueryResultPlainParagraph component.
     *
     * @return the QueryResultPlainParagraph component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        if (space != null) {
            if (response != null) {
                QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                resultPlainParagraph.setProfiledResource(space);
                resultPlainParagraph.setContextId(contextId);
                addResultButtons(resultPlainParagraph);
                return resultPlainParagraph;
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                        resultPlainParagraph.setProfiledResource(space);
                        resultPlainParagraph.setContextId(contextId);
                        addResultButtons(resultPlainParagraph);
                        return resultPlainParagraph;
                    }
                };
            }
        } else {
            if (response != null) {
                QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                resultPlainParagraph.setContextId(contextId);
                addResultButtons(resultPlainParagraph);
                return resultPlainParagraph;
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultPlainParagraph resultPlainParagraph = new QueryResultPlainParagraph(markupId, queryRef, response, viewDisplay);
                        resultPlainParagraph.setContextId(contextId);
                        addResultButtons(resultPlainParagraph);
                        return resultPlainParagraph;
                    }
                };
            }
        }
    }

}
