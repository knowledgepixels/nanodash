package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;

/**
 * A text area for long literal values.
 */
public class LiteralTextareaItem extends LiteralTextfieldItem {

    private static final long serialVersionUID = 1L;
    private TextArea<String> textarea;

    /**
     * Constructor for a long literal textfield item.
     *
     * @param id       the component id
     * @param iri      the IRI of the literal
     * @param optional whether the field is optional
     * @param context  the template context
     */
    public LiteralTextareaItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, iri, optional, context);
    }

    protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
        textarea = new TextArea<>("textarea", model);
        return textarea;
    }

    protected AbstractTextComponent<String> getTextComponent() {
        return textarea;
    }

    @Override
    public String toString() {
        return "[Long literal textfield item]";
    }

}
