package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.Literal;

/**
 * A component that represents a literal value in the context of a statement.
 * It displays the literal's string value.
 */
public class LiteralItem extends Panel implements ContextComponent {

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
        if (Utils.isHtmlLiteral(literal)) {
            // Content tagged as rdf:HTML is rendered as such, not shown as a quoted string
            // (see issue #378).
            add(new Label(LABEL_ID, Utils.sanitizeHtml(literal.stringValue()))
                    .setEscapeModelStrings(false)
                    .add(new AttributeAppender("class", "internal")));
        } else {
            add(new Label(LABEL_ID, "\"" + literal.stringValue() + "\""));
        }
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
     * Returns a string representation of the LiteralItem.
     *
     * @return a string describing the literal item
     */
    public String toString() {
        return "[Literal item: " + literal.stringValue() + "]";
    }

}
