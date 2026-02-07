package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

/**
 * Component for displaying a link to a source nanopublication. The link navigates to the ExplorePage with the specified nanopublication IRI.
 */
public class SourceNanopub extends Panel {

    private final String ELEMENT_PATH = "np";

    /**
     * Constructor for SourceNanopub.
     *
     * @param markupId  the markup ID
     * @param sourceIri the IRI of the source nanopublication
     */
    public SourceNanopub(String markupId, IRI sourceIri) {
        super(markupId);
        PageParameters pageParameters = new PageParameters();
        pageParameters.add("id", sourceIri);
        add(new BookmarkablePageLink<Void>(ELEMENT_PATH, ExplorePage.class, pageParameters));
    }

    /**
     * Constructor for SourceNanopub with additional CSS classes.
     *
     * @param markupId          the markup ID for the component
     * @param sourceIriAsString the IRI of the source nanopublication as a string
     * @param additionalClasses additional CSS classes to add to the component
     */
    public SourceNanopub(String markupId, IRI sourceIriAsString, String... additionalClasses) {
        this(markupId, sourceIriAsString);
        if (additionalClasses != null && additionalClasses.length > 0) {
            String classes = String.join(" ", additionalClasses);
            this.get(ELEMENT_PATH).add(new AttributeAppender("class", " " + classes));
        }
    }

}
