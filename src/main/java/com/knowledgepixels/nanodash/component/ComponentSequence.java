package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.List;

/**
 * A Wicket component that displays a sequence of components separated by a specified separator.
 * The first component does not have a separator before it.
 */
public class ComponentSequence extends Panel {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ComponentSequence with the given ID, separator, and list of components.
     *
     * @param id         the Wicket component ID
     * @param separator  the string to use as a separator between components
     * @param components the list of components to display in sequence
     */
    public ComponentSequence(String id, final String separator, final List<Component> components) {
        super(id);
        add(new ListView<Component>("components", components) {

            private static final long serialVersionUID = 1L;
            private boolean isFirst = true;

            @Override
            protected void populateItem(ListItem<Component> item) {
                if (isFirst) {
                    item.add(new Label("separator").setVisible(false));
                    isFirst = false;
                } else {
                    item.add(new Label("separator", separator));
                }
                item.add(item.getModelObject());
            }

        });
    }

}
