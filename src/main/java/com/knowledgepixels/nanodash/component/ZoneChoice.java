package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * The timezone of a date and time, picked from the offsets places are at, each labelled with a
 * few of those places, e.g. "UTC+01:00 — Zurich, Paris, Berlin". The timezone the user is most
 * likely in is offered first: the one their browser reports, which is asked for once per session.
 * <p>
 * Shared by every field that takes a date and time, so that they offer the same choices and
 * name them the same way.
 */
public class ZoneChoice extends Panel {

    private static final long serialVersionUID = 1L;

    private static final int MAX_LABEL_ZONES = 3;

    /**
     * The prefixes of the regular zone ids. All others are legacy aliases (US/Eastern),
     * single-country names (Japan) or artificial zones (Etc/GMT+3, CET), which are not named in
     * the labels.
     */
    private static final List<String> REGION_PREFIXES = List.of("Africa/", "America/", "Antarctica/",
            "Arctic/", "Asia/", "Atlantic/", "Australia/", "Europe/", "Indian/", "Pacific/");

    /**
     * The only places named in the labels, most widely known first. Naming just these keeps the
     * labels free of the duplicate (Asia/Calcutta), obsolete (Australia/North) and remote
     * (Antarctica/DumontDUrville) zone ids a zone list is otherwise full of. The tail of the list
     * is what gives the more unusual offsets a place name at all.
     */
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
            "America/St_Johns", "America/Puerto_Rico", "America/Caracas", "America/Noronha",
            "America/Nuuk", "America/Adak", "Atlantic/Azores", "Atlantic/Cape_Verde",
            "Atlantic/South_Georgia", "Asia/Baku", "Europe/Samara", "Asia/Kabul", "Asia/Tashkent",
            "Asia/Yekaterinburg", "Asia/Colombo", "Asia/Kathmandu", "Asia/Almaty", "Asia/Yangon",
            "Asia/Yakutsk", "Asia/Vladivostok", "Asia/Magadan", "Asia/Kamchatka", "Indian/Maldives",
            "Australia/Eucla", "Australia/Darwin", "Australia/Lord_Howe", "Pacific/Guam",
            "Pacific/Noumea", "Pacific/Fiji", "Pacific/Chatham", "Pacific/Apia", "Pacific/Tongatapu",
            "Pacific/Kiritimati", "Pacific/Pago_Pago", "Pacific/Niue", "Pacific/Tahiti",
            "Pacific/Marquesas", "Pacific/Gambier", "Pacific/Pitcairn");

    private final IModel<ZoneId> model;
    private final Map<ZoneOffset, List<ZoneId>> zonesByOffset = getZonesByCurrentOffset(Instant.now());
    private final DropDownChoice<ZoneId> dropDown;

    /**
     * Constructs a ZoneChoice editing the given model, which holds the timezone picked.
     *
     * @param id    the component ID
     * @param model the model of the timezone; its value is offered even if no place is at it today
     */
    public ZoneChoice(String id, IModel<ZoneId> model) {
        super(id);
        this.model = model;
        dropDown = new DropDownChoice<>("dropdown", model, this::getChoices, new ZoneRenderer()) {

            @Override
            public boolean isNullValid() {
                return isNoZoneAllowed();
            }

            @Override
            protected String getNullValidDisplayValue() {
                return "no timezone";
            }

            @Override
            protected String getNullKeyDisplayValue() {
                return "choose timezone";
            }

        };
        dropDown.setOutputMarkupId(true);
        add(dropDown);
        if (getClientZoneId() == null) add(new ClientZoneBehavior());
    }

    /**
     * Returns the dropdown the timezone is picked with, which is the form component of this
     * choice.
     *
     * @return the dropdown
     */
    public DropDownChoice<ZoneId> getDropDown() {
        return dropDown;
    }

    /**
     * Whether "no timezone" may be picked, which a field offers only to keep a date and time
     * filled in from an existing value that has none.
     *
     * @return false, unless a subclass says otherwise
     */
    protected boolean isNoZoneAllowed() {
        return false;
    }

    /**
     * Called once the browser has reported its timezone, so that a timezone preselected from
     * the server's own can be corrected. Repaints the dropdown, whose labels name the user's own
     * place first.
     *
     * @param target     the Ajax request target
     * @param clientZone the timezone the browser reported
     */
    protected void onClientZone(AjaxRequestTarget target, ZoneId clientZone) {
        target.add(dropDown);
    }

    /**
     * Asks the browser for its timezone, which is then known for the rest of the session.
     */
    private class ClientZoneBehavior extends AjaxClientInfoBehavior {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onClientInfo(AjaxRequestTarget target, WebClientInfo clientInfo) {
            TimeZone timeZone = clientInfo.getProperties().getTimeZone();
            if (timeZone != null) onClientZone(target, timeZone.toZoneId());
        }

    }

    /**
     * Returns the timezones on offer: every offset some place is at right now, plus the one the
     * model holds, which may be one no place is at today. They are ordered by offset.
     */
    private List<ZoneId> getChoices() {
        Instant now = Instant.now();
        List<ZoneId> choices = new ArrayList<>(zonesByOffset.keySet());
        ZoneId current = model.getObject();
        if (current != null && !choices.contains(current)) choices.add(current);
        choices.sort(Comparator.comparingInt(zone -> zone.getRules().getOffset(now).getTotalSeconds()));
        return choices;
    }

    /**
     * Labels each timezone by its offset and the places at it, and submits its id rather than its
     * position in the list.
     */
    private class ZoneRenderer implements IChoiceRenderer<ZoneId> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getDisplayValue(ZoneId zone) {
            return getZoneChoiceLabel(zone, zonesByOffset.getOrDefault(zone, List.of()), getUserZoneId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getIdValue(ZoneId zone, int index) {
            return zone.getId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ZoneId getObject(String id, IModel<? extends List<? extends ZoneId>> choices) {
            return choices.getObject().stream().filter(zone -> zone.getId().equals(id)).findFirst().orElse(null);
        }

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
                .map(ZoneChoice::getPlaceName)
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
     * Returns the offset the user's time zone has at the given local date and time, so that a
     * date in winter gets winter time even when it is entered in summer.
     *
     * @param localDateTime the date and time, or null for the current one
     * @return the user's offset at that date and time
     */
    public static ZoneOffset getUserOffsetAt(LocalDateTime localDateTime) {
        ZoneId userZone = getUserZoneId();
        if (localDateTime == null) return userZone.getRules().getOffset(Instant.now());
        return userZone.getRules().getOffset(localDateTime);
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

}
