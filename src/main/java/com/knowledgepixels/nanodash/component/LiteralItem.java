package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

/**
 * A component that represents a literal value in the context of a statement.
 * It displays the literal's string value and implements the ContextComponent interface
 * to allow for unification with other values.
 */
public class LiteralItem extends Panel implements ContextComponent {

    private static final long serialVersionUID = 1L;

    private Literal literal;

    static final String LABEL_ID = "literal";
//	private PublishFormContext context;

    /**
     * Constructor for creating a LiteralItem.
     *
     * @param id       the Wicket component ID
     * @param parentId the ID of the parent component
     * @param literal  the literal value to be displayed
     * @param rg       the repetition group context
     */
    public LiteralItem(String id, String parentId, Literal literal, RepetitionGroup rg) {
        super(id);
        this.literal = literal;
//		this.context = rg.getContext();
        add(new Label(LABEL_ID, literal.stringValue()));
    }

    @Override
    public void removeFromContext() {
        // Nothing to be done here.
    }

    @Override
    public boolean isUnifiableWith(Value v) {
        if (!(v instanceof Literal)) return false;
        return literal.stringValue().equals(v.stringValue());
    }

    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        // Nothing left to be done here.
    }

    @Override
    public void fillFinished() {
    }

    @Override
    public void finalizeValues() {
    }

    /**
     * Returns a string representation of the LiteralItem.
     *
     * @return a string describing the literal item
     */
    public String toString() {
        return "[Literal item: " + literal.stringValue() + "]";
    }

}
