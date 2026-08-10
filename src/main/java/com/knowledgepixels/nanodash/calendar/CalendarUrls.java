package com.knowledgepixels.nanodash.calendar;

import com.knowledgepixels.nanodash.Utils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds the external calendar URLs offered in the UI.
 *
 * <p>Two different things happen here, and the difference matters for the user:
 * the <em>add</em> URLs hand a single event over to a web calendar as a one-off copy,
 * whereas the <em>subscribe</em> URLs point the calendar at a Nanodash feed it will keep
 * re-fetching, so later changes to the event dates reach the user. Only the latter can
 * ever notify anyone of a rescheduling.</p>
 */
public final class CalendarUrls {

    private static final DateTimeFormatter GOOGLE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter GOOGLE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUTLOOK_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter OUTLOOK_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private CalendarUrls() {
    }

    /**
     * A pre-filled Google Calendar "create event" form for a single event.
     *
     * @param event the event to add
     * @return the Google Calendar URL
     */
    public static String addToGoogle(CalendarEvent event) {
        String dates = event.isAllDay()
                ? event.getStartDate().format(GOOGLE_DATE) + "/" + event.getEndDateExclusive().format(GOOGLE_DATE)
                : event.getStart().withOffsetSameInstant(ZoneOffset.UTC).format(GOOGLE_TIMESTAMP) + "/"
                  + event.getEnd().withOffsetSameInstant(ZoneOffset.UTC).format(GOOGLE_TIMESTAMP);
        StringBuilder url = new StringBuilder("https://calendar.google.com/calendar/render?action=TEMPLATE")
                .append("&dates=").append(Utils.urlEncode(dates))
                .append("&text=").append(Utils.urlEncode(event.getSummary()));
        String details = detailsWithLink(event);
        if (details != null) url.append("&details=").append(Utils.urlEncode(details));
        if (notBlank(event.getLocation())) url.append("&location=").append(Utils.urlEncode(event.getLocation()));
        return url.toString();
    }

    /**
     * A pre-filled Outlook.com "create event" form for a single event.
     *
     * @param event the event to add
     * @return the Outlook Calendar URL
     */
    public static String addToOutlook(CalendarEvent event) {
        StringBuilder url = new StringBuilder("https://outlook.live.com/calendar/0/deeplink/compose?path=%2Fcalendar%2Faction%2Fcompose&rru=addevent");
        if (event.isAllDay()) {
            url.append("&allday=true")
                    .append("&startdt=").append(Utils.urlEncode(event.getStartDate().format(OUTLOOK_DATE)))
                    .append("&enddt=").append(Utils.urlEncode(event.getEndDateExclusive().format(OUTLOOK_DATE)));
        } else {
            url.append("&startdt=").append(Utils.urlEncode(event.getStart().withOffsetSameInstant(ZoneOffset.UTC).format(OUTLOOK_TIMESTAMP)))
                    .append("&enddt=").append(Utils.urlEncode(event.getEnd().withOffsetSameInstant(ZoneOffset.UTC).format(OUTLOOK_TIMESTAMP)));
        }
        url.append("&subject=").append(Utils.urlEncode(event.getSummary()));
        String details = detailsWithLink(event);
        if (details != null) url.append("&body=").append(Utils.urlEncode(details));
        if (notBlank(event.getLocation())) url.append("&location=").append(Utils.urlEncode(event.getLocation()));
        return url.toString();
    }

    /**
     * The {@code webcal:} form of a feed URL. Calendar applications register this scheme,
     * so following it opens the subscription dialog rather than downloading a static copy —
     * the distinction between "a snapshot of today's dates" and "a calendar that keeps up".
     *
     * @param feedUrl the absolute {@code http(s)} feed URL
     * @return the same URL with a {@code webcal} scheme
     */
    public static String asWebcal(String feedUrl) {
        return feedUrl.replaceFirst("^https?://", "webcal://");
    }

    /**
     * Google Calendar's "add calendar from URL" entry point, pre-filled with the feed.
     *
     * <p>Google re-fetches subscribed URLs on its own schedule, which is measured in hours
     * and cannot be influenced by the feed's refresh hints; changed dates arrive, but not
     * promptly.</p>
     *
     * @param feedUrl the absolute {@code http(s)} feed URL
     * @return the Google Calendar subscription URL
     */
    public static String subscribeInGoogle(String feedUrl) {
        return "https://calendar.google.com/calendar/r?cid=" + Utils.urlEncode(asWebcal(feedUrl));
    }

    /**
     * Outlook.com's "subscribe from web" entry point, pre-filled with the feed.
     *
     * @param feedUrl      the absolute {@code http(s)} feed URL
     * @param calendarName the name to suggest for the subscribed calendar
     * @return the Outlook Calendar subscription URL
     */
    public static String subscribeInOutlook(String feedUrl, String calendarName) {
        return "https://outlook.live.com/calendar/0/addfromweb?url=" + Utils.urlEncode(feedUrl)
               + "&name=" + Utils.urlEncode(calendarName == null ? "Nanodash" : calendarName);
    }

    /**
     * The event description as handed to a web calendar: the space description, followed by
     * a link back to the Nanodash page so the copied-out event stays traceable to its source.
     */
    private static String detailsWithLink(CalendarEvent event) {
        boolean hasDescription = notBlank(event.getDescription());
        boolean hasUrl = notBlank(event.getUrl());
        if (!hasDescription && !hasUrl) return null;
        if (!hasUrl) return event.getDescription();
        if (!hasDescription) return event.getUrl();
        return event.getDescription() + "\n\n" + event.getUrl();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

}
