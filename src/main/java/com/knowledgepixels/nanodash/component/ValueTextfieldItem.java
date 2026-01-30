package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

/**
 * A text field component for entering values in a template.
 */
public class ValueTextfieldItem extends AbstractContextComponent {

    private TextField<String> textfield;
    private IRI iri;
    private static final Logger logger = LoggerFactory.getLogger(ValueTextfieldItem.class);

    /**
     * Constructor for creating a text field item.
     *
     * @param id       the component ID
     * @param parentId the parent component ID
     * @param iriP     the IRI associated with this text field
     * @param optional whether the field is optional
     * @param context  the template context containing models and components
     */
    public ValueTextfieldItem(String id, String parentId, final IRI iriP, boolean optional, final TemplateContext context) {
        super(id, context);
        this.iri = iriP;
        final Template template = context.getTemplate();
        IModel<String> model = (IModel<String>) context.getComponentModels().get(iri);
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
        }
        String postfix = Utils.getUriPostfix(iri);
        if (context.hasParam(postfix)) {
            model.setObject(context.getParam(postfix));
        }
        textfield = new TextField<>("textfield", model);
        if (!optional) textfield.setRequired(true);
        textfield.add(new Validator(iri, template));
        context.getComponents().add(textfield);
        if (template.getLabel(iri) != null) {
            textfield.add(new AttributeModifier("placeholder", template.getLabel(iri)));
            textfield.setLabel(Model.of(template.getLabel(iri)));
        }
        textfield.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (Component c : context.getComponents()) {
                    if (c == textfield) continue;
                    if (c.getDefaultModel() == textfield.getModel()) {
                        c.modelChanged();
                        target.add(c);
                    }
                }
            }

        });
        add(textfield);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromContext() {
        context.getComponents().remove(textfield);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnifiableWith(Value v) {
        if (v == null) return true;
        String vs = v.stringValue();
        if (v instanceof Literal vL) vs = Utils.getSerializedLiteral(vL);
        if (Utils.isLocalURI(vs)) vs = vs.replaceFirst("^" + LocalUri.PREFIX, "");
        Validatable<String> validatable = new Validatable<>(vs);
        if (v instanceof IRI && context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
            vs = Utils.getUriPostfix(vs);
        }
        new Validator(iri, context.getTemplate()).validate(validatable);
        if (!validatable.isValid()) {
            return false;
        }
        if (textfield.getModelObject() == null || textfield.getModelObject().isEmpty()) {
            return true;
        }
        return vs.equals(textfield.getModelObject());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unifyWith(Value v) throws UnificationException {
        if (v == null) return;
        if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
        String vs = v.stringValue();
        if (Utils.isLocalURI(vs)) {
            textfield.setModelObject(vs.replaceFirst("^" + LocalUri.PREFIX, ""));
        } else if (v instanceof Literal vL) {
            textfield.setModelObject(Utils.getSerializedLiteral(vL));
        } else {
            textfield.setModelObject(vs);
        }
    }


    /**
     * Validator class for validating the text field input.
     */
    protected static class Validator extends InvalidityHighlighting implements IValidator<String> {

//		private IRI iri;
//		private Template template;

        /**
         * Constructor for the validator.
         *
         * @param iri      the IRI associated with the value
         * @param template the template containing the context
         */
        public Validator(IRI iri, Template template) {
//			this.iri = iri;
//			this.template = template;
        }

        @Override
        public void validate(IValidatable<String> s) {
            if (s.getValue().startsWith("\"")) {
                if (!Utils.isValidLiteralSerialization(s.getValue())) {
                    s.error(new ValidationError("Invalid literal value"));
                }
                return;
            }
            String p = "";
            if (s.getValue().matches("[^:# ]+")) p = LocalUri.PREFIX;
            try {
                ParsedIRI piri = new ParsedIRI(p + s.getValue());
                if (!piri.isAbsolute()) {
                    s.error(new ValidationError("IRI not well-formed"));
                }
                if (p.isEmpty() && !Utils.isLocalURI(s.getValue()) && !(s.getValue()).matches("https?://.+")) {
                    s.error(new ValidationError("Only http(s):// IRIs are allowed here"));
                }
            } catch (URISyntaxException ex) {
                s.error(new ValidationError("IRI not well-formed"));
            }
        }

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
        Value defaultValue = context.getTemplate().getDefault(iri);
        if (isUnifiableWith(defaultValue)) {
            try {
                unifyWith(defaultValue);
            } catch (UnificationException ex) {
                logger.error("Unification with default value failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "[value textfield item: " + iri + "]";
    }

}
