package com.knowledgepixels.nanodash.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DateConverter;
import org.apache.wicket.util.convert.converter.ZonedDateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.form.datetime.AjaxDateTimePicker;
import org.wicketstuff.kendo.ui.form.datetime.DateTimePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

public class AjaxZonedDateTimePicker extends FormComponentPanel<ZonedDateTime> implements AbstractTextComponent.ITextFormatProvider {

    private final Logger logger = LoggerFactory.getLogger(AjaxZonedDateTimePicker.class);
    private IModel<ZonedDateTime> zonedDateTimeModel = Model.of((ZonedDateTime) null);
    private IModel<ZoneId> zoneIdModel = Model.of((ZoneId) null);
    private IModel<Date> dateModel = Model.of((Date) null);
    private final ZoneChoice zoneChoice;
    private final DropDownChoice<ZoneId> zoneDropDown;
    private DateTimePicker dateTimePicker;
    private String datePattern, timePattern;

    // True as long as the selected zone is just our default guess, i.e. neither the user nor an
    // existing value has determined it. Only then may the detected client zone still overrule it.
    private boolean zoneIsDefault;

    // Whether the time field asks for seconds, which it only does for a value that has them.
    private boolean secondsShown;

    public AjaxZonedDateTimePicker(String id, IModel<ZonedDateTime> model, String datePattern, String timePattern) {
        super(id);
        this.setType(ZonedDateTime.class);
        this.setModel(model);
        if (model.getObject() != null) {
            dateModel.setObject(Date.from(model.getObject().toInstant()));
        }
        this.datePattern = datePattern;
        this.timePattern = timePattern;
        this.secondsShown = hasSeconds(timePattern) || hasSeconds(model.getObject());
        this.dateTimePicker = newDateTimePicker();

        this.zonedDateTimeModel = model;

        if (this.zonedDateTimeModel.getObject() != null) {
            this.zoneIdModel = Model.of(model.getObject().getZone());
        } else {
            zoneIdModel.setObject(ZoneChoice.getUserOffsetAt(null));
            zoneIsDefault = true;
        }

        this.zoneChoice = new ZoneChoice("timezone", zoneIdModel) {
            @Override
            protected void onClientZone(AjaxRequestTarget target, ZoneId clientZone) {
                applyClientZone(target);
            }
        };
        this.zoneDropDown = zoneChoice.getDropDown();
        this.zoneDropDown.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                zoneIsDefault = false;
                updateZoneSelection();
                ajaxRequestTarget.add(AjaxZonedDateTimePicker.this);
            }
        });
        add(zoneChoice);
        add(dateTimePicker);
    }

    /**
     * Creates the date and time fields, asking for seconds or not, as {@link #secondsShown} has it.
     */
    private DateTimePicker newDateTimePicker() {
        return new AjaxDateTimePicker("datetime", dateModel, datePattern, getTimePattern()) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                // Prevent Edge's autofill from overwriting the Kendo-formatted date/time values with a mismatched format:
                datePicker.add(new org.apache.wicket.AttributeModifier("autocomplete", "off"));
                datePicker.add(new org.apache.wicket.AttributeModifier("data-form-type", "other"));
                timePicker.add(new org.apache.wicket.AttributeModifier("autocomplete", "off"));
                timePicker.add(new org.apache.wicket.AttributeModifier("data-form-type", "other"));
                if (secondsShown) {
                    // Lets the stylesheet give the field the width of a time with seconds:
                    timePicker.add(org.apache.wicket.AttributeModifier.append("class", "with-seconds"));
                }
            }
            @Override
            @SuppressWarnings("unchecked")
            public <C> IConverter<C> getConverter(Class<C> type) {
                if (Date.class.isAssignableFrom(type)) {
                    return (IConverter<C>) newDateTimeConverter();
                }
                return super.getConverter(type);
            }
            @Override
            public void onValueChanged(IPartialPageRequestHandler handler) {
                Date selectedDate = this.getModelObject();
                followUserZoneAt(selectedDate, handler);
                if (zoneIdModel.getObject() == null) {
                    this.setModelObject(selectedDate);
                    logger.info("Date selected without timezone: {}", dateModel.getObject());
                } else {
                    ZonedDateTime currentZonedDateTime = LocalDateTime.ofInstant(selectedDate.toInstant(), ZoneId.systemDefault()).atZone(zoneIdModel.getObject());
                    zonedDateTimeModel.setObject(currentZonedDateTime);
                    logger.info("Date selected: {}", dateModel.getObject());
                    logger.info("Selected datetime with current timezone: {}", zonedDateTimeModel.getObject());
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Starts asking for seconds as soon as the value at hand has them, so that a value which does
     * carry seconds is shown in full and can be edited, rather than appearing rounded to the minute.
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        if (!secondsShown && hasSeconds(getModelObject())) {
            secondsShown = true;
            dateTimePicker = newDateTimePicker();
            addOrReplace(dateTimePicker);
        }
    }

    /**
     * Returns a converter for the date and time fields that accepts a time with seconds as well, so
     * that a user who types them is taken at their word by a field that only asks for minutes.
     */
    private IConverter<Date> newDateTimeConverter() {
        List<String> patterns = new ArrayList<>();
        patterns.add(datePattern + " " + getTimePattern());
        if (!secondsShown) patterns.add(datePattern + " " + withSeconds(timePattern));
        return new DateConverter() {
            @Override
            public DateFormat getDateFormat(Locale locale) {
                return new SimpleDateFormat(patterns.get(0), locale != null ? locale : Locale.getDefault());
            }

            @Override
            public Date convertToObject(String value, Locale locale) {
                if (value == null || value.trim().isEmpty()) return null;
                for (String pattern : patterns) {
                    ParsePosition position = new ParsePosition(0);
                    Date date = new SimpleDateFormat(pattern, locale != null ? locale : Locale.getDefault()).parse(value, position);
                    if (date != null && position.getIndex() == value.length()) return date;
                }
                throw newConversionException("Cannot parse '" + value + "' as a date and time", value, locale);
            }
        };
    }

    /**
     * Returns the pattern the time field is asking for, which is the given one plus seconds where
     * the value has them.
     */
    private String getTimePattern() {
        return secondsShown ? withSeconds(timePattern) : timePattern;
    }

    private static String withSeconds(String timePattern) {
        return hasSeconds(timePattern) ? timePattern : timePattern + ":ss";
    }

    private static boolean hasSeconds(String timePattern) {
        return timePattern.contains("s");
    }

    private static boolean hasSeconds(ZonedDateTime value) {
        return value != null && (value.getSecond() != 0 || value.getNano() != 0);
    }

    /**
     * Moves a preselected zone to the offset the user's time zone has at the date and time just
     * picked, so that a date in winter gets winter time even when it is entered in summer. A zone
     * picked by the user or given by an existing value is left alone.
     *
     * @param selected the date and time picked, read in the server's time zone
     * @param handler  the handler to repaint the dropdown with, or null
     */
    private void followUserZoneAt(Date selected, IPartialPageRequestHandler handler) {
        if (!zoneIsDefault || selected == null) return;
        ZoneOffset offset = ZoneChoice.getUserOffsetAt(LocalDateTime.ofInstant(selected.toInstant(), ZoneId.systemDefault()));
        if (offset.equals(zoneIdModel.getObject())) return;
        zoneIdModel.setObject(offset);
        if (handler != null) handler.add(zoneDropDown);
    }

    /**
     * Returns the date and time currently picked, without a zone, or null if none is.
     */
    private LocalDateTime getPickedDateTime() {
        Date picked = dateTimePicker.getModelObject();
        return picked == null ? null : LocalDateTime.ofInstant(picked.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Applies the time zone the browser just reported, unless the zone was determined by the user or
     * by an existing value in the meantime. The date and time fields keep the local time the user
     * sees, so only the zone needs repainting.
     */
    private void applyClientZone(AjaxRequestTarget target) {
        if (!zoneIsDefault) return;
        ZoneId choice = ZoneChoice.getUserOffsetAt(getPickedDateTime());
        if (choice.equals(zoneIdModel.getObject())) return;
        logger.info("Setting time zone to the one reported by the browser: {}", choice);
        zoneIdModel.setObject(choice);
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
        return String.format("%s %s", this.datePattern, this.getTimePattern());
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

    /**
     * {@inheritDoc}
     * <p>
     * Builds the value from what was actually submitted in the date, time and zone fields, as a
     * {@link FormComponentPanel} is meant to. Reading the model instead would make the value depend
     * on an ajax call having stored it there first, which loses whatever the user typed straight
     * into the fields.
     */
    @Override
    public void convertInput() {
        if (!isDateTimeSubmitted()) {
            // The date and time fields took no part in this request (an ajax update of some other
            // component, say), so there is no input to convert: keep the value we have.
            setConvertedInput(getModelObject());
            return;
        }
        Date date = dateTimePicker.getConvertedInput();
        if (date == null) {
            setConvertedInput(null);
            return;
        }
        LocalDateTime local = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        setConvertedInput(local.atZone(getSubmittedZone(local)));
    }

    /**
     * Returns whether the date and time fields were submitted in this request, which they are on a
     * form submit even when the user left them empty.
     */
    private boolean isDateTimeSubmitted() {
        for (String fieldId : new String[]{"datepicker", "timepicker"}) {
            if (dateTimePicker.get(fieldId) instanceof FormComponent<?> field
                    && !getRequest().getRequestParameters().getParameterValue(field.getInputName()).isNull()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the zone to read the entered date and time in: the one just submitted, or the one
     * currently selected if the dropdown was not part of this request. A zone that is still only
     * preselected is the offset the user's time zone has at that date and time: the dropdown may
     * still show the offset for a date picked before, when the change to the date arrives together
     * with the submit, and only picking a zone in the dropdown, which its own Ajax update reports
     * as soon as it is picked, decides it.
     *
     * @param local the date and time entered
     * @return the zone
     */
    private ZoneId getSubmittedZone(LocalDateTime local) {
        if (zoneIsDefault) return ZoneChoice.getUserOffsetAt(local);
        ZoneId submitted = zoneDropDown.getConvertedInput();
        if (submitted != null) return submitted;
        return zoneIdModel.getObject() != null ? zoneIdModel.getObject() : ZoneChoice.getUserZoneId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Leaves the model alone while the date or time field holds something that could not be
     * converted, such as a date without a time. Wicket skips {@link #convertInput()} for a panel
     * whose children are in error, so updating the model here would store a null over a value the
     * user can still see in the fields.
     */
    @Override
    public void updateModel() {
        if (!isValid()) return;
        super.updateModel();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Keeps what the user typed in the date and time fields while they are in error. A half-entered
     * value has no model to fall back on, so clearing the fields' input -- which
     * {@link FormComponentPanel#clearInput()} does for the whole panel -- is what empties them when
     * the page comes back with an error, unlike every other field on the form.
     */
    @Override
    public void clearInput() {
        if (!isValid()) return;
        super.clearInput();
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

    /**
     * {@inheritDoc}
     * <p>
     * Keeps the date and time fields on the value, in both directions: a value that is cleared
     * empties them too, rather than leaving them showing a date the field no longer holds.
     */
    @Override
    public FormComponent<ZonedDateTime> setModelObject(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            dateTimePicker.setModelObject(Date.from(zonedDateTime.toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant()));
            zoneDropDown.setModelObject(zonedDateTime.getZone());
            zoneIsDefault = false;
        } else {
            dateTimePicker.setModelObject(null);
        }
        return super.setModelObject(zonedDateTime);
    }

}
