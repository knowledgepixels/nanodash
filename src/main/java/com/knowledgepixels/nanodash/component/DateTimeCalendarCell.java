package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A cell component that renders a human-readable datetime range value as-is,
 * with a dropdown offering an .ics download and a Google Calendar link.
 *
 * <p>The display string must match {@link #DISPLAY_PATTERN}, e.g.:
 * {@code 2026-07-14, 16:00–17:00 (UTC+02:00)}</p>
 */
public class DateTimeCalendarCell extends Panel {

    public static final Pattern DISPLAY_PATTERN = Pattern.compile(
            "^\\s*(\\d{4}-\\d{2}-\\d{2})\\s*,\\s*([01]\\d|2[0-3]):([0-5]\\d)\\s*[-–—]\\s*([01]\\d|2[0-3]):([0-5]\\d)\\s*\\(UTC([+-])([01]\\d|2[0-3]):([0-5]\\d)\\)\\s*$"
    );

    private static final DateTimeFormatter ICS_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public record ParsedEvent(OffsetDateTime start, OffsetDateTime end) {
    }

    public DateTimeCalendarCell(String id, String displayValue) {
        super(id);

        // Show the original display string unchanged.
        add(new Label("dateLabel", displayValue));

        ParsedEvent event = parseDisplayString(displayValue);

        List<CalendarAction> actions = new ArrayList<>();
        actions.add(new CalendarAction("📥 Download .ics", buildIcsDataUri(event, displayValue)));
        actions.add(new CalendarAction("📅 Add to Google Calendar", buildGoogleCalendarUrl(event, displayValue)));

        add(new CalendarDropdown("calendarMenu", actions));
    }

    /**
     * Parses a display string matching {@link #DISPLAY_PATTERN} into start/end
     * {@link OffsetDateTime} values. Overnight ranges (end &le; start) are shifted
     * forward by one day.
     *
     * @throws IllegalArgumentException if the string does not match the pattern
     */
    public static ParsedEvent parseDisplayString(String input) {
        Matcher m = DISPLAY_PATTERN.matcher(input);
        if (!m.matches()) {
            throw new IllegalArgumentException("Does not match display date pattern: " + input);
        }

        LocalDate date = LocalDate.parse(m.group(1));
        LocalTime startTime = LocalTime.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        LocalTime endTime = LocalTime.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)));

        String sign = m.group(6);
        int offH = Integer.parseInt(m.group(7));
        int offM = Integer.parseInt(m.group(8));
        ZoneOffset offset = ZoneOffset.of(sign + String.format("%02d:%02d", offH, offM));

        OffsetDateTime startOdt = OffsetDateTime.of(date, startTime, offset);
        OffsetDateTime endOdt = OffsetDateTime.of(date, endTime, offset);

        if (!endOdt.isAfter(startOdt)) {
            endOdt = endOdt.plusDays(1); // overnight event
        }

        return new ParsedEvent(startOdt, endOdt);
    }

    private static String buildIcsDataUri(ParsedEvent event, String displayValue) {
        String ics = buildIcsContent(event, displayValue);
        String b64 = Base64.getEncoder().encodeToString(ics.getBytes(StandardCharsets.UTF_8));
        return "data:text/calendar;charset=utf-8;base64," + b64;
    }

    private static String buildIcsContent(ParsedEvent event, String summary) {
        String dtStart = event.start().withOffsetSameInstant(ZoneOffset.UTC).format(ICS_UTC);
        String dtEnd = event.end().withOffsetSameInstant(ZoneOffset.UTC).format(ICS_UTC);
        String dtStamp = OffsetDateTime.now(ZoneOffset.UTC).format(ICS_UTC);
        String uid = UUID.randomUUID() + "@nanodash";
        String safeSummary = icsEscape(summary);

        return "BEGIN:VCALENDAR\r\n"
               + "VERSION:2.0\r\n"
               + "PRODID:-//nanodash//EN\r\n"
               + "CALSCALE:GREGORIAN\r\n"
               + "METHOD:PUBLISH\r\n"
               + "BEGIN:VEVENT\r\n"
               + "UID:" + uid + "\r\n"
               + "DTSTAMP:" + dtStamp + "\r\n"
               + "DTSTART:" + dtStart + "\r\n"
               + "DTEND:" + dtEnd + "\r\n"
               + "SUMMARY:" + safeSummary + "\r\n"
               + "END:VEVENT\r\n"
               + "END:VCALENDAR\r\n";
    }

    private static String icsEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private static String buildGoogleCalendarUrl(ParsedEvent event, String summary) {
        // Google Calendar format: yyyyMMdd'T'HHmmss'Z'
        String dtStart = event.start().withOffsetSameInstant(ZoneOffset.UTC).format(ICS_UTC);
        String dtEnd = event.end().withOffsetSameInstant(ZoneOffset.UTC).format(ICS_UTC);
        String dates = dtStart + "/" + dtEnd;
        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
               + "&dates=" + URLEncoder.encode(dates, StandardCharsets.UTF_8)
               + "&text=" + URLEncoder.encode(summary, StandardCharsets.UTF_8);
    }

    public record CalendarAction(String label, String url) implements Serializable {}

}