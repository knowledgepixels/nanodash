package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxClientInfoBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The timezone of a date and time, picked from the offsets places are at, each labelled with a
 * few of those places, e.g. "UTC+01:00 — Zurich, Paris, Berlin". The timezone the user is most
 * likely in is preselected: the one their browser reports, which is asked for once per session.
 * <p>
 * The places are those at each offset at the date and time being entered, so that a date in
 * winter is offered winter time under the user's own place.
 * <p>
 * The choices are listed by offset in a dropdown that can be searched (issue #266): by any place
 * at an offset, not only the few its label names, and by the offset itself, written as
 * "+1", "UTC+1", "GMT-05:00" or the like.
 * <p>
 * Shared by every field that takes a date and time, so that they offer the same choices and
 * name them the same way.
 */
public class ZoneChoice extends Panel {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(ZoneChoice.class);

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

    private static final String WIDTH = "22em";

    /**
     * A search term that is an offset, optionally after "utc" or "gmt", as normalized.
     */
    private static final Pattern OFFSET_TERM = Pattern.compile("^(?:utc|gmt)?([+-][0-9:]*)$");

    private final IModel<ZoneId> model;
    private transient Map<ZoneOffset, List<ZoneId>> zonesByOffset;
    private transient Instant zonesGroupedAt;
    private final Select2Choice<ZoneId> dropDown;

    /**
     * Constructs a ZoneChoice editing the given model, which holds the timezone picked.
     *
     * @param id    the component ID
     * @param model the model of the timezone; its value is offered even if no place is at it today
     */
    public ZoneChoice(String id, IModel<ZoneId> model) {
        super(id);
        this.model = model;
        dropDown = new Select2Choice<>("dropdown", model, new ZoneChoiceProvider()) {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                getSettings().setAllowClear(isNoZoneAllowed());
                getSettings().setPlaceholder(isNoZoneAllowed() ? "no timezone" : "choose timezone");
            }

        };
        dropDown.getSettings().setCloseOnSelect(true);
        dropDown.getSettings().setWidth(WIDTH);
        dropDown.getSettings().setDropdownCssClass("timezone-dropdown");
        Utils.setSelect2SearchAsYouType(dropDown);
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
    public Select2Choice<ZoneId> getDropDown() {
        return dropDown;
    }

    /**
     * Returns the date and time the timezone is for, so that the places are found under the
     * offset they are at then: in January, Zurich is at UTC+01:00, whatever the date today.
     *
     * @return the date and time entered, or null while there is none, for the current one
     */
    protected LocalDateTime getDateTime() {
        return null;
    }

    private Instant getReferenceInstant() {
        LocalDateTime dateTime = getDateTime();
        return dateTime == null ? Instant.now() : dateTime.atZone(getUserZoneId()).toInstant();
    }

    /**
     * Returns the places grouped by the offset they are at at the date and time the timezone is
     * for, regrouping them only when that moves by an hour or more.
     */
    private Map<ZoneOffset, List<ZoneId>> getZonesByOffset() {
        Instant reference = getReferenceInstant();
        if (zonesByOffset == null || Math.abs(reference.getEpochSecond() - zonesGroupedAt.getEpochSecond()) >= 3600) {
            zonesByOffset = getZonesByCurrentOffset(reference);
            zonesGroupedAt = reference;
        }
        return zonesByOffset;
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
     *
     * @return the timezones, west to east
     */
    List<ZoneId> getChoices() {
        Instant reference = getReferenceInstant();
        List<ZoneId> choices = new ArrayList<>(getZonesByOffset().keySet());
        ZoneId current = model.getObject();
        if (current != null && !choices.contains(current)) choices.add(current);
        choices.sort(Comparator.comparingInt(zone -> zone.getRules().getOffset(reference).getTotalSeconds()));
        return choices;
    }

    /**
     * Returns the timezones on offer that match what was typed into the search box.
     *
     * @param term what was typed, which may be empty or null for all of them
     * @return the matching timezones, west to east
     */
    List<ZoneId> search(String term) {
        String query = normalize(term);
        if (query.isEmpty()) return getChoices();
        return getChoices().stream().filter(zone -> matches(zone, query)).toList();
    }

    /**
     * Whether a timezone matches a search: an offset typed, such as "+1", "UTC+5:30" or
     * "GMT-05", has to be the timezone's offset, or the beginning of it while it is still being
     * typed with its minutes; anything else is looked for in the places at the offset, all of
     * them rather than only the few the label names. "UTC", "GMT" and "Z" are the places of the
     * zero offset.
     */
    private boolean matches(ZoneId zone, String query) {
        Matcher offsetTyped = OFFSET_TERM.matcher(query);
        if (offsetTyped.matches()) {
            String offset = offsetTyped.group(1);
            List<String> spellings = getOffsetSpellings(zone);
            return spellings.contains(offset) || (offset.contains(":") && spellings.get(0).startsWith(offset));
        }
        return getPlaceText(zone).contains(query);
    }

    private String getPlaceText(ZoneId zone) {
        List<String> places = new ArrayList<>();
        getZonesByOffset().getOrDefault(zone, List.of()).forEach(place -> places.add(getPlaceName(place)));
        if (getOffsetSpellings(zone).contains("+00:00")) places.addAll(List.of("UTC", "GMT", "Z"));
        return places.stream().map(ZoneChoice::normalize).collect(Collectors.joining("|"));
    }

    /**
     * Returns the ways an offset is written, the full form first: "+01:00", "+0100", "+01" and
     * "+1" for one hour east of UTC, or "+05:30", "+0530" and "+5:30" for five and a half;
     * and "-00:00", "-0" and the like for UTC too.
     */
    private static List<String> getOffsetSpellings(ZoneId zone) {
        int seconds = (zone instanceof ZoneOffset offset ? offset : zone.getRules().getOffset(Instant.now())).getTotalSeconds();
        List<String> signs = seconds == 0 ? List.of("+", "-") : List.of(seconds < 0 ? "-" : "+");
        int hours = Math.abs(seconds) / 3600, minutes = Math.abs(seconds) % 3600 / 60;
        List<String> spellings = new ArrayList<>();
        for (String sign : signs) {
            spellings.add(String.format("%s%02d:%02d", sign, hours, minutes));
            spellings.add(String.format("%s%02d%02d", sign, hours, minutes));
            if (minutes == 0) {
                spellings.add(String.format("%s%02d", sign, hours));
                spellings.add(sign + hours);
            } else {
                spellings.add(String.format("%s%d:%02d", sign, hours, minutes));
            }
        }
        return spellings;
    }

    /**
     * Makes a search term and the text it is looked for in comparable: lower case, without
     * spaces, with "−" read as "-" and with letters stripped of their accents, so that
     * "sao paulo" finds "São Paulo".
     */
    private static String normalize(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replace('\u2212', '-').replaceAll("[\\s_]", "");
    }

    /**
     * Offers the timezones that match the search, labels each one by its offset and the places
     * at it, and submits its id.
     */
    private class ZoneChoiceProvider extends ChoiceProvider<ZoneId> {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayValue(ZoneId zone) {
            return getZoneChoiceLabel(zone, getZonesByOffset().getOrDefault(zone, List.of()), getUserZoneId());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getIdValue(ZoneId zone) {
            return zone.getId();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void query(String term, int page, Response<ZoneId> response) {
            response.addAll(search(term));
            response.setHasMore(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<ZoneId> toChoices(Collection<String> ids) {
            List<ZoneId> zones = new ArrayList<>();
            for (String id : ids) {
                try {
                    zones.add(ZoneId.of(id));
                } catch (DateTimeException ex) {
                    logger.warn("Ignoring unknown timezone {}", id);
                }
            }
            return zones;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Holds nothing to detach; overridden as the other providers are, for Tomcat's sake.
         */
        @Override
        public void detach() {
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
