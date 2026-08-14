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
import java.util.stream.Collectors;

public class AjaxZonedDateTimePicker extends FormComponentPanel<ZonedDateTime> implements AbstractTextComponent.ITextFormatProvider {

    private static final int MAX_LABEL_ZONES = 3;

    // The prefixes of the regular zone ids; all others are legacy aliases (US/Eastern), single-country
    // names (Japan), or artificial zones (Etc/GMT+3, CET), which we don't want to name in the labels.
    private static final List<String> REGION_PREFIXES = List.of("Africa/", "America/", "Antarctica/",
            "Arctic/", "Asia/", "Atlantic/", "Australia/", "Europe/", "Indian/", "Pacific/");

    // The only places we name in the labels, most widely known first. Naming just these keeps the
    // labels free of the duplicate (Asia/Calcutta), obsolete (Australia/North) and remote
    // (Antarctica/DumontDUrville) zone ids that a zone list is otherwise full of. The tail of the
    // list is what gives the more unusual offsets a place name at all.
    private static final List<String> WELL_KNOWN_ZONES = List.of(
            "Europe/London", "America/New_York", "Asia/Tokyo", "Europe/Paris", "Asia/Shanghai",
            "America/Los_Angeles", "America/Chicago", "Europe/Berlin", "Europe/Madrid", "Europe/Rome",
            "Europe/Amsterdam", "Europe/Lisbon", "Europe/Athens", "Europe/Istanbul", "Europe/Kyiv",
            "Europe/Moscow", "Africa/Lagos", "Africa/Cairo", "Africa/Nairobi", "Africa/Johannesburg",
            "Africa/Casablanca", "Africa/Accra", "America/Toronto", "America/Vancouver",
            "America/Mexico_City", "America/Denver", "America/Phoenix", "America/Halifax",
            "America/Bogota", "America/Lima", "America/Santiago", "America/Sao_Paulo",
            "America/Argentina/Buenos_Aires", "America/Anchorage", "Asia/Jerusalem", "Asia/Dubai",
            "Asia/Baghdad", "Asia/Tehran", "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka",
            "Asia/Bangkok", "Asia/Jakarta", "Asia/Ho_Chi_Minh", "Asia/Hong_Kong", "Asia/Singapore",
            "Asia/Manila", "Asia/Seoul", "Australia/Perth", "Australia/Adelaide", "Australia/Brisbane",
            "Australia/Sydney", "Pacific/Auckland", "Pacific/Honolulu", "Atlantic/Reykjavik",
            // Below: places that are the only ones to give their offset a name.
            "America/St_Johns", "America/Puerto_Rico", "America/Caracas", "America/Noronha",
            "America/Nuuk", "America/Adak", "Atlantic/Azores", "Atlantic/Cape_Verde",
            "Atlantic/South_Georgia", "Asia/Baku", "Europe/Samara", "Asia/Kabul", "Asia/Tashkent",
            "Asia/Yekaterinburg", "Asia/Colombo", "Asia/Kathmandu", "Asia/Almaty", "Asia/Yangon",
            "Asia/Yakutsk", "Asia/Vladivostok", "Asia/Magadan", "Asia/Kamchatka", "Indian/Maldives",
            "Australia/Eucla", "Australia/Darwin", "Australia/Lord_Howe", "Pacific/Guam",
            "Pacific/Noumea", "Pacific/Fiji", "Pacific/Chatham", "Pacific/Apia", "Pacific/Tongatapu",
            "Pacific/Kiritimati", "Pacific/Pago_Pago", "Pacific/Niue", "Pacific/Tahiti",
            "Pacific/Marquesas", "Pacific/Gambier", "Pacific/Pitcairn");

    private final Logger logger = LoggerFactory.getLogger(AjaxZonedDateTimePicker.class);
    private IModel<ZonedDateTime> zonedDateTimeModel = Model.of((ZonedDateTime) null);
    private IModel<ZoneId> zoneIdModel = Model.of((ZoneId) null);
    private IModel<Date> dateModel = Model.of((Date) null);
    private final DropDownChoice<ZoneId> zoneDropDown;
    private DateTimePicker dateTimePicker;
    private final List<ZoneId> zones;
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

        Instant now = Instant.now();
        Map<ZoneOffset, List<ZoneId>> timezoneGroups = getZonesByCurrentOffset(now);
        this.zones = new ArrayList<>(timezoneGroups.keySet());
        sortByOffset(zones, now);

        if (this.zonedDateTimeModel.getObject() != null) {
            this.zoneIdModel = Model.of(model.getObject().getZone());
            // An existing value can carry an offset that no zone is currently at, and the dropdown
            // has to show it nonetheless:
            ensureZoneChoice(zoneIdModel.getObject(), now);
        } else {
            // No zone given by an existing value, so we preselect the one the user is most likely in:
            zoneIdModel.setObject(selectZoneChoice(getUserZoneId(), now));
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
                (IChoiceRenderer<ZoneId>) zoneId -> getZoneChoiceLabel(zoneId, timezoneGroups.getOrDefault(zoneId, List.of()), getUserZoneId())
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
     * Groups all time zones by the offset they are currently at, so that a zone is found under the
     * offset it is really at right now, including daylight saving time. Legacy and artificial zone
     * ids (like {@code US/Eastern} or {@code Etc/GMT+3}) are left out: they duplicate the regular
     * zones, and would offer offsets that no place is at.
     *
     * @param instant the instant to determine the zones' offsets at
     * @return the zones of each currently used offset
     */
    public static Map<ZoneOffset, List<ZoneId>> getZonesByCurrentOffset(Instant instant) {
        return ZoneId.getAvailableZoneIds().stream()
                .filter(id -> REGION_PREFIXES.stream().anyMatch(id::startsWith))
                .map(ZoneId::of)
                .collect(Collectors.groupingBy(zoneId -> zoneId.getRules().getOffset(instant)));
    }

    /**
     * Returns the label of the given zone choice, for example {@code "UTC+02:00 — Rome, Paris,
     * Berlin"}: the offset, followed by the places that are currently at it. The user's own place is
     * named first, so that they recognize the zone that is preselected for them.
     *
     * @param choice   the zone choice to label
     * @param atOffset the zones that are currently at the choice's offset
     * @param userZone the user's own time zone, or null if unknown
     * @return the label of the given zone choice
     */
    public static String getZoneChoiceLabel(ZoneId choice, List<ZoneId> atOffset, ZoneId userZone) {
        String examples = atOffset.stream()
                .filter(zoneId -> getExampleRank(zoneId, userZone) < Integer.MAX_VALUE)
                .sorted(Comparator.comparingInt(zoneId -> getExampleRank(zoneId, userZone)))
                .limit(MAX_LABEL_ZONES)
                .map(AjaxZonedDateTimePicker::getPlaceName)
                .collect(Collectors.joining(", "));
        String offsetLabel = getOffsetLabel(choice);
        return examples.isEmpty() ? offsetLabel : offsetLabel + " — " + examples;
    }

    /**
     * Returns the offset as shown in the dropdown, for example {@code "UTC-05:30"} or, for the zero
     * offset, just {@code "UTC"}.
     *
     * @param zone the zone to label
     * @return the label of the given zone's offset
     */
    public static String getOffsetLabel(ZoneId zone) {
        int seconds = (zone instanceof ZoneOffset offset ? offset : zone.getRules().getOffset(Instant.now())).getTotalSeconds();
        if (seconds == 0) return "UTC";
        return String.format("UTC%s%02d:%02d", seconds < 0 ? "-" : "+", Math.abs(seconds) / 3600, Math.abs(seconds) % 3600 / 60);
    }

    /**
     * Returns the place of the given zone as shown in the labels, for example {@code "New York"} for
     * {@code America/New_York}.
     */
    private static String getPlaceName(ZoneId zoneId) {
        String id = zoneId.getId();
        return id.substring(id.lastIndexOf('/') + 1).replace('_', ' ');
    }

    /**
     * Ranks the given zone as an example in a label: the user's own zone comes first, followed by
     * the well-known ones in their given order, followed by all others.
     */
    private static int getExampleRank(ZoneId zoneId, ZoneId userZone) {
        if (zoneId.equals(userZone)) return -1;
        int index = WELL_KNOWN_ZONES.indexOf(zoneId.getId());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    /**
     * Adds the given zone to the choices if it isn't among them already, keeping them ordered by
     * offset.
     */
    private void ensureZoneChoice(ZoneId zone, Instant instant) {
        if (zone == null || zones.contains(zone)) return;
        zones.add(zone);
        sortByOffset(zones, instant);
    }

    private static void sortByOffset(List<ZoneId> zones, Instant instant) {
        zones.sort(Comparator.comparingInt(zoneId -> zoneId.getRules().getOffset(instant).getTotalSeconds()));
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
     * Returns the choice for the given zone, which is the offset that zone is at at the given
     * instant, daylight saving time included, and makes sure it is among the choices.
     */
    private ZoneOffset selectZoneChoice(ZoneId zone, Instant instant) {
        ZoneOffset offset = zone.getRules().getOffset(instant);
        ensureZoneChoice(offset, instant);
        return offset;
    }

    /**
     * Applies the time zone the browser just reported, unless the zone was determined by the user or
     * by an existing value in the meantime.
     */
    private void applyClientZone(WebClientInfo clientInfo, AjaxRequestTarget target) {
        if (!zoneIsDefault) return;
        TimeZone timeZone = clientInfo.getProperties().getTimeZone();
        if (timeZone == null) return;
        ZoneId choice = selectZoneChoice(timeZone.toZoneId(), Instant.now());
        if (choice.equals(zoneIdModel.getObject())) return;
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
