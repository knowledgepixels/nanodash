package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AjaxZonedDateTimePicker extends FormComponentPanel<ZonedDateTime> implements AbstractTextComponent.ITextFormatProvider {

    private final Logger logger = LoggerFactory.getLogger(AjaxZonedDateTimePicker.class);
    private final IModel<ZonedDateTime> zonedDateTimeModel;
    private IModel<ZoneId> zoneIdModel = Model.of(ZoneId.systemDefault());
    private final DropDownChoice<ZoneId> zoneDropDown;
    private final DateTimePicker dateTimePicker;
    private IModel<Date> dateModel = Model.of((Date) null);
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
                ZonedDateTime currentZonedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault()).atZone(zoneIdModel.getObject());
                zonedDateTimeModel.setObject(currentZonedDateTime);
                logger.info("Date selected: {}", dateModel.getObject());
                logger.info("Selected datetime with current timezone: {}", zonedDateTimeModel.getObject());
                dateTimePicker.modelChanged();
                this.modelChanged();
            }
        };

        this.zonedDateTimeModel = model;
        if (this.zonedDateTimeModel.getObject() != null) {
            this.zoneIdModel = Model.of(model.getObject().getZone());
        }

        List<ZoneId> zones = ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .collect(Collectors.toList());

        this.zoneDropDown = new DropDownChoice<>("timezone-dropdown", zoneIdModel, zones);
        this.zoneDropDown.setOutputMarkupId(true);
        this.zoneDropDown.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                ZoneId selectedZone = zoneIdModel.getObject();
                logger.info("Selected time zone: {}", selectedZone);
                if (zonedDateTimeModel.getObject() != null) {
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
                ajaxRequestTarget.add(AjaxZonedDateTimePicker.this);
            }
        });
        add(zoneDropDown);
        add(dateTimePicker);
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

}
