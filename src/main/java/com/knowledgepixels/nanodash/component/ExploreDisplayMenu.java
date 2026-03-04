package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

public class ExploreDisplayMenu extends BaseDisplayMenu {

    /**
     * Constructs a ExploreDisplayMenu.
     *
     * @param id           the Wicket component ID
     * @param exploreUri   the URI of the resource to explore
     * @param exploreLabel the label of the resource to be rendered in the explore page
     * @param sourceUri    the uri of the source nanopublication defining the resource
     */
    public ExploreDisplayMenu(String id, String exploreUri, String exploreLabel, IRI sourceUri) {
        super(id);

        addEntry("explore", new BookmarkablePageLink<Void>("explore", ExplorePage.class, new PageParameters().set("id", exploreUri).set("label", exploreLabel)));
        addEntry("viewDeclaration", new BookmarkablePageLink<Void>("viewDeclaration", ExplorePage.class, new PageParameters().set("id", sourceUri)));
    }

}
