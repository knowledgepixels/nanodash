package com.knowledgepixels.nanodash.component;

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
     * @param id       the Wicket markup id
     * @param resource the maintained resource whose About listings to render
     */
    public AboutResourcePanel(String id, MaintainedResource resource) {
        super(id);

        View presetsView = View.get(AboutSpacePanel.PRESET_ASSIGNMENTS_VIEW);
        add(QueryResultTableBuilder.create("presets", new QueryRef(presetsView.getQuery().getQueryId(), "resource", resource.getId()), new ViewDisplay(presetsView)).id(resource.getId()).contextId(resource.getId()).build());

        View vdView = View.get(AboutSpacePanel.VIEW_DISPLAYS_VIEW);
        add(QueryResultTableBuilder.create("viewdisplays", new QueryRef(vdView.getQuery().getQueryId(), "resource", resource.getId()), new ViewDisplay(vdView)).id(resource.getId()).contextId(resource.getId()).build());
    }

}
