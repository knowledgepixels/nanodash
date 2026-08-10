package com.knowledgepixels.nanodash.calendar;

import com.knowledgepixels.nanodash.domain.Space;
import jakarta.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarUrlsTest {

    private static final String FEED_URL = "https://nanodash.example.org/calendar.ics?space=https%3A%2F%2Fw3id.org%2Fnp%2Fp";

    private static CalendarEvent timedEvent() {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn("https://w3id.org/np/RAexample/bh26");
        when(space.getLabel()).thenReturn("BioHackathon 2026");
        when(space.getLocation()).thenReturn("Mishima, Japan");
        when(space.getStartDate()).thenReturn(DatatypeConverter.parseDateTime("2026-07-14T16:00:00+02:00"));
        when(space.getEndDate()).thenReturn(DatatypeConverter.parseDateTime("2026-07-14T17:00:00+02:00"));
        return CalendarEvent.fromSpace(space, "https://nanodash.example.org/space?id=x");
    }

    private static CalendarEvent allDayEvent() {
        Space space = mock(Space.class);
        when(space.getId()).thenReturn("https://w3id.org/np/RAexample/bh26");
        when(space.getLabel()).thenReturn("BioHackathon 2026");
        when(space.getStartDate()).thenReturn(DatatypeConverter.parseDate("2026-07-14"));
        when(space.getEndDate()).thenReturn(DatatypeConverter.parseDate("2026-07-17"));
        when(space.isStartDateOnly()).thenReturn(true);
        when(space.isEndDateOnly()).thenReturn(true);
        return CalendarEvent.fromSpace(space, null);
    }

    @Test
    void googleGetsUtcTimestampsForATimedEvent() {
        String url = CalendarUrls.addToGoogle(timedEvent());
        assertTrue(url.contains("dates=20260714T140000Z%2F20260714T150000Z"), url);
        assertTrue(url.contains("text=BioHackathon+2026"), url);
        assertTrue(url.contains("location=Mishima%2C+Japan"), url);
    }

    @Test
    void googleGetsPlainDatesForAnAllDayEvent() {
        String url = CalendarUrls.addToGoogle(allDayEvent());
        // The end date stays exclusive, matching the DTEND written into the .ics.
        assertTrue(url.contains("dates=20260714%2F20260718"), url);
    }

    @Test
    void outlookMarksAllDayEventsAsSuch() {
        assertTrue(CalendarUrls.addToOutlook(allDayEvent()).contains("&allday=true"));
        assertFalse(CalendarUrls.addToOutlook(timedEvent()).contains("&allday=true"));
    }

    @Test
    void webcalSchemeReplacesHttpSoClientsSubscribeRatherThanDownload() {
        assertEquals("webcal://example.org/calendar.ics", CalendarUrls.asWebcal("https://example.org/calendar.ics"));
        assertEquals("webcal://example.org/calendar.ics", CalendarUrls.asWebcal("http://example.org/calendar.ics"));
    }

    @Test
    void googleSubscriptionCarriesTheWebcalFeedUrl() {
        String url = CalendarUrls.subscribeInGoogle(FEED_URL);
        assertTrue(url.startsWith("https://calendar.google.com/calendar/r?cid="), url);
        assertTrue(url.contains("webcal%3A%2F%2F"), url);
    }

    @Test
    void outlookSubscriptionCarriesTheHttpFeedUrlAndAName() {
        String url = CalendarUrls.subscribeInOutlook(FEED_URL, "My Program");
        assertTrue(url.startsWith("https://outlook.live.com/calendar/0/addfromweb?url="), url);
        assertTrue(url.contains("name=My+Program"), url);
    }

}
