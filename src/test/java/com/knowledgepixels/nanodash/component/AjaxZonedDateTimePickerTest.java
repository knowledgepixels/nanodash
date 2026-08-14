package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDateTimePicker;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the time zone preselection of {@link AjaxZonedDateTimePicker}.
 */
public class AjaxZonedDateTimePickerTest {

    private static final String DATE_PATTERN = "d MMM yyyy";
    private static final String TIME_PATTERN = "HH:mm:ss";

    private static final Instant SUMMER = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant WINTER = Instant.parse("2026-01-01T12:00:00Z");

    private WicketTester tester;
    private TimeZone defaultTimeZone;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        defaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
        tester.destroy();
    }

    private AjaxZonedDateTimePicker newPicker(ZonedDateTime value) {
        IModel<ZonedDateTime> model = Model.of(value);
        return new AjaxZonedDateTimePicker("zoned-datetime", model, DATE_PATTERN, TIME_PATTERN);
    }

    private List<? extends ZoneId> zoneChoices() {
        return newPicker(null).getZoneDropDown().getChoices();
    }

    @Test
    void zoneIsPreselectedForNewValues() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(null);
        ZoneId selected = picker.getZoneDropDown().getModelObject();
        assertNotNull(selected, "a time zone must be preselected");
        assertEquals(ZoneId.of("Europe/Rome").getRules().getOffset(Instant.now()), selected);
    }

    @Test
    void zoneOfExistingValueIsKept() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(ZonedDateTime.parse("2020-05-17T10:15:00-04:00"));
        assertEquals(ZoneOffset.ofHours(-4), picker.getZoneDropDown().getModelObject());
    }

    @Test
    void resolveZoneChoiceFollowsDaylightSavingTime() {
        List<? extends ZoneId> choices = zoneChoices();
        ZoneId rome = ZoneId.of("Europe/Rome");
        assertEquals(ZoneOffset.ofHours(2), AjaxZonedDateTimePicker.resolveZoneChoice(rome, choices, SUMMER));
        assertEquals(ZoneOffset.ofHours(1), AjaxZonedDateTimePicker.resolveZoneChoice(rome, choices, WINTER));
    }

    @Test
    void resolveZoneChoiceHandlesOffsetsAndUnknownZones() {
        List<? extends ZoneId> choices = zoneChoices();
        assertEquals(ZoneOffset.UTC, AjaxZonedDateTimePicker.resolveZoneChoice(ZoneOffset.UTC, choices, SUMMER));
        assertEquals(ZoneOffset.ofHours(-4), AjaxZonedDateTimePicker.resolveZoneChoice(ZoneOffset.ofHours(-4), choices, SUMMER));
        assertNull(AjaxZonedDateTimePicker.resolveZoneChoice(null, choices, SUMMER));
        // An offset that no zone has as its standard offset cannot be matched:
        assertNull(AjaxZonedDateTimePicker.resolveZoneChoice(ZoneOffset.ofHoursMinutes(7, 7), choices, SUMMER));
    }

    @Test
    void pickedDateGetsThePreselectedZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        AjaxDateTimePicker dateTimePicker = (AjaxDateTimePicker) picker.getDateTimePicker();
        dateTimePicker.setModelObject(Date.from(LocalDateTime.of(2026, 5, 17, 10, 15).atZone(ZoneId.systemDefault()).toInstant()));
        // Simulate the user picking a date/time without touching the time zone dropdown:
        dateTimePicker.onValueChanged(null);
        ZonedDateTime value = picker.getModelObject();
        assertNotNull(value, "picking a date must yield a value even if the zone is left untouched");
        assertEquals(ZoneId.of("Europe/Rome").getRules().getOffset(Instant.now()), value.getZone());
        assertEquals(LocalDateTime.of(2026, 5, 17, 10, 15), value.toLocalDateTime());
    }

    @Test
    void browserZoneReplacesThePreselectedZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        List<AjaxClientInfoBehavior> behaviors = picker.getBehaviors(AjaxClientInfoBehavior.class);
        assertEquals(1, behaviors.size(), "the browser's time zone must be requested when it is unknown");
        tester.getRequest().setParameter("jsTimeZone", "America/New_York");
        tester.executeBehavior(behaviors.get(0));
        assertEquals(ZoneId.of("America/New_York").getRules().getOffset(Instant.now()), picker.getZoneDropDown().getModelObject());
        // Now that the browser has reported its time zone, it is used right away and not asked again:
        assertEquals(ZoneId.of("America/New_York"), AjaxZonedDateTimePicker.getClientZoneId());
        AjaxZonedDateTimePicker nextPicker = newPicker(null);
        assertEquals(ZoneId.of("America/New_York").getRules().getOffset(Instant.now()), nextPicker.getZoneDropDown().getModelObject());
        assertTrue(nextPicker.getBehaviors(AjaxClientInfoBehavior.class).isEmpty());
    }

    @Test
    void clientZoneIsUnknownOutsideOfARequest() {
        assertNull(AjaxZonedDateTimePicker.getClientZoneId());
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        assertEquals(ZoneId.of("Europe/Rome"), AjaxZonedDateTimePicker.getUserZoneId());
    }

}
