package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;

/**
 * A Wicket behavior that adds a CSS class to form components that are invalid.
 */
public class InvalidityHighlighting extends Behavior {

    /**
     * Constructor for InvalidityHighlighting.
     * This behavior can be attached to any FormComponent to highlight invalid inputs.
     */
    public InvalidityHighlighting() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onComponentTag(Component c, ComponentTag tag) {
        FormComponent<?> fc = (FormComponent<?>) c;
        if (!fc.isValid()) {
            tag.append("class", "invalid", " ");
        }
    }

    /**
     * Updates the highlighting of a form component in the browser after an Ajax request has
     * validated its input, without re-rendering it, so the field keeps its focus and cursor.
     *
     * @param target the Ajax request target
     * @param fc     the form component whose input has just been validated
     */
    public static void refresh(AjaxRequestTarget target, FormComponent<?> fc) {
        target.appendJavaScript(String.format(
                "var e = document.getElementById('%s'); if (e) e.classList.toggle('invalid', %b);",
                fc.getMarkupId(), !fc.isValid()));
    }

}
