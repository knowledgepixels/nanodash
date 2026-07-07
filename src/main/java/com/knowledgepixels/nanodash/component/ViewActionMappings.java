package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies a view entry action's per-row query mappings and decides whether the
 * action's button should render for that row. See docs/magic-query-params.md
 * (phase 2: empty-into-required visibility + multiple mappings / non-{@code param_}
 * targets).
 */
class ViewActionMappings {

    private ViewActionMappings() {
    }

    /**
     * Writes the action's mapped values into the link parameters and returns whether
     * the button should be shown for this row.
     *
     * <p>Each mapping is {@code "col:target"}: the row's value for result column
     * {@code col} is written to URL parameter {@code param_target}, or — when
     * {@code target} starts with {@code @} — to the raw URL key {@code target}
     * (fill-mode keys such as {@code @derive-a} / {@code @supersede}). The button is
     * <b>hidden</b> (returns false) if any <i>required</i> mapped value is empty: a
     * raw key is always required; a {@code param_} target is required unless its
     * template placeholder is optional. Empty values for optional placeholders are
     * simply skipped (button kept).</p>
     *
     * @param view      the view declaring the action
     * @param actionIri the action node IRI
     * @param row       the result row
     * @param params    the link parameters to populate
     * @return true if the action button should be rendered for this row
     */
    /**
     * Adds a button to the result component for each result action declared by the view,
     * linking to the action's template on the publish page. Resource-context parameters
     * (the target field, the context, the part, and the part field) are only set when the
     * corresponding id/contextId is available, so this also works on resource-less pages
     * such as the general Spaces page or the standalone view-results page.
     *
     * @param result              the result component to add the action buttons to
     * @param viewDisplay         the view display whose view declares the actions
     * @param queryRef            the query reference backing the component (used for refresh and query mapping)
     * @param id                  the resource id, or null if there is no specific resource in context
     * @param contextId           the context id, or null if there is no context
     * @param resourceWithProfile the resource whose page the component is on, or null
     * @param refRoot             the pinned ref's root nanopub, or null
     */
    static void addResultActions(QueryResult result, ViewDisplay viewDisplay, QueryRef queryRef, String id, String contextId, AbstractResourceWithProfile resourceWithProfile, String refRoot) {
        View view = viewDisplay.getView();
        if (view == null) return;
        for (IRI actionIri : view.getViewResultActionList()) {
            // Per-action role gating (docs/role-specific-views.md): skip an action
            // whose gen:isVisibleTo the current viewer does not satisfy. Additive —
            // actions without gen:isVisibleTo are unaffected. Gated against the pinned
            // ref's authority on a ?root=-pinned page. See docs/space-ref-identity.md.
            if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile, refRoot)) continue;
            Template t = view.getTemplateForAction(actionIri);
            if (t == null) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            if (!label.endsWith("...")) label += "...";
            PageParameters params = new PageParameters().set("template", t.getId())
                    .set("template-version", "latest");
            if (id != null) params.set("param_" + targetField, id);
            if (contextId != null) params.set("context", contextId);
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
            if (result.getPostPublishTab() != null) params.set("postpub-tab", result.getPostPublishTab());
            result.addButton(label, PublishPage.class, params);
        }
    }

    /**
     * Builds the entry-action links of a view for one result row: one publish-page link
     * per {@code gen:ViewEntryAction} the viewer is entitled to and whose query mappings
     * are satisfied by the row (see {@link #applyEntryMappings}). Each link uses the
     * markup id {@code "link"}, ready for an {@link com.knowledgepixels.nanodash.component.menu.EntryActionMenu}.
     *
     * @param view                the view declaring the actions, or null for none
     * @param row                 the result row the actions apply to
     * @param queryRef            the query reference backing the component (used for refresh and query mapping)
     * @param entitlementResource the resource the viewer's entitlement is checked against, or null
     * @param contextId           the context id, or null if there is no context
     * @param partId              the part id when shown on a part page, or null
     * @param refRoot             the pinned ref's root nanopub, or null
     * @param postPublishTab      the tab to return to after publishing, or null for the default
     * @return the entry-action links for this row (never null)
     */
    static List<AbstractLink> buildEntryActionLinks(View view, ApiResponseEntry row, QueryRef queryRef,
            AbstractResourceWithProfile entitlementResource, String contextId, String partId, String refRoot, String postPublishTab) {
        List<AbstractLink> links = new ArrayList<>();
        if (view == null) return links;
        for (IRI actionIri : view.getViewEntryActionList()) {
            // Per-action role gating (docs/role-specific-views.md): skip an action
            // whose gen:isVisibleTo the viewer does not satisfy.
            if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), entitlementResource, refRoot)) continue;
            Template t = view.getTemplateForAction(actionIri);
            if (t == null) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            if (!label.endsWith("...")) label += "...";
            PageParameters params = new PageParameters().set("template", t.getId())
                    .set("param_" + targetField, contextId)
                    .set("context", contextId)
                    .set("template-version", "latest");
            if (partId != null && contextId != null && !partId.equals(contextId)) {
                params.set("part", partId);
            }
            String partField = view.getTemplatePartFieldForAction(actionIri);
            if (partField != null) {
                // The part field pre-fills a namespaced child IRI (the user fills the suffix).
                // TODO Find a better way to pass the MaintainedResource object to this method:
                MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
                if (r != null && r.getNamespace() != null) {
                    params.set("param_" + partField, r.getNamespace() + "<SET-SUFFIX>");
                }
            }
            // Apply the action's query mappings; hide the button for this row
            // if any required mapped value is empty (docs/magic-query-params.md).
            if (!applyEntryMappings(view, actionIri, row, params)) {
                continue;
            }
            params.set("refresh-upon-publish", queryRef.getAsUrlString());
            if (postPublishTab != null) params.set("postpub-tab", postPublishTab);
            AbstractLink button = new BookmarkablePageLink<NanodashPage>("link", PublishPage.class, params);
            // A label that starts with a leading symbol/emoji renders that as the entry icon.
            String iconBody = Utils.menuEntryIconBodyHtml(label);
            if (iconBody != null) {
                button.setBody(Model.of(iconBody)).setEscapeModelStrings(false);
            } else {
                button.setBody(Model.of(label));
            }
            links.add(button);
        }
        return links;
    }

    static boolean applyEntryMappings(View view, IRI actionIri, ApiResponseEntry row, PageParameters params) {
        Template template = view.getTemplateForAction(actionIri);
        for (String mapping : view.getTemplateQueryMappings(actionIri)) {
            int sep = mapping.indexOf(':');
            if (sep < 0) continue;
            String col = mapping.substring(0, sep);
            String target = mapping.substring(sep + 1);
            boolean rawKey = target.startsWith("@");
            String key = rawKey ? target.substring(1) : target;
            String value = row.get(col);
            if (value == null || value.isBlank()) {
                // Empty: hide the action only if the target is required.
                if (rawKey || template == null || template.isRequiredField(key)) return false;
                continue;
            }
            params.set(rawKey ? key : "param_" + key, value);
        }
        return true;
    }

}
