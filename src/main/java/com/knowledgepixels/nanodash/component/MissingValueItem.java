package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.Value;

/**
 * A component that stands in for a statement part the template doesn't define, such as a
 * statement without an {@code rdf:subject}. It never unifies, so statements containing it
 * cannot be matched or published.
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
    public boolean isUnifiableWith(Value v) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        throw new UnificationException(v == null ? "null" : v.stringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFinished() {
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
