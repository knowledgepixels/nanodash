package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDateTimePicker;
import org.wicketstuff.kendo.ui.form.datetime.DateTimePicker;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AjaxZonedDateTimePicker extends DateTimePicker {

    private final Logger logger = LoggerFactory.getLogger(AjaxZonedDateTimePicker.class);
    private final IModel<ZonedDateTime> zonedDateTimeModel;
    private IModel<ZoneId> zoneIdModel = Model.of(ZoneId.systemDefault());
    private final DropDownChoice<ZoneId> zoneDropDown;
    private DateTimePicker dateTimePicker;

    public AjaxZonedDateTimePicker(String id, IModel<ZonedDateTime> model, String datePattern, String timePattern) {
        super(id);
        dateTimePicker = new AjaxDateTimePicker("datetime", model.getObject() == null ? Model.of((Date) null) : Model.of(Date.from(model.getObject().toInstant())), datePattern, timePattern) {
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                Date selectedDate = this.getModelObject();
                ZonedDateTime currentZonedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault()).atZone(zoneIdModel.getObject());
                zonedDateTimeModel.setObject(currentZonedDateTime);
                logger.info("Selected datetime with current timezone: {}", zonedDateTimeModel.getObject());
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
                    dateTimePicker.setModelObject(Date.from(currentZonedDateTime.toLocalDateTime().atZone(zoneIdModel.getObject()).toInstant()));
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

}
