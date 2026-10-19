package com.knowledgepixels.nanodash.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the timezones {@link ZoneChoice} offers and how it names them.
 */
class ZoneChoiceTest {

    private static final Instant SUMMER = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant WINTER = Instant.parse("2026-01-01T12:00:00Z");

    private TimeZone defaultTimeZone;

    @BeforeEach
    void setUp() {
        defaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    void zonesAreGroupedByTheOffsetTheyAreCurrentlyAt() {
        ZoneId rome = ZoneId.of("Europe/Rome");
        assertTrue(ZoneChoice.getZonesByCurrentOffset(SUMMER).get(ZoneOffset.ofHours(2)).contains(rome));
        assertTrue(ZoneChoice.getZonesByCurrentOffset(WINTER).get(ZoneOffset.ofHours(1)).contains(rome));
        assertFalse(ZoneChoice.getZonesByCurrentOffset(SUMMER).get(ZoneOffset.ofHours(1)).contains(rome));
    }

    @Test
    void legacyAndArtificialZonesAreLeftOut() {
        List<String> ids = ZoneChoice.getZonesByCurrentOffset(SUMMER).values().stream()
                .flatMap(List::stream).map(ZoneId::getId).toList();
        assertTrue(ids.contains("America/New_York"));
        for (String excluded : new String[]{"US/Eastern", "Etc/GMT+3", "CET", "Japan", "UTC"}) {
            assertFalse(ids.contains(excluded), excluded + " must not be used as an example zone");
        }
    }

    @Test
    void labelsNameThePlacesAtTheOffset() {
        ZoneOffset summerOffsetOfRome = ZoneOffset.ofHours(2);
        List<ZoneId> atOffset = ZoneChoice.getZonesByCurrentOffset(SUMMER).get(summerOffsetOfRome);
        assertEquals("UTC+02:00 — Paris, Berlin, Madrid",
                ZoneChoice.getZoneChoiceLabel(summerOffsetOfRome, atOffset, null));
        // The user's own place is named first, so that they recognize their preselected zone:
        assertEquals("UTC+02:00 — Rome, Paris, Berlin",
                ZoneChoice.getZoneChoiceLabel(summerOffsetOfRome, atOffset, ZoneId.of("Europe/Rome")));
    }

    @Test
    void labelsNameNeitherDuplicateNorObscurePlaces() {
        Map<ZoneOffset, List<ZoneId>> groups = ZoneChoice.getZonesByCurrentOffset(SUMMER);
        // Asia/Calcutta is a duplicate of Asia/Kolkata, and there are a dozen more zones at +05:30:
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        assertEquals("UTC+05:30 — Kolkata, Colombo", ZoneChoice.getZoneChoiceLabel(offset, groups.get(offset), null));
        // Every offset a place is at is named by at least one of them:
        for (Map.Entry<ZoneOffset, List<ZoneId>> group : groups.entrySet()) {
            String label = ZoneChoice.getZoneChoiceLabel(group.getKey(), group.getValue(), null);
            assertTrue(label.contains(" — "), "no place is named for " + group.getKey() + ": " + label);
        }
    }

    @Test
    void offsetsAreLabelledAsUtcOffsets() {
        assertEquals("UTC", ZoneChoice.getOffsetLabel(ZoneOffset.UTC));
        assertEquals("UTC+05:30", ZoneChoice.getOffsetLabel(ZoneOffset.ofHoursMinutes(5, 30)));
        assertEquals("UTC-03:30", ZoneChoice.getOffsetLabel(ZoneOffset.ofHoursMinutes(-3, -30)));
        // Offsets without any place at them are labelled by the offset alone:
        assertEquals("UTC+01:23", ZoneChoice.getZoneChoiceLabel(ZoneOffset.ofHoursMinutes(1, 23), List.of(), null));
    }

    @Test
    void clientZoneIsUnknownOutsideOfARequest() {
        assertNull(ZoneChoice.getClientZoneId());
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        assertEquals(ZoneId.of("Europe/Rome"), ZoneChoice.getUserZoneId());
    }

    @Test
    void theUsersOffsetIsTheOneAtTheDateGiven() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));

        assertEquals(ZoneOffset.ofHours(1), ZoneChoice.getUserOffsetAt(LocalDateTime.of(2026, 1, 17, 10, 0)));
        assertEquals(ZoneOffset.ofHours(2), ZoneChoice.getUserOffsetAt(LocalDateTime.of(2026, 7, 17, 10, 0)));
        assertEquals(ZoneId.of("Europe/Rome").getRules().getOffset(Instant.now()), ZoneChoice.getUserOffsetAt(null));
    }

}
