package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

/**
 * A reusable component that represents an item in a list with a link and an optional note.
 */
public class ItemListElement extends Panel {
    /**
     * Constructor for ItemListElement.
     *
     * @param markupId              the Wicket markup ID
     * @param pageClass             the class of the page to link to
     * @param parameters            the page parameters for the link
     * @param linkText              the text to display for the link
     * @param note                  an optional note to display alongside the link
     * @param sourceNanopublication the source nanopublication
     */
    public ItemListElement(String markupId, Class<? extends WebPage> pageClass, final PageParameters parameters, String linkText, String note, Nanopub sourceNanopublication) {
        super(markupId);

        add(new BookmarkablePageLink<>("link", pageClass, parameters).setBody(Model.of(linkText)));
        if (note == null) {
            add(new Label("note").setVisible(false));
        } else {
            add(new Label("note", note));
        }
        if (sourceNanopublication == null) {
            add(new Label("np").setVisible(false));
        } else {
            add(new BookmarkablePageLink<>("np", ExplorePage.class, new PageParameters().add("id", sourceNanopublication.getUri())));
        }
    }

    /**
     * Overloaded constructor without note.
     *
     * @param markupId   the Wicket markup ID
     * @param pageClass  the class of the page to link to
     * @param parameters the page parameters for the link
     * @param linkText   the text to display for the link
     */
    public ItemListElement(String markupId, Class<? extends WebPage> pageClass, final PageParameters parameters, String linkText) {
        this(markupId, pageClass, parameters, linkText, null, null);
    }

}
