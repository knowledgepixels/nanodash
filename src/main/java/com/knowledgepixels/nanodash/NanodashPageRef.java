package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/**
 * Represents a reference to a Nanodash page, including its class, parameters, and label.
 * This class is used to create components for navigation or display purposes.
 */
public class NanodashPageRef implements Serializable {

    private final Class<? extends NanodashPage> pageClass;
    private final PageParameters parameters;
    private final String label;

    /**
     * Constructs a NanodashPageRef with the specified page class, parameters, and label.
     *
     * @param pageClass  The class of the Nanodash page.
     * @param parameters The parameters for the page.
     * @param label      The label for the page reference.
     */
    public NanodashPageRef(Class<? extends NanodashPage> pageClass, PageParameters parameters, String label) {
        this.pageClass = pageClass;
        this.parameters = parameters;
        this.label = label;
    }

    /**
     * Constructs a NanodashPageRef with the specified page class and label.
     * Parameters are set to null.
     *
     * @param pageClass The class of the Nanodash page.
     * @param label     The label for the page reference.
     */
    public NanodashPageRef(Class<? extends NanodashPage> pageClass, String label) {
        this(pageClass, null, label);
    }

    /**
     * Constructs a NanodashPageRef with the specified label.
     * Page class and parameters are set to null.
     *
     * @param label The label for the page reference.
     */
    public NanodashPageRef(String label) {
        this(null, null, label);
    }

    /**
     * Returns the class of the Nanodash page being referenced.
     *
     * @return The page class.
     */
    public Class<? extends NanodashPage> getPageClass() {
        return pageClass;
    }

    /**
     * Returns the parameters associated with the page reference.
     *
     * @return The page parameters.
     */
    public PageParameters getParameters() {
        return parameters;
    }

    /**
     * Returns the label for the page reference.
     *
     * @return The label of the page reference.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Creates a WebMarkupContainer component for the page reference.
     * If the page class is null, an external link is created.
     * Otherwise, a bookmarkable page link is created.
     *
     * @param id The Wicket ID for the component.
     * @return The created WebMarkupContainer.
     */
    public WebMarkupContainer createComponent(String id) {
        if (pageClass == null) {
            ExternalLink l = new ExternalLink(id, "#");
            l.add(new Label(id + "-label", label));
            return l;
        } else {
            BookmarkablePageLink<Void> l = new BookmarkablePageLink<>(id, pageClass, parameters);
            l.add(new Label(id + "-label", label));
            return l;
        }
    }

}
