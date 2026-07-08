package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
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
 * Builder class for creating QueryResultList components.
 */
public class QueryResultListBuilder implements Serializable {

    private String markupId;
    private ViewDisplay viewDisplay;
    private String contextId = null;
    private QueryRef queryRef;
    private AbstractResourceWithProfile resourceWithProfile = null;
    private String id = null;
    private AbstractResourceWithProfile pageResource = null;
    private String postPublishTab = null;
    private String refRoot = null;

    private QueryResultListBuilder(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        this.markupId = markupId;
        // Bind session-derived "magic" query parameters here on the request thread
        // (ApiCache fetches on background threads where the session is absent).
        this.queryRef = com.knowledgepixels.nanodash.MagicQueryParams.augment(queryRef);
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

    public QueryResultListBuilder resourceWithProfile(AbstractResourceWithProfile resourceWithProfile) {
        this.resourceWithProfile = resourceWithProfile;
        return this;
    }

    /**
     * Sets the context ID for the QueryResultList.
     *
     * @param contextId the context ID
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder contextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public QueryResultListBuilder id(String id) {
        this.id = id;
        return this;
    }

    public QueryResultListBuilder pageResource(AbstractResourceWithProfile pageResource) {
        this.pageResource = pageResource;
        return this;
    }

    /**
     * Sets the tab to return to after publishing one of this view's action
     * buttons (e.g. {@code "about"}). Null leaves the post-publish redirect on
     * its default tab.
     *
     * @param postPublishTab the tab name, or null for the default
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder postPublishTab(String postPublishTab) {
        this.postPublishTab = postPublishTab;
        return this;
    }

    /**
     * Pins this list to a specific ref (root definition), so action visibility is gated
     * against that claimant's authority rather than the resource's representative ref. Used
     * on {@code ?root=}-pinned pages. Null leaves it on the representative ref.
     *
     * @param refRoot the ref's root nanopub, or null
     * @return the current QueryResultListBuilder instance
     */
    public QueryResultListBuilder refRoot(String refRoot) {
        this.refRoot = refRoot;
        return this;
    }

    /**
     * Builds the QueryResultList component.
     *
     * @return the QueryResultList component
     */
    public Component build() {
        ApiResponse response = ApiCache.retrieveResponseAsync(queryRef);
        String colClass = " col-" + viewDisplay.getDisplayWidth();
        if (resourceWithProfile != null) {
            if (response != null) {
                QueryResultList resultList = new QueryResultList(markupId, queryRef, response, viewDisplay);
                resultList.setResourceWithProfile(resourceWithProfile);
                resultList.setPageResource(pageResource);
                resultList.setContextId(contextId);
                resultList.setPostPublishTab(postPublishTab);
                resultList.setRefRoot(refRoot);
                View view = viewDisplay.getView();
                if (view != null) {
                    for (IRI actionIri : view.getViewResultActionList()) {
                        if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile, refRoot)) continue;
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
                        if (partField != null && contextId != null) {
                            // The part field pre-fills a namespaced child IRI (the user fills the suffix).
                            // TODO Find a better way to pass the MaintainedResource object to this method:
                            MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
                            String namespace = null;
                            if (r != null) {
                                namespace = r.getNamespace();
                            } else if (resourceWithProfile instanceof Space) {
                                // The Space-creation templates' `space` placeholder has a fixed
                                // `https://w3id.org/spaces/` prefix, so the pre-fill is relative to it.
                                // Nesting the new space's IRI under this space's path makes it a
                                // sub-space via the prefix match.
                                namespace = contextId.replaceFirst("https://w3id.org/spaces/", "") + "/";
                            }
                            if (namespace != null) {
                                params.set("param_" + partField, namespace + "<SET-SUFFIX>");
                            }
                        }
                        String queryMapping = view.getTemplateQueryMapping(actionIri);
                        if (queryMapping != null && queryMapping.contains(":")) {
                            params.set("values-from-query", queryRef.getAsUrlString());
                            params.set("values-from-query-mapping", queryMapping);
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        if (postPublishTab != null) params.set("postpub-tab", postPublishTab);
                        resultList.addButton(label, PublishPage.class, params);
                    }
                }
                resultList.add(new AttributeAppender("class", colClass));
                return resultList;
            } else {
                ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultList resultList = new QueryResultList(markupId, queryRef, response, viewDisplay);
                        resultList.setResourceWithProfile(resourceWithProfile);
                        resultList.setPageResource(pageResource);
                        resultList.setContextId(contextId);
                        resultList.setPostPublishTab(postPublishTab);
                        resultList.setRefRoot(refRoot);
                        View view = viewDisplay.getView();
                        if (view != null) {
                            for (IRI actionIri : view.getViewResultActionList()) {
                                if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile, refRoot)) continue;
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
                                if (partField != null && contextId != null) {
                                    // The part field pre-fills a namespaced child IRI (the user fills the suffix).
                                    // TODO Find a better way to pass the MaintainedResource object to this method:
                                    MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
                                    String namespace = null;
                                    if (r != null) {
                                        namespace = r.getNamespace();
                                    } else if (resourceWithProfile instanceof Space) {
                                        // The Space-creation templates' `space` placeholder has a fixed
                                        // `https://w3id.org/spaces/` prefix, so the pre-fill is relative to it.
                                        // Nesting the new space's IRI under this space's path makes it a
                                        // sub-space via the prefix match.
                                        namespace = contextId.replaceFirst("https://w3id.org/spaces/", "") + "/";
                                    }
                                    if (namespace != null) {
                                        params.set("param_" + partField, namespace + "<SET-SUFFIX>");
                                    }
                                }
                                String queryMapping = view.getTemplateQueryMapping(actionIri);
                                if (queryMapping != null && queryMapping.contains(":")) {
                                    params.set("values-from-query", queryRef.getAsUrlString());
                                    params.set("values-from-query-mapping", queryMapping);
                                }
                                params.set("refresh-upon-publish", queryRef.getAsUrlString());
                                if (postPublishTab != null) params.set("postpub-tab", postPublishTab);
                                resultList.addButton(label, PublishPage.class, params);
                            }
                        }
                        return resultList;
                    }
                };
                comp.add(new AttributeAppender("class", colClass));
                return comp;
            }
        } else {
            if (response != null) {
                QueryResultList resultList = new QueryResultList(markupId, queryRef, response, viewDisplay);
                resultList.setPageResource(pageResource);
                resultList.setContextId(contextId);
                ViewActionMappings.addResultActions(resultList, viewDisplay, queryRef, id, contextId, null, refRoot);
                resultList.add(new AttributeAppender("class", colClass));
                return resultList;
            } else {
                ApiResultComponent comp = new ApiResultComponent(markupId, queryRef) {
                    @Override
                    public Component getApiResultComponent(String markupId, ApiResponse response) {
                        QueryResultList resultList = new QueryResultList(markupId, queryRef, response, viewDisplay);
                        resultList.setPageResource(pageResource);
                        resultList.setContextId(contextId);
                        ViewActionMappings.addResultActions(resultList, viewDisplay, queryRef, id, contextId, null, refRoot);
                        return resultList;
                    }
                };
                comp.add(new AttributeAppender("class", colClass));
                return comp;
            }
        }
    }

}
