package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.panel.Panel;

/**
 * A component that stands in for a statement part the template doesn't define, such as a
 * statement without an {@code rdf:subject}. Statements containing it cannot be published.
 */
public class MissingValueItem extends Panel implements ContextComponent {

    /**
     * Constructor for creating a MissingValueItem.
     *
     * @param id the Wicket component ID
     */
    public MissingValueItem(String id) {
        super(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        // Nothing to be done here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalizeValues() {
    }

    /**
     * Returns a string representation of the MissingValueItem.
     *
     * @return a string describing the missing value item
     */
    public String toString() {
        return "[Missing value item]";
    }

}
