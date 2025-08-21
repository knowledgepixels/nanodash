package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

public class LiteralItem extends Panel implements ContextComponent {

    private static final long serialVersionUID = 1L;

    private Literal literal;
//	private PublishFormContext context;

    public LiteralItem(String id, String parentId, Literal literal, RepetitionGroup rg) {
        super(id);
        this.literal = literal;
//		this.context = rg.getContext();
        add(new Label("literal", literal.stringValue()));
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

    public String toString() {
        return "[Literal item: " + literal.stringValue() + "]";
    }

}
