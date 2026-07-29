package com.knowledgepixels.nanodash.calendar;

import com.knowledgepixels.nanodash.domain.Space;
import org.nanopub.SimpleTimestampPattern;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;

/**
 * A single calendar event, ready to be serialised as a {@code VEVENT} (see
 * {@link IcsWriter}) or turned into an external calendar URL (see {@link CalendarUrls}).
 *
 * <p>An event is either <em>timed</em> ({@link #getStart()}/{@link #getEnd()} are set)
 * or <em>all-day</em> ({@link #getStartDate()}/{@link #getEndDateExclusive()} are set),
 * depending on whether the source data carried a full {@code xsd:dateTime} or a bare
 * {@code xsd:date}. Emitting an all-day event as a timed one would place a multi-day
 * conference at midnight UTC, so the distinction is carried all the way through.</p>
 *
 * <p>The {@link #getUid() UID} is <em>derived from the event's IRI</em>, never random:
 * calendar clients recognise a re-fetched event as an update of the one they already
 * hold only when the UID is stable across fetches. Together with {@link #getLastModified()}
 * — which feeds the {@code SEQUENCE} — this is what lets a subscribed feed propagate a
 * changed date rather than growing a duplicate.</p>
 */
public final class CalendarEvent implements Serializable {

    private final String uid;
    private final String summary;
    private final String description;
    private final String location;
    private final String url;
    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final LocalDate startDate;
    private final LocalDate endDateExclusive;
    private final OffsetDateTime lastModified;

    private CalendarEvent(String uid, String summary, String description, String location, String url,
                          OffsetDateTime start, OffsetDateTime end,
                          LocalDate startDate, LocalDate endDateExclusive,
                          OffsetDateTime lastModified) {
        this.uid = uid;
        this.summary = summary;
        this.description = description;
        this.location = location;
        this.url = url;
        this.start = start;
        this.end = end;
        this.startDate = startDate;
        this.endDateExclusive = endDateExclusive;
        this.lastModified = lastModified;
    }

    /**
     * Builds the calendar event for an event space, from the {@code schema:startDate},
     * {@code schema:endDate} and {@code schema:location} of its root-definition assertion.
     *
     * <p>A missing or unusable end date is filled in: one hour after the start for a timed
     * event, one day for an all-day one. {@code schema:endDate} is treated as
     * <em>inclusive</em> (the last day of the event), as iCalendar's {@code DTEND} for
     * all-day events is exclusive.</p>
     *
     * @param space    the space to map; typically of type {@code Event}
     * @param eventUrl the Nanodash page for this space, used as the event's {@code URL}
     * @return the calendar event, or null if the space carries no usable start date
     */
    public static CalendarEvent fromSpace(Space space, String eventUrl) {
        Calendar startCal = space.getStartDate();
        if (startCal == null) return null;
        Calendar endCal = space.getEndDate();

        String label = space.getLabel();
        String summary = (label == null || label.isBlank()) ? space.getId() : label;
        OffsetDateTime lastModified = lastModifiedOf(space);

        if (space.isStartDateOnly()) {
            LocalDate from = toLocalDate(startCal);
            // schema:endDate names the last day of the event; DTEND is exclusive.
            LocalDate toInclusive = (endCal != null && space.isEndDateOnly()) ? toLocalDate(endCal) : from;
            if (toInclusive.isBefore(from)) toInclusive = from;
            return new CalendarEvent(space.getId(), summary, space.getDescription(), space.getLocation(), eventUrl,
                    null, null, from, toInclusive.plusDays(1), lastModified);
        }

        OffsetDateTime from = toOffsetDateTime(startCal);
        OffsetDateTime to = (endCal != null && !space.isEndDateOnly()) ? toOffsetDateTime(endCal) : null;
        if (to == null || !to.isAfter(from)) to = from.plusHours(1);
        return new CalendarEvent(space.getId(), summary, space.getDescription(), space.getLocation(), eventUrl,
                from, to, null, null, lastModified);
    }

    /**
     * The point in time this event was last changed, used for {@code LAST-MODIFIED} and
     * to derive the {@code SEQUENCE}. Taken from the creation timestamp of the space's
     * current root-definition nanopub: a changed date means a new root definition, hence
     * a later timestamp and a higher sequence number. Falls back to the epoch when the
     * nanopub carries no usable timestamp, so the sequence never moves backwards.
     */
    private static OffsetDateTime lastModifiedOf(Space space) {
        Calendar created = space.getNanopub() == null ? null : SimpleTimestampPattern.getCreationTime(space.getNanopub());
        return created == null ? OffsetDateTime.ofInstant(java.time.Instant.EPOCH, ZoneOffset.UTC) : toOffsetDateTime(created);
    }

    private static OffsetDateTime toOffsetDateTime(Calendar cal) {
        return OffsetDateTime.ofInstant(cal.toInstant(), ZoneOffset.UTC);
    }

    /**
     * The calendar's own year/month/day fields, read without any timezone conversion —
     * a bare {@code xsd:date} denotes a day, not an instant, so shifting it into another
     * zone would move the event to the wrong day.
     */
    private static LocalDate toLocalDate(Calendar cal) {
        return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /** The stable event identifier (the event space's IRI). */
    public String getUid() {
        return uid;
    }

    /** The event title. */
    public String getSummary() {
        return summary;
    }

    /** The event description, or null. */
    public String getDescription() {
        return description;
    }

    /** The event location (a place name or a URL), or null. */
    public String getLocation() {
        return location;
    }

    /** The Nanodash page for this event, or null. */
    public String getUrl() {
        return url;
    }

    /** Whether this is an all-day event, i.e. described by dates rather than instants. */
    public boolean isAllDay() {
        return startDate != null;
    }

    /** The start instant of a timed event, or null for an all-day event. */
    public OffsetDateTime getStart() {
        return start;
    }

    /** The end instant of a timed event, or null for an all-day event. */
    public OffsetDateTime getEnd() {
        return end;
    }

    /** The first day of an all-day event, or null for a timed event. */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** The day <em>after</em> the last day of an all-day event, or null for a timed event. */
    public LocalDate getEndDateExclusive() {
        return endDateExclusive;
    }

    /** When this event was last changed; feeds {@code LAST-MODIFIED} and {@code SEQUENCE}. */
    public OffsetDateTime getLastModified() {
        return lastModified;
    }

}
