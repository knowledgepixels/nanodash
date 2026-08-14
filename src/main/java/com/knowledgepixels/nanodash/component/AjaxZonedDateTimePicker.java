package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.ZonedDateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDateTimePicker;
import org.wicketstuff.kendo.ui.form.datetime.DateTimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class AjaxZonedDateTimePicker extends FormComponentPanel<ZonedDateTime> implements AbstractTextComponent.ITextFormatProvider {

    private final Logger logger = LoggerFactory.getLogger(AjaxZonedDateTimePicker.class);
    private IModel<ZonedDateTime> zonedDateTimeModel = Model.of((ZonedDateTime) null);
    private IModel<ZoneId> zoneIdModel = Model.of((ZoneId) null);
    private IModel<Date> dateModel = Model.of((Date) null);
    private final DropDownChoice<ZoneId> zoneDropDown;
    private final DateTimePicker dateTimePicker;
    private final List<ZoneId> zones;
    private String datePattern, timePattern;

    // True as long as the selected zone is just our default guess, i.e. neither the user nor an
    // existing value has determined it. Only then may the detected client zone still overrule it.
    private boolean zoneIsDefault;

    public AjaxZonedDateTimePicker(String id, IModel<ZonedDateTime> model, String datePattern, String timePattern) {
        super(id);
        this.setType(ZonedDateTime.class);
        this.setModel(model);
        if (model.getObject() != null) {
            dateModel.setObject(Date.from(model.getObject().toInstant()));
        }
        this.datePattern = datePattern;
        this.timePattern = timePattern;
        this.dateTimePicker = new AjaxDateTimePicker("datetime", dateModel, datePattern, timePattern) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                // Prevent Edge's autofill from overwriting the Kendo-formatted date/time values with a mismatched format:
                datePicker.add(new org.apache.wicket.AttributeModifier("autocomplete", "off"));
                datePicker.add(new org.apache.wicket.AttributeModifier("data-form-type", "other"));
                timePicker.add(new org.apache.wicket.AttributeModifier("autocomplete", "off"));
                timePicker.add(new org.apache.wicket.AttributeModifier("data-form-type", "other"));
            }
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                Date selectedDate = this.getModelObject();
                if (zoneIdModel.getObject() == null) {
                    dateTimePicker.setModelObject(selectedDate);
                    logger.info("Date selected without timezone: {}", dateModel.getObject());
                } else {
                    ZonedDateTime currentZonedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault()).atZone(zoneIdModel.getObject());
                    zonedDateTimeModel.setObject(currentZonedDateTime);
                    logger.info("Date selected: {}", dateModel.getObject());
                    logger.info("Selected datetime with current timezone: {}", zonedDateTimeModel.getObject());
                }
            }
        };

        this.zonedDateTimeModel = model;
        if (this.zonedDateTimeModel.getObject() != null) {
            this.zoneIdModel = Model.of(model.getObject().getZone());
        }

        Map<ZoneOffset, List<ZoneId>> timezoneGroups = ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .collect(Collectors.groupingBy(x -> x.getRules().getStandardOffset(Instant.now())));
        this.zones = new ArrayList<>(timezoneGroups.keySet());
        zones.sort(Comparator.comparing(zoneId -> zoneId.getRules().getStandardOffset(Instant.now()).getTotalSeconds()));

        if (zoneIdModel.getObject() == null) {
            // No zone given by an existing value, so we preselect the one the user is most likely in:
            zoneIdModel.setObject(resolveZoneChoice(getUserZoneId(), zones, Instant.now()));
            zoneIsDefault = true;
            if (getClientZoneId() == null) {
                // The client's zone isn't known yet (typically the first page view of a session), so
                // we ask the browser for it and correct our guess as soon as the answer comes in:
                add(new AjaxClientInfoBehavior() {
                    @Override
                    protected void onClientInfo(AjaxRequestTarget target, WebClientInfo clientInfo) {
                        applyClientZone(clientInfo, target);
                    }
                });
            }
        }

        this.zoneDropDown = new DropDownChoice<>("timezone-dropdown", zoneIdModel, zones,
                (IChoiceRenderer<ZoneId>) zoneId -> String.format("%s : %s", zoneId, timezoneGroups.get(zoneId).stream()
                        .map(ZoneId::getId)
                        .limit(3)
                        .collect(Collectors.joining(", ")))
        );

        this.zoneDropDown.setOutputMarkupId(true);
        this.zoneDropDown.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                zoneIsDefault = false;
                updateZoneSelection();
                ajaxRequestTarget.add(AjaxZonedDateTimePicker.this);
            }
        });
        add(zoneDropDown);
        add(dateTimePicker);
    }

    /**
     * Returns the time zone the user is most likely in: the one reported by their browser, or the
     * time zone of this Nanodash instance if the browser hasn't told us (yet). The latter is the
     * user's own time zone too when Nanodash runs locally.
     *
     * @return the user's time zone, never null
     */
    public static ZoneId getUserZoneId() {
        ZoneId clientZone = getClientZoneId();
        return clientZone != null ? clientZone : ZoneId.systemDefault();
    }

    /**
     * Returns the time zone reported by the user's browser, or null if it hasn't been collected yet.
     *
     * @return the client's time zone, or null
     */
    public static ZoneId getClientZoneId() {
        if (!Session.exists() || RequestCycle.get() == null) return null;
        if (!(Session.get().getClientInfo() instanceof WebClientInfo clientInfo)) return null;
        TimeZone timeZone = clientInfo.getProperties().getTimeZone();
        return timeZone == null ? null : timeZone.toZoneId();
    }

    /**
     * Returns the entry of the given zone choices that matches the given zone at the given instant.
     * The choices are offsets, so we match the offset the zone is actually at (including daylight
     * saving time), and fall back to its standard offset if that isn't among the choices.
     *
     * @param zone    the zone to match
     * @param choices the available zone choices
     * @param instant the instant to determine the zone's offset at
     * @return the matching choice, or null if there is none
     */
    public static ZoneId resolveZoneChoice(ZoneId zone, List<? extends ZoneId> choices, Instant instant) {
        if (zone == null) return null;
        ZoneOffset offset = zone.getRules().getOffset(instant);
        if (choices.contains(offset)) return offset;
        ZoneOffset standardOffset = zone.getRules().getStandardOffset(instant);
        if (choices.contains(standardOffset)) return standardOffset;
        return null;
    }

    /**
     * Applies the time zone the browser just reported, unless the zone was determined by the user or
     * by an existing value in the meantime.
     */
    private void applyClientZone(WebClientInfo clientInfo, AjaxRequestTarget target) {
        if (!zoneIsDefault) return;
        TimeZone timeZone = clientInfo.getProperties().getTimeZone();
        if (timeZone == null) return;
        ZoneId choice = resolveZoneChoice(timeZone.toZoneId(), zones, Instant.now());
        if (choice == null || choice.equals(zoneIdModel.getObject())) return;
        logger.info("Setting time zone to the one reported by the browser: {}", choice);
        zoneIdModel.setObject(choice);
        // The date/time fields keep the local time the user sees, so only the zone needs repainting:
        updateZoneSelection();
        target.add(zoneDropDown);
    }

    private void updateZoneSelection() {
        ZoneId selectedZone = zoneIdModel.getObject();
        logger.info("Selected time zone: {}", selectedZone);
        if (selectedZone == null) return;
        if (zonedDateTimeModel.getObject() == null) {
            if (dateTimePicker.getModelObject() == null) return;
            zonedDateTimeModel.setObject(ZonedDateTime.of(LocalDateTime.ofInstant(dateTimePicker.getModelObject().toInstant(), ZoneId.systemDefault()), selectedZone));
            logger.info("Initializing datetime with selected timezone: {}", zonedDateTimeModel.getObject());
        }
        ZonedDateTime currentZonedDateTime = zonedDateTimeModel.getObject().withZoneSameLocal(selectedZone);
        Date newDate;
        try {
            newDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(currentZonedDateTime.toLocalDateTime().toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        dateTimePicker.setModelObject(newDate).modelChanged();
        zonedDateTimeModel.setObject(currentZonedDateTime);
        logger.info("Updating existing datetime with selected timezone: {}", currentZonedDateTime);
    }

    public DropDownChoice<ZoneId> getZoneDropDown() {
        return zoneDropDown;
    }

    public DateTimePicker getDateTimePicker() {
        return dateTimePicker;
    }

    @Override
    public String getTextFormat() {
        logger.info("Getting text format.");
        return String.format("%s %s", this.datePattern, this.timePattern);
    }

    @Override
    public IModel<ZonedDateTime> getModel() {
        return this.zonedDateTimeModel;
    }

    @Override
    public String getInput() {
        logger.info("Getting input as string.");
        if (zonedDateTimeModel.getObject() == null) {
            return "";
        } else {
            return zonedDateTimeModel.getObject().toString();
        }
    }

    @Override
    public <C> IConverter<C> getConverter(Class<C> type) {
        return (IConverter<C>) this.newConverter();
    }

    private static IConverter<ZonedDateTime> newConverter() {
        return new ZonedDateTimeConverter() {
            @Override
            public ZonedDateTime convertToObject(String value, Locale locale) {
                if (value == null || value.trim().isEmpty()) {
                    return null;
                }
                return ZonedDateTime.parse(value);
            }
        };
    }

    @Override
    public FormComponent<ZonedDateTime> setModelObject(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            dateTimePicker.setModelObject(Date.from(zonedDateTime.toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant()));
            zoneDropDown.setModelObject(zonedDateTime.getZone());
            zoneIsDefault = false;
        }
        return super.setModelObject(zonedDateTime);
    }

}
