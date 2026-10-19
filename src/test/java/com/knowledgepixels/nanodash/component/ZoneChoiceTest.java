package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
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
    private WicketTester tester;

    @BeforeEach
    void setUp() {
        defaultTimeZone = TimeZone.getDefault();
        tester = new WicketTester(new WicketApplication());
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
        tester.destroy();
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

    private static ZoneChoice newChoice() {
        return new ZoneChoice("zone", Model.of((ZoneId) null));
    }

    private static ZoneOffset offsetNowOf(String zone) {
        return ZoneId.of(zone).getRules().getOffset(Instant.now());
    }

    @Test
    void anEmptySearchOffersEveryTimezoneWestToEast() {
        ZoneChoice choice = newChoice();
        List<ZoneId> all = choice.search("");

        assertEquals(choice.getChoices(), all);
        assertEquals(choice.getChoices(), choice.search(null));
        assertTrue(all.size() > 30);
    }

    @Test
    void aTimezoneIsFoundByAnyPlaceAtItsOffset() {
        ZoneChoice choice = newChoice();

        assertTrue(choice.search("tokyo").contains(offsetNowOf("Asia/Tokyo")));
        assertTrue(choice.search("Zurich").contains(offsetNowOf("Europe/Zurich")),
                "a place the label leaves out is found all the same");
        assertTrue(choice.search("sao paulo").contains(offsetNowOf("America/Sao_Paulo")),
                "without its accent or underscore");
        assertEquals(List.of(offsetNowOf("Asia/Kathmandu")), choice.search("kathmandu"));
    }

    @Test
    void aTimezoneIsFoundByItsOffsetHoweverItIsWritten() {
        ZoneChoice choice = newChoice();
        ZoneOffset kolkata = ZoneOffset.ofHoursMinutes(5, 30);
        ZoneOffset newYorkWinter = ZoneOffset.ofHours(-5);

        for (String term : List.of("+05:30", "+5:30", "UTC+5:30", "gmt +05:30", "+0530")) {
            assertEquals(List.of(kolkata), choice.search(term), term);
        }
        for (String term : List.of("-5", "UTC-5", "GMT-05", "-05:00")) {
            assertTrue(choice.search(term).contains(newYorkWinter), term);
        }
        assertEquals(List.of(ZoneOffset.UTC), choice.search("utc"), "not every label that starts with UTC");
        assertEquals(List.of(ZoneOffset.UTC), choice.search("GMT"));
        assertEquals(List.of(ZoneOffset.UTC), choice.search("+0"));
        assertEquals(List.of(ZoneOffset.ofHours(1)), choice.search("+1"), "not +10:00 to +14:00 as well");
        assertTrue(choice.search("+05:").containsAll(List.of(kolkata, ZoneOffset.ofHoursMinutes(5, 45))),
                "an offset still being typed finds the ones it begins");
    }

    @Test
    void aSearchThatMatchesNothingOffersNothing() {
        assertTrue(newChoice().search("atlantis").isEmpty());
    }

    @Test
    void anOffsetNoPlaceIsAtIsOfferedWhileItIsTheValue() {
        ZoneOffset unusual = ZoneOffset.ofHoursMinutes(1, 23);
        ZoneChoice choice = new ZoneChoice("zone", Model.of(unusual));

        assertTrue(choice.getChoices().contains(unusual));
        assertEquals(List.of(unusual), choice.search("+01:23"));
    }

    @Test
    void thePlacesAreThoseAtEachOffsetAtTheDateEntered() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        ZoneChoice winter = new ZoneChoice("zone", Model.of((ZoneId) null)) {
            @Override
            protected LocalDateTime getDateTime() {
                return LocalDateTime.of(2026, 1, 16, 9, 30);
            }
        };
        ZoneChoice summer = new ZoneChoice("zone", Model.of((ZoneId) null)) {
            @Override
            protected LocalDateTime getDateTime() {
                return LocalDateTime.of(2026, 7, 16, 9, 30);
            }
        };

        assertTrue(winter.getDropDown().getProvider().getDisplayValue(ZoneOffset.ofHours(1)).startsWith("UTC+01:00 — Rome,"),
                "the user's own place is named first under winter time in January");
        assertTrue(winter.search("rome").contains(ZoneOffset.ofHours(1)));
        assertFalse(winter.search("london").contains(ZoneOffset.ofHours(1)), "London is at UTC in January");
        assertTrue(summer.search("rome").contains(ZoneOffset.ofHours(2)));
        assertTrue(summer.search("london").contains(ZoneOffset.ofHours(1)));
    }

    @Test
    void theUsersOffsetIsTheOneAtTheDateGiven() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));

        assertEquals(ZoneOffset.ofHours(1), ZoneChoice.getUserOffsetAt(LocalDateTime.of(2026, 1, 17, 10, 0)));
        assertEquals(ZoneOffset.ofHours(2), ZoneChoice.getUserOffsetAt(LocalDateTime.of(2026, 7, 17, 10, 0)));
        assertEquals(ZoneId.of("Europe/Rome").getRules().getOffset(Instant.now()), ZoneChoice.getUserOffsetAt(null));
    }

}
