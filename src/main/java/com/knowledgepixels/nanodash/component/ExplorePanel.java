package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ReferencesPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.nanopub.extra.services.QueryRef;

/**
 * The "Explore" tab body for a resource: the generic exploration panels (RDF
 * types/classes, where the thing is described, instances, templates) plus the
 * references to the thing. This is the inline equivalent of what {@link
 * com.knowledgepixels.nanodash.page.ExplorePage} shows for an arbitrary term;
 * the standalone page forwards here for known spaces/users/resources/parts.
 */
public class ExplorePanel extends Panel {

    private static final String DESCRIBED_IN_VIEW = "https://w3id.org/np/RAMH_7qMY-jmgXr2jqqk5F_XW7t2k2n3NCB6LtoKEXDzY/described-in-view";
    private static final String CLASSES_VIEW = "https://w3id.org/np/RAHPtR1VriEW09tcvZhrM8Dr3vE1JnMWWi9-ajKJWNOJs/classes-view";
    private static final String INSTANCES_VIEW = "https://w3id.org/np/RABXfsNoT_RYlk8LpDmKfJ2poSlvIGk3jgq4DkR4YLAps/instances-view";
    private static final String TEMPLATES_VIEW = "https://w3id.org/np/RAP0-S9PUUVF1rQiqo8vq8z6XWsXkeGBUo60DJf8JsXsc/templates-view";

    /**
     * @param id  the Wicket markup id
     * @param ref the IRI of the thing to explore
     */
    public ExplorePanel(String id, String ref) {
        super(id);

        View classesView = View.get(CLASSES_VIEW);
        add(QueryResultListBuilder.create("classes-panel", new QueryRef(classesView.getQuery().getQueryId(), "thing", ref), new ViewDisplay(classesView)).build());

        View describedInView = View.get(DESCRIBED_IN_VIEW);
        add(QueryResultNanopubSetBuilder.create("definitions-panel", new QueryRef(describedInView.getQuery().getQueryId(), "term", ref), new ViewDisplay(describedInView)).build());

        View instancesView = View.get(INSTANCES_VIEW);
        add(QueryResultListBuilder.create("instances-panel", new QueryRef(instancesView.getQuery().getQueryId(), "class", ref), new ViewDisplay(instancesView)).build());

        View templatesView = View.get(TEMPLATES_VIEW);
        add(QueryResultListBuilder.create("templates-panel", new QueryRef(templatesView.getQuery().getQueryId(), "thing", ref), new ViewDisplay(templatesView)).build());

        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        add(QueryResultTableBuilder.create("references", new QueryRef(refView.getQuery().getQueryId(), "ref", ref), new ViewDisplay(refView)).build());
    }

}
