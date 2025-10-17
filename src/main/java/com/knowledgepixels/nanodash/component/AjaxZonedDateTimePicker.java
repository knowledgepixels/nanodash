package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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
    private String datePattern, timePattern;

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
        List<ZoneId> zones = new ArrayList<>(timezoneGroups.keySet());
        zones.sort(Comparator.comparing(zoneId -> zoneId.getRules().getStandardOffset(Instant.now()).getTotalSeconds()));

        this.zoneDropDown = new DropDownChoice<>("timezone-dropdown", zoneIdModel, zones, (IChoiceRenderer<ZoneId>) zoneId -> String.format("%s : %s", zoneId, timezoneGroups.get(zoneId).stream().map(ZoneId::getId).collect(Collectors.joining(", "))));

        this.zoneDropDown.setOutputMarkupId(true);
        this.zoneDropDown.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                updateZoneSelection();
                ajaxRequestTarget.add(AjaxZonedDateTimePicker.this);
            }
        });
        add(zoneDropDown);
        add(dateTimePicker);
    }

    private void updateZoneSelection() {
        ZoneId selectedZone = zoneIdModel.getObject();
        logger.info("Selected time zone: {}", selectedZone);
        if (zonedDateTimeModel.getObject() == null) {
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
        dateTimePicker.setModelObject(Date.from(zonedDateTime.toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant()));
        zoneDropDown.setModelObject(zonedDateTime.getZone());
        return super.setModelObject(zonedDateTime);
    }

}
