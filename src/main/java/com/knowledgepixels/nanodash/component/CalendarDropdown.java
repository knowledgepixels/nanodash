package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import java.util.List;

/**
 * A dropdown menu component for calendar actions.
 */
public class CalendarDropdown extends Panel {

    public CalendarDropdown(String id, List<DateTimeCalendarCell.CalendarAction> actions) {
        super(id);

        add(new DataView<>("actions", new ListDataProvider<>(actions)) {
            @Override
            protected void populateItem(Item<DateTimeCalendarCell.CalendarAction> item) {
                DateTimeCalendarCell.CalendarAction action = item.getModelObject();
                ExternalLink link = new ExternalLink("link", action.url(), action.label());
                item.add(link);
            }
        });
    }
}