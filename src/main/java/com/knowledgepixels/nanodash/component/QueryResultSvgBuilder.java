package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.io.Serializable;

/**
 * Builder class for creating QueryResultSvg components.
 */
public class QueryResultSvgBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String refRoot = null;

    private void addResultButtons(QueryResultSvg resultSvg) {
        View view = viewDisplay.getView();
        if (view == null) return;
        for (IRI actionIri : view.getViewResultActionList()) {
            // Per-action role gating (docs/role-specific-views.md): skip an action
            // whose gen:isVisibleTo the viewer does not satisfy.
            if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), pageResource, refRoot)) continue;
            Template t = view.getTemplateForAction(actionIri);
            if (t == null) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            if (!label.endsWith("...")) label += "...";
            PageParameters params = new PageParameters().set("template", t.getId())
                    .set("param_" + targetField, id)
                    .set("context", contextId)
                    .set("template-version", "latest");
            if (id != null && contextId != null && !id.equals(contextId)) {
                params.set("part", id);
            }
            String partField = view.getTemplatePartFieldForAction(actionIri);
            if (partField != null) {
                MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
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
            resultSvg.addButton(label, PublishPage.class, params);
        }
    }

    private QueryResultSvgBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
        this.viewDisplay = viewDisplay;
    }

    /**
     * Creates a new QueryResultSvgBuilder instance.
     *
     * @param markupId    the markup ID for the component
     * @param queryRef    the query reference
     * @param viewDisplay the view display
     * @return a new QueryResultSvgBuilder instance
     */
    public static QueryResultSvgBuilder create(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        return new QueryResultSvgBuilder(markupId, queryRef, viewDisplay);
    }

    /**
     * Sets the context ID for the QueryResultSvg.
     *
     * @param contextId the context ID
     * @return the current QueryResultSvgBuilder instance
     */
    public QueryResultSvgBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultSvgBuilder id(String id) {
        this.id = id;
        return this;
    }

    public QueryResultSvgBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Pins this view to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref.
     * Used on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultSvgBuilder instance
     */
    public QueryResultSvgBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultSvg component.
     *
     * @return the QueryResultSvg component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        Component comp = ApiResultComponent.create(markupId, queryRef, response, this::buildSvg);
        comp.add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));
        return comp;
    }

    private QueryResultSvg buildSvg(String markupId, ApiResponse response) {
        QueryResultSvg resultSvg = new QueryResultSvg(markupId, queryRef, response, viewDisplay);
        resultSvg.setPageResource(pageResource);
        resultSvg.setContextId(contextId);
        addResultButtons(resultSvg);
        return resultSvg;
    }

}
