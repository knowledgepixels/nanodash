package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.calendar.CalendarEvent;
import com.knowledgepixels.nanodash.calendar.IcsWriter;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serves the iCalendar ({@code text/calendar}) representation of a space, at a stable URL
 * that doubles as a subscribable feed.
 *
 * <p>Which events the calendar contains follows from the space itself:</p>
 * <ul>
 *   <li>an <strong>Event</strong> space yields its own single event;</li>
 *   <li>any other space (a Program, typically) yields every direct sub-space of type Event
 *       that carries a start date — past events included, since dropping them would make
 *       some clients delete the user's history of the series.</li>
 * </ul>
 *
 * <p>The URL is the same whether it is fetched once ({@code &download=1}, offered as
 * "download .ics") or polled by a subscribed calendar client. That is deliberate: the
 * distinction lives entirely in how the user's calendar treats it, and a subscription only
 * keeps up with rescheduled events because every event here carries a UID derived from its
 * space IRI and a SEQUENCE derived from its root nanopub's timestamp. See {@link IcsWriter}.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code space}: the space IRI (required)</li>
 *   <li>{@code download}: present to serve as a file attachment rather than inline</li>
 * </ul>
 */
public class CalendarFeedPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(CalendarFeedPage.class);

    /**
     * The mount path for this page. Ends in {@code .ics} because some calendar clients
     * decide whether a URL is subscribable by looking at its extension rather than at the
     * content type.
     */
    public static final String MOUNT_PATH = "/calendar.ics";

    /**
     * How long a fetched calendar may be reused. Kept short: the whole point of a
     * subscription is that a rescheduled event reaches the subscriber, and an intermediary
     * caching the feed for longer would silently defeat that.
     */
    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);

    public CalendarFeedPage(final PageParameters parameters) {
        super(parameters);

        String spaceId = parameters.get("space").toString();
        if (spaceId == null || spaceId.isBlank()) {
            throw new AbortWithHttpErrorCodeException(400, "Parameter 'space' is required");
        }
        Space space = resolveSpace(spaceId);
        if (space == null) {
            throw new AbortWithHttpErrorCodeException(404, "No space found for id: " + spaceId);
        }

        boolean singleEvent = isEvent(space);
        List<CalendarEvent> events = singleEvent ? ownEvent(space) : subEvents(space);
        boolean asFeed = !singleEvent;
        String calendarName = space.getLabel();
        String calendarDescription = singleEvent ? space.getDescription()
                : "Events of " + calendarName + " — subscribed from Nanodash.";

        logger.info("Serving calendar for space {} ({} event(s))", spaceId, events.size());

        String ics = IcsWriter.write(calendarName, calendarDescription, events, asFeed);
        byte[] bytes = ics.getBytes(StandardCharsets.UTF_8);

        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(bytes);
            }

            @Override
            public String getContentType() {
                return "text/calendar; charset=utf-8";
            }
        };

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, filenameFor(space));
        handler.setContentDisposition(parameters.get("download").isNull()
                ? ContentDisposition.INLINE : ContentDisposition.ATTACHMENT);
        handler.setCacheDuration(CACHE_DURATION);
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * The absolute URL of this page for the given space — absolute because it is handed to
     * third-party calendar services, which fetch it themselves.
     *
     * @param spaceId  the space IRI
     * @param download true for the one-shot download variant, false for the subscribable feed
     * @return the full URL, including scheme and host
     */
    public static String urlFor(String spaceId, boolean download) {
        PageParameters params = new PageParameters().add("space", spaceId);
        if (download) params.add("download", "1");
        return Utils.absolutePageUrl(CalendarFeedPage.class, params);
    }

    /**
     * Whether a space describes an event in its own right, and so has a date of its own to
     * put in a calendar.
     *
     * @param space the space to test
     * @return true if the space is of type Event
     */
    public static boolean isEvent(Space space) {
        return space.isOfType(KPXL_TERMS.EVENT);
    }

    /**
     * The calendar events of the sub-events of a space: every direct sub-space of type
     * Event that carries a usable start date, earliest first.
     *
     * @param space the containing space (a Program, typically)
     * @return the events, possibly empty
     */
    public static List<CalendarEvent> subEvents(Space space) {
        List<CalendarEvent> events = new ArrayList<>();
        for (Space subSpace : subEventSpaces(space)) {
            CalendarEvent event = CalendarEvent.fromSpace(subSpace, SpacePage.urlFor(subSpace.getId()));
            if (event != null) events.add(event);
        }
        events.sort(Comparator.comparing(CalendarFeedPage::sortKey));
        return events;
    }

    /**
     * Whether a space contains any event worth subscribing to. Cheaper than
     * {@link #subEvents(Space)}, which every space page would otherwise pay for just to
     * decide whether to show a menu.
     *
     * @param space the containing space
     * @return true if at least one direct sub-space is a dated Event
     */
    public static boolean hasSubEvents(Space space) {
        for (Space subSpace : subEventSpaces(space)) {
            if (subSpace.getStartDate() != null) return true;
        }
        return false;
    }

    private static List<Space> subEventSpaces(Space space) {
        List<Space> events = new ArrayList<>();
        for (Space subSpace : SpaceRepository.get().findSubspaces(space)) {
            if (isEvent(subSpace)) events.add(subSpace);
        }
        return events;
    }

    private static List<CalendarEvent> ownEvent(Space space) {
        CalendarEvent event = CalendarEvent.fromSpace(space, SpacePage.urlFor(space.getId()));
        return event == null ? List.of() : List.of(event);
    }

    private static java.time.OffsetDateTime sortKey(CalendarEvent event) {
        return event.isAllDay()
                ? event.getStartDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                : event.getStart();
    }

    private static Space resolveSpace(String spaceId) {
        Space space = SpaceRepository.get().findById(spaceId);
        return space != null ? space : SpaceRepository.get().findByAltId(spaceId);
    }

    private static String filenameFor(Space space) {
        String label = space.getLabel();
        String base = (label == null || label.isBlank()) ? "calendar" : label;
        String safe = base.replaceAll("[^a-zA-Z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        if (safe.isEmpty()) safe = "calendar";
        if (safe.length() > 60) safe = safe.substring(0, 60);
        return safe + ".ics";
    }

}
