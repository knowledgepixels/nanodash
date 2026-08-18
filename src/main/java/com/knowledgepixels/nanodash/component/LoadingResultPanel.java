package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * The loading state of a view display whose results are not there yet: the view's title
 * with the spinner beside it, in the same gutter position {@link RefreshingResultPanel}
 * uses for a refresh. So a view that is loading for the first time and one that is being
 * brought up to date look alike, and the page's section titles are in place from the first
 * paint instead of appearing only once the queries return.
 */
public class LoadingResultPanel extends Panel {

    /**
     * @param id    the Wicket markup id
     * @param title the view's title (not null or blank — without one there is nothing for
     *              the spinner to sit beside, and callers should show the bare icon)
     */
    public LoadingResultPanel(String id, String title) {
        super(id);
        add(new Label("label", title));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        // Marks the panel as updating, so the client-side indicator in nanodash.js leaves it
        // alone rather than adding a second spinner.
        tag.append("class", "view-refreshing", " ");
    }

}
