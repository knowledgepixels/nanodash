package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

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
