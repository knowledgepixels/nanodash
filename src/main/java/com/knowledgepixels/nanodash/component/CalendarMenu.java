package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.calendar.CalendarEvent;
import com.knowledgepixels.nanodash.calendar.CalendarUrls;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.CalendarFeedPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * A dropdown of calendar actions, shown next to a resource title.
 *
 * <p>Unlike the per-entry menus inside view results, this one carries a visible label
 * ("Add to calendar", "Subscribe to events") rather than a bare chevron: it is the primary
 * offer on an event page, not a secondary action on a row.</p>
 *
 * <p>Each supplied link must use the markup id {@code "link"}; the links are rendered in
 * order as menu entries.</p>
 */
public class CalendarMenu extends Panel {

    /**
     * @param id      the Wicket component id
     * @param label   the text on the dropdown trigger
     * @param entries the menu entries, each an {@link AbstractLink} with markup id {@code "link"}
     */
    public CalendarMenu(String id, String label, List<AbstractLink> entries) {
        super(id);
        add(new Label("buttonLabel", label));
        add(new DataView<AbstractLink>("entries", new ListDataProvider<>(entries)) {
            @Override
            protected void populateItem(Item<AbstractLink> item) {
                item.add(item.getModelObject());
            }
        });
    }

    /**
     * The calendar menu for a space, or an invisible placeholder when the space has nothing
     * to offer a calendar — neither a date of its own nor any dated sub-events.
     *
     * <p>An Event space offers its own date as a one-off copy (an {@code .ics} file, or a
     * pre-filled form at Google or Outlook). A space containing Events offers a
     * <em>subscription</em> to the series instead, which is the only form that keeps up when
     * an event is later rescheduled. A space that is both gets both sets of entries.</p>
     *
     * @param id    the Wicket component id
     * @param space the space to build the menu for
     * @return the menu, or an invisible {@link EmptyPanel}
     */
    public static Component forSpace(String id, Space space) {
        List<AbstractLink> entries = new ArrayList<>();

        CalendarEvent ownEvent = CalendarFeedPage.isEvent(space)
                ? CalendarEvent.fromSpace(space, SpacePage.urlFor(space.getId())) : null;
        if (ownEvent != null) {
            entries.add(link("📥", "download .ics", CalendarFeedPage.urlFor(space.getId(), true)));
            entries.add(externalLink("📅", "add to Google Calendar", CalendarUrls.addToGoogle(ownEvent)));
            entries.add(externalLink("📆", "add to Outlook", CalendarUrls.addToOutlook(ownEvent)));
        }

        boolean hasSubEvents = CalendarFeedPage.hasSubEvents(space);
        if (hasSubEvents) {
            String feedUrl = CalendarFeedPage.urlFor(space.getId(), false);
            entries.add(link("🔔", "subscribe (webcal)", CalendarUrls.asWebcal(feedUrl)));
            entries.add(externalLink("📅", "subscribe in Google Calendar", CalendarUrls.subscribeInGoogle(feedUrl)));
            entries.add(externalLink("📆", "subscribe in Outlook", CalendarUrls.subscribeInOutlook(feedUrl, space.getLabel())));
            entries.add(copyLink(feedUrl));
            entries.add(link("📥", "download all events (.ics)", CalendarFeedPage.urlFor(space.getId(), true)));
        }

        if (entries.isEmpty()) {
            return new EmptyPanel(id).setVisible(false);
        }
        String label = ownEvent != null ? (hasSubEvents ? "Calendar" : "Add to calendar") : "Subscribe to events";
        return new CalendarMenu(id, label, entries);
    }

    /**
     * An entry handled without leaving the page: a file download, or a {@code webcal:} URL
     * handed straight to the operating system's calendar application. Opening these in a new
     * tab would leave the user staring at a blank one.
     */
    private static AbstractLink link(String icon, String label, String url) {
        ExternalLink externalLink = new ExternalLink("link", url);
        externalLink.setBody(Model.of("<span class=\"actionmenu-icon\">" + icon + "</span>" + label))
                .setEscapeModelStrings(false);
        return externalLink;
    }

    /** An entry leading to a third-party web calendar, opened in a new tab. */
    private static AbstractLink externalLink(String icon, String label, String url) {
        AbstractLink externalLink = link(icon, label, url);
        externalLink.add(AttributeModifier.replace("target", "_blank"));
        externalLink.add(AttributeModifier.replace("rel", "noopener noreferrer"));
        return externalLink;
    }

    /**
     * The "copy feed URL" entry, for pasting into a calendar application that offers no
     * web-based subscription flow (Thunderbird, Apple Calendar on some platforms, most
     * self-hosted clients).
     */
    private static AbstractLink copyLink(String feedUrl) {
        AjaxLink<Void> copy = new AjaxLink<>("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String escaped = feedUrl.replace("\\", "\\\\").replace("'", "\\'");
                target.appendJavaScript(
                        "navigator.clipboard.writeText('" + escaped + "')" +
                        ".then(function() { showToast('Calendar feed URL copied to clipboard!'); })" +
                        ".catch(function(err) { console.error('Copy failed:', err); });"
                );
            }
        };
        copy.setBody(Model.of("<span class=\"actionmenu-icon\">⧉</span>copy feed URL")).setEscapeModelStrings(false);
        return copy;
    }

}
