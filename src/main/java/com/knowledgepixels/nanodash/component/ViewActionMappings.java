package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
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
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Applies a view entry action's per-row query mappings and decides whether the
 * action's button should render for that row. See docs/magic-query-params.md
 * (phase 2: empty-into-required visibility + multiple mappings / non-{@code param_}
 * targets).
 */
class ViewActionMappings {

    private static final Logger logger = LoggerFactory.getLogger(ViewActionMappings.class);

    /**
     * Page source naming the nanopublication the view's query is bound to: the value the
     * page filled into the query's {@code <queryField>Np} parameter, i.e. the nanopub whose
     * content the view is showing. Mapped to {@code @override} it opens that nanopub in the
     * publish form (docs/fill-modes.md).
     */
    static final String SOURCE_NP = "@sourceNp";

    /**
     * Page source naming the template {@link #SOURCE_NP} was created from, read from its
     * {@code nt:wasCreatedFromTemplate}. A view showing nanopubs of more than one template
     * maps this to {@code @template} so the form opens the one that actually made the
     * source, rather than the action's declared fallback.
     */
    static final String SOURCE_NP_TEMPLATE = "@sourceNpTemplate";

    /**
     * Page-source prefix naming a column of the view's <em>own result</em>:
     * {@code @result.<column>}. Its value is that column's, taken once for the view rather
     * than per row, so a result action (which has no row) can carry it — the column must
     * therefore hold one and the same value in every row. This is what lets the <em>query</em>
     * decide which nanopub an action acts on: it resolves the nanopub, returns it as a column,
     * and the action reads it back. On an entry action there is no need for it — map the row's
     * own column instead.
     */
    static final String RESULT_PREFIX = "@result.";

    /**
     * The raw publish-URL keys that put the form in a fill mode, i.e. make it take its
     * values from a source nanopub ({@link com.knowledgepixels.nanodash.component.PublishForm.FillMode}).
     * An action mapping one of these gets no target-field parameter: the source supplies
     * every field, and passing the page's own resource on top would overwrite the filled
     * value of a field that happens to carry the target field's name.
     */
    private static final Set<String> FILL_MODE_KEYS = Set.of(
            "use", "use-a", "partial", "partial-a", "supersede", "supersede-a",
            "derive", "derive-a", "override", "override-a", "fill", "fill-all");

    private ViewActionMappings() {
    }

    /**
     * Resolves a page source — a mapping whose {@code column} begins with {@code @} — to the
     * value the page supplies for it. Unlike a result column this is one value for the whole
     * view, so it is available to result actions (which have no row) as well as entry ones.
     *
     * @param source    the page-source name, including its {@code @}
     * @param view      the view declaring the action
     * @param queryRef  the query reference the view is rendered from, or null
     * @param response  the view's own results, or null where they are not in hand
     * @return the value, or null if the page cannot supply it
     */
    private static String resolvePageSource(String source, View view, QueryRef queryRef, ApiResponse response) {
        if (source.startsWith(RESULT_PREFIX)) {
            return singleResultValue(response, source.substring(RESULT_PREFIX.length()));
        }
        String npId = sourceNanopubId(view, queryRef);
        if (SOURCE_NP.equals(source)) return npId;
        if (SOURCE_NP_TEMPLATE.equals(source)) {
            Nanopub np = Utils.getAsNanopub(npId);
            if (np == null) return null;
            IRI templateId = TemplateData.get().getTemplateId(np);
            return templateId == null ? null : templateId.stringValue();
        }
        logger.warn("Unknown page source in action mapping: {}", source);
        return null;
    }

    /**
     * The one value a result column holds across every row, or null if it holds none or
     * several. A view-level action speaks for the whole view, so a column that varies from
     * row to row is not something it can act on — that is what an entry action is for, and
     * hiding the button is the safe reading (see {@link #applyEntryMappings}).
     *
     * @param response the view's results, or null
     * @param column   the result column to read
     * @return the single value, or null
     */
    private static String singleResultValue(ApiResponse response, String column) {
        if (response == null || column.isEmpty()) return null;
        String found = null;
        for (ApiResponseEntry row : response.getData()) {
            String value = row.get(column);
            if (value == null || value.isBlank()) continue;
            for (String token : value.trim().split("\\s+")) {
                if (found == null) {
                    found = token;
                } else if (!found.equals(token)) {
                    return null;
                }
            }
        }
        return found;
    }

    /**
     * The nanopub the view's query is bound to: the {@code <queryField>Np} parameter the
     * page filled in (see {@link ViewList}, which resolves it from the resource or, on a part
     * page, from the part's term definition).
     *
     * @param view     the view whose query field names the parameter
     * @param queryRef the query reference, or null
     * @return the nanopub id, or null when the page has none
     */
    private static String sourceNanopubId(View view, QueryRef queryRef) {
        if (view == null || queryRef == null || queryRef.getParams() == null) return null;
        String queryField = view.getQueryField();
        if (queryField == null || queryField.isBlank()) return null;
        Collection<String> values = queryRef.getParams().get(queryField + "Np");
        if (values == null) return null;
        for (String value : values) {
            // "x:" is the sentinel ViewList fills in when the page has no nanopub and the
            // placeholder is not optional -- no source, so no action.
            if (value != null && !value.isBlank() && !"x:".equals(value)) return value;
        }
        return null;
    }

    /**
     * Whether the action takes the template to open from its mappings (a {@code @template}
     * raw key) rather than from a declared {@code gen:hasActionTemplate}. A view showing
     * nanopubs of several templates wants the former, and then a declared one would only be
     * a fallback the action never uses.
     * <p>
     * Leaving the declaration off is also what makes such an action <em>invisible to older
     * Nanodash versions</em>, which cannot resolve the mapping and skip an action with no
     * template — rather than offering a button that opens an empty form. That is what lets a
     * view carrying one be published before the code that understands it is released.
     *
     * @param view      the view declaring the action
     * @param actionIri the action node IRI
     * @return true if a mapping supplies the template
     */
    private static boolean mapsTemplate(View view, IRI actionIri) {
        for (String mapping : view.getTemplateQueryMappings(actionIri)) {
            View.ActionMapping m = View.ActionMapping.parse(mapping);
            if (m != null && m.rawKey() && "template".equals(m.key())) return true;
        }
        return false;
    }

    /**
     * Whether the action opens the form in a fill mode, i.e. maps one of
     * {@link #FILL_MODE_KEYS} as a raw key.
     *
     * @param view      the view declaring the action
     * @param actionIri the action node IRI
     * @return true if the action's values come from a source nanopub
     */
    private static boolean declaresFillMode(View view, IRI actionIri) {
        for (String mapping : view.getTemplateQueryMappings(actionIri)) {
            View.ActionMapping m = View.ActionMapping.parse(mapping);
            if (m != null && m.rawKey() && FILL_MODE_KEYS.contains(m.key())) return true;
        }
        return false;
    }

    /**
     * Writes one resolved mapping into the link parameters: to the raw URL key, or to the
     * template field's {@code param_} name, locking it when the mapping asks for that.
     *
     * @param params the link parameters
     * @param m      the parsed mapping
     * @param value  the resolved, non-empty value
     */
    private static void setMapped(PageParameters params, View.ActionMapping m, String value) {
        params.set(m.rawKey() ? m.key() : "param_" + m.key(), value);
        // A "!" in front of the field name says that what the action fills in is not the
        // user's to change (issue #678): the field is shown with the value but locked.
        if (m.locked()) params.add("locked", "param_" + m.key());
    }

    /**
     * Writes the action's mapped values into the link parameters and returns whether
     * the button should be shown for this row.
     *
     * <p>Each mapping is {@code "col:target"}: the row's value for result column
     * {@code col} is written to URL parameter {@code param_target}, or — when
     * {@code target} starts with {@code @} — to the raw URL key {@code target}
     * (fill-mode keys such as {@code @derive-a} / {@code @supersede}). A {@code target}
     * written as {@code !field} additionally locks the field, so the form shows the
     * value the action filled in but does not let the user change it
     * (docs/locked-prefilled-values.md); this is for values an action determines rather
     * than proposes, such as the local public key an introduction is to declare. The button is
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
            // An action that takes its template from the data need not declare one; one that
            // does not, cannot open a form at all.
            if (t == null && !mapsTemplate(view, actionIri)) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            if (!label.endsWith("...")) label += "...";
            PageParameters params = new PageParameters().set("template-version", "latest");
            if (t != null) params.set("template", t.getId());
            // The target field names the resource the action is about. An action in a fill
            // mode takes every field from its source nanopub instead, so passing the page's
            // own resource on top could only overwrite a filled field of that name.
            if (id != null && !declaresFillMode(view, actionIri)) params.set("param_" + targetField, id);
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
            // Page sources are resolved here and now: a result action has no row to read
            // from, so this is the one way it can carry a value of the view's own — above
            // all the nanopub it is showing, for an action that opens that nanopub in a
            // fill mode. An unresolvable one hides the action, as an empty raw key does
            // for an entry action (docs/magic-query-params.md).
            List<String> queryMappings = new ArrayList<>();
            boolean pageSourceMissing = false;
            for (String mapping : view.getTemplateQueryMappings(actionIri)) {
                View.ActionMapping m = View.ActionMapping.parse(mapping);
                if (m == null) continue;
                if (!m.pageSource()) {
                    queryMappings.add(mapping);
                    continue;
                }
                String value = resolvePageSource(m.column(), view, queryRef, result.getApiResponse());
                if (value == null || value.isBlank()) {
                    pageSourceMissing = true;
                    break;
                }
                setMapped(params, m, value);
            }
            if (pageSourceMissing) continue;
            // Listing-driven mappings: the form re-runs this view's query and copies the
            // mapped columns of every row into the template (see PublishForm.applyQueryValues).
            if (!queryMappings.isEmpty()) {
                params.set("values-from-query", queryRef.getAsUrlString());
                params.set("values-from-query-mapping", String.join(" ", queryMappings));
            }
            // Target-driven fill (issue #690): the form runs the action's own query against
            // the target resource and copies its first row into the template, so a field
            // can default to something known about the target — the event's start date for
            // a presentation, say — with an empty listing being no obstacle. Bound to the
            // page resource, so there is nothing to fill on a resource-less page.
            GrlcQuery fillQuery = view.getFillQueryForAction(actionIri);
            List<String> fillMappings = view.getFillQueryMappings(actionIri);
            if (id != null && fillQuery != null && !fillMappings.isEmpty()) {
                String fillTargetField = view.getFillQueryTargetFieldForAction(actionIri);
                if (fillTargetField == null) fillTargetField = "resource";
                params.set("fill-query", new QueryRef(fillQuery.getQueryId(), fillTargetField, id).getAsUrlString());
                params.set("fill-query-mapping", String.join(" ", fillMappings));
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
            // See addResultActions: the template may come from the row instead.
            if (t == null && !mapsTemplate(view, actionIri)) continue;
            String targetField = view.getTemplateTargetFieldForAction(actionIri);
            if (targetField == null) targetField = "resource";
            String label = view.getLabelForAction(actionIri);
            if (label == null) label = "action...";
            if (!label.endsWith("...")) label += "...";
            PageParameters params = new PageParameters()
                    .set("context", contextId)
                    .set("template-version", "latest");
            if (t != null) params.set("template", t.getId());
            // See addResultActions: an action in a fill mode gets its fields from the source
            // nanopub, so the target field is not passed on top of them.
            if (!declaresFillMode(view, actionIri)) params.set("param_" + targetField, contextId);
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
            if (!applyEntryMappings(view, actionIri, row, params, queryRef)) {
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

    static boolean applyEntryMappings(View view, IRI actionIri, ApiResponseEntry row, PageParameters params, QueryRef queryRef) {
        Template template = view.getTemplateForAction(actionIri);
        for (String mapping : view.getTemplateQueryMappings(actionIri)) {
            View.ActionMapping m = View.ActionMapping.parse(mapping);
            if (m == null) continue;
            // The row is this action's own source of values, so a @result. source — the one
            // value a column holds for the whole view — has nothing to add here: map the row's
            // column directly instead. It resolves to nothing and hides the action.
            String value = m.pageSource() ? resolvePageSource(m.column(), view, queryRef, null) : row.get(m.column());
            if (value == null || value.isBlank()) {
                // Empty: hide the action only if the target is required. A page source is
                // always required — the page was meant to supply it, and an action built on
                // a value that isn't there would open a form on nothing.
                if (m.rawKey() || m.pageSource() || template == null || template.isRequiredField(m.key())) return false;
                continue;
            }
            setMapped(params, m, value);
        }
        return true;
    }

}
