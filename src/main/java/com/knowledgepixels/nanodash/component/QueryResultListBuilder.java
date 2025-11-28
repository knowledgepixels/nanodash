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
 * Builder class for creating QueryResultList components.
 */
public class QueryResultListBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private Space space = null;
    private String id = null;

    private QueryResultListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        this.queryRef = queryRef;
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultListBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultListBuilder instance
     */
    public static QueryResultListBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultListBuilder(markupId, queryRef, viewDisplay);
    }

    public QueryResultListBuilder space(Space space) {
        this.space = space;
        return this;
    }

    public QueryResultListBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultListBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Builds the QueryResultList component.
     *
     * @return the QueryResultList component
     */
    public Component build() {
        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        ApiResponse response = ApiCache.retrieveResponse(queryRef);
        if (space != null) {
            if (response != null) {
                QueryResultList resultList = new QueryResultList(markupId, grlcQuery, response, viewDisplay);
                resultList.setSpace(space);
                resultList.setContextId(contextId);
                ResourceView view = viewDisplay.getView();
                if (view != null) {
                    for (IRI actionIri : view.getViewResultActionList()) {
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
                        String queryMapping = view.getTemplateQueryMapping(actionIri);
                        if (queryMapping != null && queryMapping.contains(":")) {
                            params.set("values-from-query", queryRef.getAsUrlString());
                            params.set("values-from-query-mapping", queryMapping);
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        resultList.addButton(label, PublishPage.class, params);
                    }
                }
                return resultList;
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultList resultList = new QueryResultList(markupId, grlcQuery, response, viewDisplay);
                        resultList.setSpace(space);
                        resultList.setContextId(contextId);
                        ResourceView view = viewDisplay.getView();
                        if (view != null) {
                            for (IRI actionIri : view.getViewResultActionList()) {
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
                                String queryMapping = view.getTemplateQueryMapping(actionIri);
                                if (queryMapping != null && queryMapping.contains(":")) {
                                    params.set("values-from-query", queryRef.getAsUrlString());
                                    params.set("values-from-query-mapping", queryMapping);
                                }
                                params.set("refresh-upon-publish", queryRef.getAsUrlString());
                                resultList.addButton(label, PublishPage.class, params);
                            }
                        }
                        return resultList;
                    }
                };
            }
        } else {
            if (response != null) {
                QueryResultList resultList = new QueryResultList(markupId, grlcQuery, response, viewDisplay);
                resultList.setContextId(contextId);
                return resultList;
            } else {
                return new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultList resultList = new QueryResultList(markupId, grlcQuery, response, viewDisplay);
                        resultList.setContextId(contextId);
                        return resultList;
                    }
                };
            }
        }
    }

}
