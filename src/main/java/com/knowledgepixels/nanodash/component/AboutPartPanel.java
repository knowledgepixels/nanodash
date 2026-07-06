package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.User;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.util.Set;

/**
 * The "About" tab body for a resource part: an "ℹ️ Info" table (the part's type,
 * owning resource, and defining nanopub), the presets assigned to the part's
 * owning resource, and that resource's configured view displays each flagged
 * (displayed_here) for this specific part (issue #302). The presets and view displays
 * are inherited from the owning resource (a part has none of its own); the view
 * displays are managed on the owning resource, where part-level views live.
 */
public class AboutPartPanel extends Panel {

    /**
     * The "ℹ️ Info" view for a part: a key-value table (Type / Belongs to / Defined
     * in) sourced from the part's defining nanopub, found among the owning resource's
     * space members' pubkeys (as on the part page itself).
     */
    public static final String PART_INFO_VIEW = "https://w3id.org/np/RAKMu5L23lMUfZe-DSGopJTRFHBh-drfvazSDC6ZocxhU/part-info-view";

    /**
     * The "⬜ View displays" view for a part: the owning resource's view displays,
     * each flagged for this specific part via the partid/partclass parameters.
     */
    public static final String PART_VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAkvLDMWg1GCSY2RUpvVA0P3th3gzjUcoaXTzqMeFi5OY/part-view-displays-view";

    /**
     * @param id          the Wicket markup id
     * @param context     the part's owning resource (maintained resource, space, or user)
     * @param partId      the part IRI
     * @param partClasses the part's own RDF types (used to flag which of the owning
     *                    resource's views are shown on this part)
     */
    public AboutPartPanel(String id, AbstractResourceWithProfile context, String partId, Set<IRI> partClasses) {
        super(id);

        // Info: the part's type / owning resource / defining nanopub, read from the
        // part's defining nanopub. As on the part page, the definition is looked up
        // among the owning resource's space members' pubkeys (or the owning user's).
        View infoView = View.get(PART_INFO_VIEW);
        Multimap<String, String> infoParams = ArrayListMultimap.create();
        infoParams.put("partid", partId);
        infoParams.put("context", context.getId());
        if (context.getSpace() != null) {
            for (IRI userIri : context.getSpace().getUsers()) {
                for (String pubkey : User.getUserData().getPubkeyHashes(userIri, true)) {
                    infoParams.put("pubkey", pubkey);
                }
            }
        } else {
            for (String pubkey : User.getUserData().getPubkeyHashes(Utils.vf.createIRI(context.getId()), true)) {
                infoParams.put("pubkey", pubkey);
            }
        }
        add(QueryResultTableBuilder.create("info", new QueryRef(infoView.getQuery().getQueryId(), infoParams), new ViewDisplay(infoView)).resourceWithProfile(context).id(context.getId()).contextId(context.getId()).build());

        // Presets are assigned to the owning resource (a part has none of its own),
        // so this lists the owning resource's presets. Uses the maintained-resource preset
        // view, so the "add preset" action links the maintained-resource template and
        // pre-fills the owning resource IRI (id = context), not the part.
        View presetsView = View.get(AboutResourcePanel.MAINTAINED_RESOURCE_PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", context.getId()), new ViewDisplay(presetsView)).resourceWithProfile(context).id(context.getId()).contextId(context.getId()).build());

        // View displays: the owning resource's displays (resource = context, for the
        // display set + admin/maintainer auth), with displayed_here computed for THIS part
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
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(QueryApiAccess.LIST_PART_VIEW_DISPLAYS, vdParams), new ViewDisplay(vdView)).resourceWithProfile(context).id(context.getId()).contextId(context.getId()).build());
    }

}
