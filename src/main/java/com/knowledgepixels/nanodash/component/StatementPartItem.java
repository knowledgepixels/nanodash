package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.panel.Panel;

/**
 * Represents a part of a statement consisting of subject, predicate, and object.
 */
public class StatementPartItem extends Panel {

    private static final long serialVersionUID = 1L;

    private final ValueItem subjItem, predItem, objItem;

    /**
     * Constructor for creating a StatementPartItem.
     *
     * @param id       the component ID
     * @param subjItem subject item
     * @param predItem predicate item
     * @param objItem  object item
     */
    public StatementPartItem(String id, ValueItem subjItem, ValueItem predItem, ValueItem objItem) {
        super(id);
        this.subjItem = subjItem;
        this.predItem = predItem;
        this.objItem = objItem;
        add(subjItem);
        add(predItem);
        add(objItem);
    }

    /**
     * Gets the subject item.
     *
     * @return the subject item
     */
    public ValueItem getSubject() {
        return subjItem;
    }

    /**
     * Gets the predicate item.
     *
     * @return the predicate item
     */
    public ValueItem getPredicate() {
        return predItem;
    }

    /**
     * Gets the object item.
     *
     * @return the object item
     */
    public ValueItem getObject() {
        return objItem;
    }

    /**
     * Returns a string representation of the statement part.
     *
     * @return a string in the format "subject predicate object"
     */
    public String toString() {
        return subjItem + " " + predItem + " " + objItem;
    }

}
