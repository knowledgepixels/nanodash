package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.template.TemplateContext;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * An abstract base class that extends Wicket's Panel and implements ContextComponent, providing a TemplateContext for derived components.
 */
public abstract class AbstractContextComponent extends Panel implements ContextComponent {

    protected TemplateContext context;
    protected static ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String LOCKED_FIELD_MESSAGE = "This value is set by the link that opened this form and cannot be changed here.";

    /**
     * Makes the given form component follow the lock state of the given placeholder: a locked
     * placeholder is shown with its pre-filled value but cannot be edited (issue #678).
     * <p>
     * The component stays enabled as far as Wicket is concerned, and the value keeps being
     * submitted with the form: a locked field is still an ordinary field holding a value, and
     * disabling it in Wicket would make the browser send nothing for it, which the form then reads
     * as an emptied field and reports as a missing required value. Text inputs are marked
     * {@code readonly}, which browsers submit; anything else (the choice fields, which HTML has no
     * readonly for) is marked {@code disabled} and its value mirrored in a hidden field of the
     * same name, so that what reaches the form is the locked value either way.
     * <p>
     * The state is decided at render time rather than at construction time, because it can change
     * while the form is open: removing a repetition group shifts the values of the following
     * groups up into its slot, and the lock travels with the value (see
     * {@link TemplateContext#moveLock(String, String)}).
     * <p>
     * This is a guardrail rather than enforcement: the lock is stated in the URL that opened the
     * form, so it stops accidental edits, not deliberate ones.
     *
     * @param component the form component to lock when its placeholder is locked
     * @param iri       the placeholder IRI, including any repetition suffix
     */
    protected void lockIfNeeded(final FormComponent<?> component, final IRI iri) {
        final TemplateContext c = context;
        component.add(new Behavior() {

            // What the tag turned out to be in this render pass, so that afterRender knows
            // whether the value still needs a hidden mirror field.
            private boolean renderedAsTextInput = false;

            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                renderedAsTextInput = isTextInput(tag);
                if (!c.isLocked(iri)) return;
                // A field that is simply greyed out leaves the user guessing why they can't type
                // in it, so say where the value came from.
                tag.put("title", LOCKED_FIELD_MESSAGE);
                tag.append("class", "locked-value", " ");
                if (renderedAsTextInput) {
                    tag.put("readonly", "readonly");
                } else {
                    tag.put("disabled", "disabled");
                }
            }

            @Override
            public void afterRender(Component component) {
                if (!c.isLocked(iri) || renderedAsTextInput) return;
                // Browsers don't submit a disabled control, so mirror its value in a hidden field
                // of the same name to keep the form seeing the value it rendered.
                FormComponent<?> fc = (FormComponent<?>) component;
                String value = fc.getValue() == null ? "" : fc.getValue();
                component.getResponse().write("<input type=\"hidden\" name=\""
                        + Strings.escapeMarkup(fc.getInputName()) + "\" value=\""
                        + Strings.escapeMarkup(value) + "\" />");
            }

        });
    }

    /**
     * Whether the tag renders as a control that browsers submit while readonly. Decided on the
     * rendered tag rather than on the Java class, because a choice field can be a text component
     * that renders as a {@code select} (the select2-based choice fields are).
     */
    private static boolean isTextInput(ComponentTag tag) {
        String name = tag.getName().toLowerCase();
        if ("textarea".equals(name)) return true;
        if (!"input".equals(name)) return false;
        String type = tag.getAttribute("type");
        return !"checkbox".equalsIgnoreCase(type) && !"radio".equalsIgnoreCase(type) && !"hidden".equalsIgnoreCase(type);
    }

    /**
     * Constructor for AbstractContextComponent.
     *
     * @param id      the Wicket component ID
     * @param context the TemplateContext to be used by this component
     */
    public AbstractContextComponent(String id, TemplateContext context) {
        super(id);
        this.context = context;
    }

}
