package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.util.Set;

/**
 * The "About" tab body for a resource part: the presets assigned to the part's
 * owning resource, and that resource's configured view displays each flagged
 * (shown_here) for this specific part (issue #302). Read-only: part-level views
 * are inherited from the owning resource via class/namespace targeting, so their
 * configuration is managed on the owning resource, not the part. The "ℹ️ Info"
 * section is intentionally left for later.
 */
public class AboutPartPanel extends Panel {

    /**
     * The "⬜ View displays" view for a part: the owning resource's view displays,
     * each flagged for this specific part via the partid/partclass parameters.
     * Read-only (no add/deactivate actions).
     */
    public static final String PART_VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAHPaxWXTO6Utt54oRFQqTjyGuk4w7y8IDZEsmNqeptwE/part-view-displays-view";

    /**
     * @param id          the Wicket markup id
     * @param context     the part's owning resource (maintained resource, space, or user)
     * @param partId      the part IRI
     * @param partClasses the part's own RDF types (used to flag which of the owning
     *                    resource's views are shown on this part)
     */
    public AboutPartPanel(String id, AbstractResourceWithProfile context, String partId, Set<IRI> partClasses) {
        super(id);

        // Presets are assigned to the owning resource (a part has none of its own),
        // so this lists the owning resource's presets.
        View presetsView = View.get(AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", context.getId()), new ViewDisplay(presetsView)).resourceWithProfile(context).id(context.getId()).contextId(context.getId()).build());

        // View displays: the owning resource's displays (resource = context, for the
        // display set + admin/maintainer auth), with shown_here computed for THIS part
        // (partid + the part's classes). id = context so the view's "add view display"
        // action creates a display on the owning resource (where part-level views live,
        // and so the new display appears in this list).
        View vdView = View.get(PART_VIEW_DISPLAYS_VIEW);
        Multimap<String, String> vdParams = ArrayListMultimap.create();
        vdParams.put("resource", context.getId());
        vdParams.put("partid", partId);
        for (IRI partClass : partClasses) {
            vdParams.put("partclass", partClass.stringValue());
        }
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), vdParams), new ViewDisplay(vdView)).resourceWithProfile(context).id(context.getId()).contextId(context.getId()).build());
    }

}
