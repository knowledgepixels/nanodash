package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;

/**
 * A Wicket behavior that adds a CSS class to form components that are invalid.
 */
public class InvalidityHighlighting extends Behavior {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for InvalidityHighlighting.
     * This behavior can be attached to any FormComponent to highlight invalid inputs.
     */
    public InvalidityHighlighting() {
    }

    @Override
    public void onComponentTag(Component c, ComponentTag tag) {
        FormComponent<?> fc = (FormComponent<?>) c;
        if (!fc.isValid()) {
            tag.append("class", "invalid", " ");
        }
    }

}