package com.knowledgepixels.nanodash.component.menu;

import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import java.util.List;

/**
 * A per-entry (per-row) dropdown menu, using the same chevron trigger as
 * {@link ViewDisplayMenu}. It bundles a result row's entry-level actions together
 * with its "source" link (the former "^" link) into a single menu.
 *
 * <p>Each supplied link must use the markup id {@code "link"}; the links are rendered
 * in order as menu entries.</p>
 */
public class EntryActionMenu extends BaseDisplayMenu {

    /**
     * @param id    the Wicket component id
     * @param links the menu entries, each an {@link AbstractLink} with markup id {@code "link"}
     */
    public EntryActionMenu(String id, List<AbstractLink> links) {
        super(id);
        add(new DataView<AbstractLink>("entries", new ListDataProvider<>(links)) {
            @Override
            protected void populateItem(Item<AbstractLink> item) {
                item.add(item.getModelObject());
            }
        });
    }

}
