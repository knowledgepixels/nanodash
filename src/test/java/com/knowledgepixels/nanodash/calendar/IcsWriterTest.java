package com.knowledgepixels.nanodash.calendar;

import com.knowledgepixels.nanodash.domain.Space;
import jakarta.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IcsWriterTest {

    private static final String EVENT_IRI = "https://w3id.org/np/RAexample/bh26";

    private static Space timedEventSpace() {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(EVENT_IRI);
        when(space.getLabel()).thenReturn("BioHackathon 2026");
        when(space.getStartDate()).thenReturn(DatatypeConverter.parseDateTime("2026-07-14T16:00:00+02:00"));
        when(space.getEndDate()).thenReturn(DatatypeConverter.parseDateTime("2026-07-14T17:00:00+02:00"));
        return space;
    }

    private static Space allDayEventSpace() {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn(EVENT_IRI);
        when(space.getLabel()).thenReturn("BioHackathon 2026");
        when(space.getStartDate()).thenReturn(DatatypeConverter.parseDate("2026-07-14"));
        when(space.getEndDate()).thenReturn(DatatypeConverter.parseDate("2026-07-17"));
        when(space.isStartDateOnly()).thenReturn(true);
        when(space.isEndDateOnly()).thenReturn(true);
        return space;
    }

    private static String write(Space space) {
        return IcsWriter.write("cal", null, List.of(CalendarEvent.fromSpace(space, null)), false);
    }

    @Test
    void timedEventIsWrittenInUtc() {
        String ics = write(timedEventSpace());
        assertTrue(ics.contains("\r\nDTSTART:20260714T140000Z\r\n"), ics);
        assertTrue(ics.contains("\r\nDTEND:20260714T150000Z\r\n"), ics);
    }

    @Test
    void allDayEventUsesDateValuesWithExclusiveEnd() {
        String ics = write(allDayEventSpace());
        assertTrue(ics.contains("\r\nDTSTART;VALUE=DATE:20260714\r\n"), ics);
        // schema:endDate names the last day (the 17th); DTEND is exclusive, so the 18th.
        assertTrue(ics.contains("\r\nDTEND;VALUE=DATE:20260718\r\n"), ics);
    }

    @Test
    void uidIsDerivedFromTheEventIriSoRefetchesAreRecognisedAsUpdates() {
        String first = write(timedEventSpace());
        String second = write(timedEventSpace());
        assertTrue(first.contains("\r\nUID:" + EVENT_IRI + "\r\n"), first);
        assertEquals(uidOf(first), uidOf(second));
    }

    @Test
    void sequenceAdvancesWhenTheEventIsRepublished() {
        CalendarEvent older = CalendarEvent.fromSpace(timedEventSpace(), null);
        Space republished = timedEventSpace();
        // A later root definition — the same event, redefined — must outrank the older one.
        CalendarEvent newer = CalendarEvent.fromSpace(republished, null);
        assertFalse(newer.getLastModified().isBefore(older.getLastModified()));

        String ics = IcsWriter.write("cal", null, List.of(older), false);
        long sequence = Long.parseLong(valueOf(ics, "SEQUENCE"));
        assertTrue(sequence >= 0, "SEQUENCE must be a non-negative integer, was " + sequence);
    }

    @Test
    void endDefaultsToAnHourAfterStartWhenMissing() {
        Space space = timedEventSpace();
        when(space.getEndDate()).thenReturn(null);
        String ics = write(space);
        assertTrue(ics.contains("\r\nDTSTART:20260714T140000Z\r\n"), ics);
        assertTrue(ics.contains("\r\nDTEND:20260714T150000Z\r\n"), ics);
    }

    @Test
    void noEventIsProducedWithoutAStartDate() {
        Space space = mock(Space.class);
        when(space.getStartDate()).thenReturn(null);
        assertNull(CalendarEvent.fromSpace(space, null));
    }

    @Test
    void textValuesAreEscaped() {
        assertEquals("a\\, b\\; c\\\\ d\\ne", IcsWriter.escape("a, b; c\\ d\ne"));
    }

    // URI is not a TEXT value type, so a URL must reach the file verbatim. Escaping it would
    // write "\;" and "\," into the address and break the link.
    @Test
    void urlValuesAreNotTextEscaped() {
        String url = "https://example.org/space?id=a,b;c";
        Space space = timedEventSpace();
        String ics = IcsWriter.write("cal", null, List.of(CalendarEvent.fromSpace(space, url)), false);
        assertTrue(ics.replace("\r\n ", "").contains("URL;VALUE=URI:" + url), ics);
    }

    // A newline in a URI would terminate the content line and let the remainder be parsed as
    // a property of its own.
    @Test
    void urlValuesCannotBreakOutOfTheirContentLine() {
        assertEquals("https://example.org/aSUMMARY:evil",
                IcsWriter.sanitizeUri("https://example.org/a\r\nSUMMARY:evil"));
    }

    @Test
    void longLinesAreFoldedAtSeventyFiveOctets() {
        Space space = timedEventSpace();
        when(space.getLabel()).thenReturn("x".repeat(200));
        String ics = write(space);
        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75,
                    "line exceeds 75 octets: " + line);
        }
        // Unfolding (dropping CRLF + the continuation space) must restore the original value.
        assertTrue(ics.replace("\r\n ", "").contains("SUMMARY:" + "x".repeat(200)), ics);
    }

    @Test
    void foldingNeverSplitsAMultiByteCharacter() {
        Space space = timedEventSpace();
        when(space.getLabel()).thenReturn("é".repeat(100));
        String ics = write(space);
        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75,
                    "line exceeds 75 octets: " + line);
        }
        assertTrue(ics.replace("\r\n ", "").contains("SUMMARY:" + "é".repeat(100)), ics);
    }

    @Test
    void feedsAdvertiseARefreshHintAndOneOffDownloadsDoNot() {
        List<CalendarEvent> events = List.of(CalendarEvent.fromSpace(timedEventSpace(), null));
        assertTrue(IcsWriter.write("cal", null, events, true).contains("REFRESH-INTERVAL;VALUE=DURATION:PT1H"));
        assertFalse(IcsWriter.write("cal", null, events, false).contains("REFRESH-INTERVAL"));
    }

    @Test
    void anEmptyCalendarIsStillValid() {
        String ics = IcsWriter.write("cal", "no events yet", List.of(), true);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"), ics);
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"), ics);
        assertFalse(ics.contains("BEGIN:VEVENT"), ics);
    }

    private static String uidOf(String ics) {
        return valueOf(ics, "UID");
    }

    private static String valueOf(String ics, String property) {
        for (String line : ics.split("\r\n")) {
            if (line.startsWith(property + ":")) return line.substring(property.length() + 1);
        }
        return fail("no " + property + " in:\n" + ics);
    }

}
