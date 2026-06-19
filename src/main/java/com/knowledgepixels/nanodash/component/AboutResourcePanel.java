package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "About" tab body for a maintained resource: its assigned presets and the
 * listing of its configured view displays (issue #302). Resource-level
 * members/roles are intentionally not shown (roles live on the parent space).
 */
public class AboutResourcePanel extends Panel {

    /**
     * The "ℹ️ Info" view for a maintained resource: key-value facts (type,
     * namespace, maintaining space). Also shown on the Content tab; surfaced here
     * at the top of the About tab.
     */
    public static final String MAINTAINED_RESOURCE_INFO_VIEW = "https://w3id.org/np/RAqKkqvOFWWCDJ4LOa6rrgQJMFbDnoaG56zkIBN5AMBZw/maintained-resource-info-view-kind";

    /**
     * View listing a maintained resource's configured view displays. Mirrors the space
     * view-displays view but its "add view display" action links the maintained-resource-
     * specific creation template.
     */
    public static final String MAINTAINED_RESOURCE_VIEW_DISPLAYS_VIEW = "https://w3id.org/np/RAZOxROZRaxSeoeO05iI0tKpDIy2mEN3jlUuVIo0uZN1I/view-displays-view";

    /**
     * View listing the presets assigned to a maintained resource. Mirrors the space
     * preset-assignments view but its "add preset" action links the maintained-resource-specific
     * template (offering only presets that apply to maintained resources).
     */
    public static final String MAINTAINED_RESOURCE_PRESET_ASSIGNMENTS_VIEW = "https://w3id.org/np/RA-CRSYJrYf-uaw1fip7hr_kdPqVb7_6dENwTPYQpikRY/preset-assignments-view";

    /**
     * @param id       the Wicket markup id
     * @param resource the maintained resource whose About listings to render
     */
    public AboutResourcePanel(String id, MaintainedResource resource) {
        super(id);

        // The info view leads the "Structure" section (to the left of the presets).
        View infoView = View.get(MAINTAINED_RESOURCE_INFO_VIEW);
        add(QueryResultTableBuilder.create("info", new QueryRef(infoView.getQuery().getQueryId(), "resource", resource.getId()), new ViewDisplay(infoView)).resourceWithProfile(resource).id(resource.getId()).contextId(resource.getId()).build());

        View presetsView = View.get(MAINTAINED_RESOURCE_PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", resource.getId()), new ViewDisplay(presetsView)).resourceWithProfile(resource).id(resource.getId()).contextId(resource.getId()).build());

        View vdView = View.get(MAINTAINED_RESOURCE_VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(QueryApiAccess.LIST_VIEW_DISPLAYS, "resource", resource.getId()), new ViewDisplay(vdView)).resourceWithProfile(resource).id(resource.getId()).contextId(resource.getId()).build());
    }

}
