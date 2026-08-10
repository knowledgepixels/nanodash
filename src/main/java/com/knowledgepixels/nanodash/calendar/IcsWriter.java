package com.knowledgepixels.nanodash.calendar;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serialises {@link CalendarEvent}s as an RFC 5545 {@code text/calendar} document.
 *
 * <p>The same output serves both uses: a one-shot {@code .ics} download and a subscribed
 * feed that clients re-fetch. What makes the second work is that a re-fetched event keeps
 * its {@link CalendarEvent#getUid() UID} and raises its {@code SEQUENCE}, which is how a
 * calendar client tells "this is the event you already have, with a new date" from "this
 * is a new event". Feeds additionally advertise a polling hint via {@code X-PUBLISHED-TTL}
 * and {@code REFRESH-INTERVAL} — a hint only: the actual refresh cadence is the client's
 * decision (Apple honours it, Outlook polls a few times a day, Google is slower still).</p>
 */
public final class IcsWriter {

    /** Product identifier announced in the calendar; identifies the generator, not the data. */
    private static final String PRODID = "-//Knowledge Pixels//Nanodash//EN";

    /** How often subscribers are asked to re-fetch a feed. Advisory; clients may ignore it. */
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private static final DateTimeFormatter UTC_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Content lines are folded to at most this many octets, per RFC 5545 section 3.1. */
    private static final int MAX_LINE_OCTETS = 75;

    private IcsWriter() {
    }

    /**
     * Writes a calendar containing the given events.
     *
     * @param name        the calendar name shown by subscribing clients, or null
     * @param description the calendar description shown by subscribing clients, or null
     * @param events      the events; an empty list yields a valid, empty calendar
     * @param asFeed      true to advertise the refresh hints of a subscribed feed
     * @return the iCalendar document
     */
    public static String write(String name, String description, List<CalendarEvent> events, boolean asFeed) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        line(sb, "VERSION", "2.0");
        line(sb, "PRODID", PRODID);
        line(sb, "CALSCALE", "GREGORIAN");
        line(sb, "METHOD", "PUBLISH");
        if (name != null && !name.isBlank()) line(sb, "X-WR-CALNAME", escape(name));
        if (description != null && !description.isBlank()) line(sb, "X-WR-CALDESC", escape(description));
        if (asFeed) {
            String duration = REFRESH_INTERVAL.toString();
            line(sb, "REFRESH-INTERVAL;VALUE=DURATION", duration);
            line(sb, "X-PUBLISHED-TTL", duration);
        }
        String dtStamp = OffsetDateTime.now(ZoneOffset.UTC).format(UTC_TIMESTAMP);
        for (CalendarEvent event : events) {
            writeEvent(sb, event, dtStamp);
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static void writeEvent(StringBuilder sb, CalendarEvent event, String dtStamp) {
        sb.append("BEGIN:VEVENT\r\n");
        line(sb, "UID", escape(event.getUid()));
        line(sb, "DTSTAMP", dtStamp);
        line(sb, "SEQUENCE", Long.toString(sequenceOf(event)));
        line(sb, "LAST-MODIFIED", event.getLastModified().withOffsetSameInstant(ZoneOffset.UTC).format(UTC_TIMESTAMP));
        if (event.isAllDay()) {
            line(sb, "DTSTART;VALUE=DATE", event.getStartDate().format(DATE));
            line(sb, "DTEND;VALUE=DATE", event.getEndDateExclusive().format(DATE));
        } else {
            line(sb, "DTSTART", event.getStart().withOffsetSameInstant(ZoneOffset.UTC).format(UTC_TIMESTAMP));
            line(sb, "DTEND", event.getEnd().withOffsetSameInstant(ZoneOffset.UTC).format(UTC_TIMESTAMP));
        }
        line(sb, "SUMMARY", escape(event.getSummary()));
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            line(sb, "DESCRIPTION", escape(event.getDescription()));
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            line(sb, "LOCATION", escape(event.getLocation()));
        }
        if (event.getUrl() != null && !event.getUrl().isBlank()) {
            // URI is not a TEXT value type (RFC 5545 section 3.3.13), so it takes no
            // backslash escaping — a URL is already percent-encoded. Escaping it would
            // write "\;" and "\," into the address itself.
            line(sb, "URL;VALUE=URI", sanitizeUri(event.getUrl()));
        }
        line(sb, "STATUS", "CONFIRMED");
        sb.append("END:VEVENT\r\n");
    }

    /**
     * The event's revision number: whole minutes since the epoch at which it was last
     * modified. {@code SEQUENCE} must be a non-negative integer that only ever increases
     * for a given UID, and clients ignore an update whose sequence has not advanced. Minute
     * granularity keeps the value well inside the 32-bit range clients assume, while still
     * advancing on every republication of the event's root definition.
     */
    private static long sequenceOf(CalendarEvent event) {
        return Math.max(0, event.getLastModified().toEpochSecond() / 60);
    }

    /**
     * Escapes a text value for RFC 5545 section 3.3.11: backslash, semicolon and comma are
     * value delimiters, and newlines are written as the literal {@code \n} sequence.
     */
    /**
     * Makes a URI safe to write as a property value without escaping it: a newline or
     * carriage return would end the content line and let the rest be read as a property of
     * its own, so those are dropped rather than encoded.
     */
    static String sanitizeUri(String uri) {
        return uri.replaceAll("[\\r\\n]", "");
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /**
     * Appends one content line, folded to {@link #MAX_LINE_OCTETS} octets. Folding counts
     * bytes rather than characters (labels and descriptions are frequently non-ASCII), and
     * never splits a multi-byte character across the fold.
     */
    private static void line(StringBuilder sb, String property, String value) {
        String content = property + ":" + value;
        StringBuilder out = new StringBuilder(content.length() + 8);
        boolean firstLine = true;
        int lineOctets = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            int width = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            // Continuation lines carry a leading space that counts towards the limit.
            int limit = firstLine ? MAX_LINE_OCTETS : MAX_LINE_OCTETS - 1;
            if (lineOctets + width > limit) {
                out.append("\r\n ");
                firstLine = false;
                lineOctets = 0;
            }
            out.append(content, i, i + charCount);
            lineOctets += width;
            i += charCount;
        }
        sb.append(out).append("\r\n");
    }

}
