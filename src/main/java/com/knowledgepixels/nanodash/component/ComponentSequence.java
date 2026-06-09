package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A Wicket component that displays a sequence of components separated by a specified separator.
 * The first component does not have a separator before it.
 */
public class ComponentSequence extends Panel {

    /**
     * Constructs a ComponentSequence with the given ID, separator, and list of components.
     *
     * @param id         the Wicket component ID
     * @param separator  the string to use as a separator between components
     * @param components the list of components to display in sequence
     */
    public ComponentSequence(String id, final String separator, final List<Component> components) {
        this(id, separator, components, Collections.emptySet());
    }

    /**
     * Constructs a ComponentSequence in which the components at the given indices are
     * preceded by a single space instead of the full separator (e.g. a trailing "^"
     * link that should hug the preceding content with just a space rather than read as
     * its own list item).
     *
     * @param id              the Wicket component ID
     * @param separator       the string to use as a separator between components
     * @param components      the list of components to display in sequence
     * @param spaceSeparator  indices of components to precede with a single space instead of the separator
     */
    public ComponentSequence(String id, final String separator, final List<Component> components, final Set<Integer> spaceSeparator) {
        super(id);
        add(new ListView<Component>("components", components) {

            private boolean isFirst = true;

            @Override
            protected void populateItem(ListItem<Component> item) {
                boolean first = isFirst;
                isFirst = false;
                String sep = spaceSeparator.contains(item.getIndex()) ? " " : separator;
                item.add(new Label("separator", sep).setVisible(!first));
                item.add(item.getModelObject());
            }

        });
    }

}
