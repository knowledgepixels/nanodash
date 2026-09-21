package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.wicketstuff.kendo.ui.widget.editor.Editor;

/**
 * A rich-text editor for literals declared with the {@code rdf:HTML} datatype (issue #378).
 * Such a literal is rendered as HTML wherever it is shown, so it is also written as HTML
 * here, rather than leaving the author to type markup into a plain text area.
 * <p>
 * The markup the editor produces is sanitized when the value is finalized, with the same
 * policy that renders it later, so a nanopublication cannot carry markup that would be
 * dropped on display — and pasted content brings no scripting into the assertion. A
 * nanopublication cannot be edited after publication, which is why this happens before
 * signing rather than only at render time.
 */
public class LiteralHtmlEditorItem extends LiteralTextfieldItem {

    private Editor editor;

    /**
     * Constructor for an HTML literal editor item.
     *
     * @param id       the component id
     * @param iri      the IRI of the literal placeholder
     * @param optional whether the field is optional
     * @param context  the template context
     */
    public LiteralHtmlEditorItem(String id, final IRI iri, boolean optional, TemplateContext context) {
        super(id, iri, optional, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
        editor = new Editor("textarea", model);
        return editor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractTextComponent<String> getTextComponent() {
        return editor;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sanitizes the markup before it becomes part of the nanopublication.
     */
    @Override
    public void finalizeValues() {
        IModel<String> model = editor.getModel();
        String value = model.getObject();
        if (value != null && !value.isBlank()) {
            model.setObject(Utils.sanitizeHtml(value));
        }
        super.finalizeValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[HTML literal editor item]";
    }

}
