package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewAnchors;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ReferencesPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "Explore" tab body for a resource: the references to the thing. This is
 * the inline equivalent of what {@link
 * com.knowledgepixels.nanodash.page.ExplorePage} shows for an arbitrary term;
 * the standalone page forwards here for known spaces/users/resources/parts.
 */
public class ExplorePanel extends Panel {

    /**
     * @param id  the Wicket markup id
     * @param ref the IRI of the thing to explore
     */
    public ExplorePanel(String id, String ref) {
        super(id);

        // This panel builds its view panel itself rather than going through ViewList, so it
        // hands out the section anchor itself too (see docs/section-anchors.md).
        ViewAnchors.Allocator anchors = new ViewAnchors.Allocator();

        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        ViewDisplay refDisplay = new ViewDisplay(refView);
        add(anchors.anchor(QueryResultTableBuilder.create("references", new QueryRef(refView.getQuery().getQueryId(), "ref", ref), refDisplay).build(), refDisplay));
    }

}
