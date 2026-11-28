package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.Utils;
import org.nanopub.templates.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.templates.RawValue;


/**
 * A text field component for entering values in a template.
 */
public class ValueTextfieldItem extends AbstractContextComponent {

    private TextField<String> textfield;
    private IRI iri;

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
        boolean modelIsNew = false;
        if (model == null) {
            model = Model.of("");
            context.getComponentModels().put(iri, model);
            modelIsNew = true;
        }
        String postfix = Utils.getUriPostfix(iri);
        if (modelIsNew && context.hasValue(postfix)) {
            model.setObject(context.getValue(postfix));
        }
        textfield = new TextField<>("textfield", model);
        if (!optional) textfield.setRequired(true);
        textfield.add(new PlaceholderValidator(context, iri));
        context.getComponents().add(textfield);
        lockIfNeeded(textfield, iri, false);
        labelField(textfield, iri);
        if (template.getLabel(iri) != null) {
            textfield.add(new AttributeModifier("placeholder", template.getLabel(iri)));
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
        add(lockedTooltip());
    }

    /**
     * The hover box of a locked field: why the value cannot be changed here, and the whole value
     * it holds, of which the field itself shows only as much as it is wide (issue #786). A URI is
     * shown as the link every other non-changeable value is shown as, so it can be read, followed
     * and copied out from here too. Only a locked field gets a box: one opening under a field that
     * is there to be typed in would be in the way.
     */
    private WebMarkupContainer lockedTooltip() {
        WebMarkupContainer tooltip = new WebMarkupContainer("lockedtooltip") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                // Read at render time, like the lock itself: it can change while the form is open
                // (see AbstractContextComponent#lockIfNeeded).
                setVisible(context.isLocked(iri));
            }

        };
        tooltip.add(new Label("description", LOCKED_FIELD_MESSAGE));
        tooltip.add(Utils.getFullUriLink("uri", new FullValueModel()));
        return tooltip;
    }

    /**
     * The whole value the field stands for: a literal as it was typed, and a bare name under the
     * local prefix the validator reads it as. Empty for an empty field.
     */
    private class FullValueModel implements IModel<String> {

        @Override
        public String getObject() {
            String value = textfield.getModelObject();
            if (value == null || value.isEmpty()) return "";
            if (value.startsWith("\"")) return value;
            if (value.matches("[^:# ]+")) return LocalUri.PREFIX + value;
            return value;
        }

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
    public void finalizeValues() {
        if (textfield.getModelObject() != null && !textfield.getModelObject().isEmpty()) return;
        RawValue defaultValue = context.getDefaultRaw(iri);
        if (defaultValue != null) textfield.setModelObject(defaultValue.text());
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
