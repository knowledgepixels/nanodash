package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
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
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the time zone preselection of {@link AjaxZonedDateTimePicker}.
 */
public class AjaxZonedDateTimePickerTest {

    private static final String DATE_PATTERN = "d MMM yyyy";
    private static final String TIME_PATTERN = "HH:mm";


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
    void unusualOffsetOfExistingValueIsAddedToTheChoices() {
        AjaxZonedDateTimePicker picker = newPicker(ZonedDateTime.parse("2020-05-17T10:15:00+01:23"));
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(1, 23);
        assertEquals(offset, picker.getZoneDropDown().getModelObject());
        assertTrue(picker.getZoneDropDown().getChoices().contains(offset), "the value's offset must be selectable");
    }

    @Test
    void choicesAreOrderedByOffset() {
        List<? extends ZoneId> choices = zoneChoices();
        List<Integer> offsets = choices.stream().map(zone -> ((ZoneOffset) zone).getTotalSeconds()).toList();
        assertEquals(offsets.stream().sorted().toList(), offsets);
        assertTrue(choices.contains(ZoneOffset.UTC));
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
        assertEquals(ZoneOffset.ofHours(2), value.getZone(), "Rome is at summer time in May");
        assertEquals(LocalDateTime.of(2026, 5, 17, 10, 15), value.toLocalDateTime());
    }

    @Test
    void thePreselectedZoneFollowsThePickedDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        AjaxDateTimePicker dateTimePicker = (AjaxDateTimePicker) picker.getDateTimePicker();
        dateTimePicker.setModelObject(Date.from(LocalDateTime.of(2026, 1, 17, 10, 15).atZone(ZoneId.systemDefault()).toInstant()));
        dateTimePicker.onValueChanged(null);

        assertEquals(ZoneOffset.ofHours(1), picker.getModelObject().getZone(), "a January date gets winter time, whenever it is entered");
        assertEquals(ZoneOffset.ofHours(1), picker.getZoneDropDown().getModelObject());
    }

    @Test
    void aZonePickedByTheUserIsNotMoved() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(ZonedDateTime.parse("2026-07-17T10:15:00-04:00"));
        tester.startComponentInPage(picker);
        AjaxDateTimePicker dateTimePicker = (AjaxDateTimePicker) picker.getDateTimePicker();
        dateTimePicker.setModelObject(Date.from(LocalDateTime.of(2026, 1, 17, 10, 15).atZone(ZoneId.systemDefault()).toInstant()));
        dateTimePicker.onValueChanged(null);

        assertEquals(ZoneOffset.ofHours(-4), picker.getModelObject().getZone());
    }

    @Test
    void browserZoneReplacesThePreselectedZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        List<AjaxClientInfoBehavior> behaviors = picker.getZoneDropDown().getParent().getBehaviors(AjaxClientInfoBehavior.class);
        assertEquals(1, behaviors.size(), "the browser's time zone must be requested when it is unknown");
        tester.getRequest().setParameter("jsTimeZone", "America/New_York");
        tester.executeBehavior(behaviors.get(0));
        assertEquals(ZoneId.of("America/New_York").getRules().getOffset(Instant.now()), picker.getZoneDropDown().getModelObject());
        // Now that the browser has reported its time zone, it is used right away and not asked again:
        assertEquals(ZoneId.of("America/New_York"), ZoneChoice.getClientZoneId());
        AjaxZonedDateTimePicker nextPicker = newPicker(null);
        assertEquals(ZoneId.of("America/New_York").getRules().getOffset(Instant.now()), nextPicker.getZoneDropDown().getModelObject());
        assertTrue(nextPicker.getZoneDropDown().getParent().getBehaviors(AjaxClientInfoBehavior.class).isEmpty());
    }

    @Test
    void secondsAreNotAskedForByDefault() {
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        assertEquals("d MMM yyyy HH:mm", picker.getTextFormat());
        // A time picked without seconds is a time at zero seconds:
        AjaxDateTimePicker dateTimePicker = (AjaxDateTimePicker) picker.getDateTimePicker();
        dateTimePicker.setModelObject(Date.from(LocalDateTime.of(2026, 5, 17, 10, 15).atZone(ZoneId.systemDefault()).toInstant()));
        dateTimePicker.onValueChanged(null);
        assertEquals(0, picker.getModelObject().getSecond());
    }

    @Test
    void secondsAreAskedForOnceTheValueHasThem() {
        assertEquals("d MMM yyyy HH:mm:ss", newPicker(ZonedDateTime.parse("2020-05-17T10:15:30Z")).getTextFormat());
        // A value that arrives after the picker was built, as when filling in an existing nanopub:
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        assertEquals("d MMM yyyy HH:mm", picker.getTextFormat());
        picker.setModelObject(ZonedDateTime.parse("2020-05-17T10:15:30Z"));
        tester.startComponentInPage(picker);
        assertEquals("d MMM yyyy HH:mm:ss", picker.getTextFormat());
        tester.assertContains("10:15:30");
    }

    @Test
    void typedSecondsAreTakenByAFieldThatOnlyAsksForMinutes() {
        AjaxZonedDateTimePicker picker = newPicker(null);
        tester.startComponentInPage(picker);
        IConverter<Date> converter = picker.getDateTimePicker().getConverter(Date.class);
        assertEquals(LocalDateTime.of(2026, 5, 17, 10, 15, 0), toLocalDateTime(converter.convertToObject("17 May 2026 10:15", Locale.US)));
        assertEquals(LocalDateTime.of(2026, 5, 17, 10, 15, 30), toLocalDateTime(converter.convertToObject("17 May 2026 10:15:30", Locale.US)));
        assertNull(converter.convertToObject("", Locale.US));
        assertThrows(ConversionException.class, () -> converter.convertToObject("17 May 2026 10:15 pm-ish", Locale.US));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

}
