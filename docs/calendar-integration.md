# Calendar integration

Issue [#440](https://github.com/knowledge-pixels/nanodash/issues/440).

An **Event** space (`gen:Event`) can be added to a user's calendar. A space that *contains*
Event sub-spaces — a **Program**, typically — can be **subscribed to**, so its whole series
of events lands in the user's calendar and keeps up as events are added or rescheduled.

Both are offered by a single dropdown next to the space title (`CalendarMenu`), and both are
served by a single endpoint (`CalendarFeedPage`, mounted at `/calendar.ics`).

## Where the data comes from

Everything is read from the space's **root-definition assertion**, via `Space`:

| Property | `Space` accessor | Notes |
| --- | --- | --- |
| `schema:startDate` | `getStartDate()`, `isStartDateOnly()` | required; without it there is nothing to put in a calendar |
| `schema:endDate` | `getEndDate()`, `isEndDateOnly()` | optional; **inclusive** (names the last day) |
| `schema:location` | `getLocation()` | optional; a place name or a URL (e.g. a video call) |
| `rdfs:label` | `getLabel()` | the event title |
| `dct:description` | `getDescription()` | the event description |

Both `xsd:date` and `xsd:dateTime` are accepted, and the difference is carried through rather
than normalised away. A bare date denotes a *day*, not an instant: a three-day hackathon
declared as `2026-07-14` must become an all-day event, not one starting at midnight UTC.
`isStartDateOnly()` records which form was used, and `CalendarEvent` maps it to
`DTSTART;VALUE=DATE` accordingly.

Note the off-by-one that this implies. `schema:endDate` is the last day of the event;
iCalendar's `DTEND` for an all-day event is *exclusive*. An event running the 14th to the
17th is written as `DTEND;VALUE=DATE:20260718`.

## Deciding whether a space is an Event

Use `Space.isOfType(KPXL_TERMS.EVENT)`, **not** a comparison against `Space.getType()`.

`getType()` comes from the `?type` column of the get-spaces query, which emits one row per
`npx:hasNanopubType` of the declaring nanopub. A root definition that carries several — a
space declared in the same nanopub as its preset assignment publishes `Event`,
`PresetAssignment` *and* `ActivatedPresetAssignment` — produces three rows, of which
`reduceByRef` keeps one as the representative, and it need not be the semantic one. Real
example: `…/officehours/20260715` reports `ActivatedPresetAssignment` as its type.

Relying on `getType()` here caused an Event page to serve an empty calendar and silently
dropped three of nine events from the Incubator Office Hours feed. `isOfType` reads the
root-definition assertion, which lists every declared type.

(The same wrinkle affects `getTypeLabel()` and `getTypeEmoji()`, so such spaces render with
the wrong label and emoji in listings. Not addressed here.)

## Why the UID has to be stable

A calendar client decides whether an incoming event is *new* or an *update to one it already
holds* by comparing `UID`s. So the UID is derived from the event space's IRI and nothing
else — it is identical on every fetch, forever. (An earlier draft used
`UUID.randomUUID()`, which meant every download produced a fresh event: re-downloading
duplicated it, and a rescheduled event could never replace the stale copy.)

Given a matching UID, the client then applies the update only if `SEQUENCE` has advanced.
`SEQUENCE` is derived from the creation timestamp of the space's current root-definition
nanopub, in whole minutes since the epoch: changing an event's date means publishing a new
root definition, which means a later timestamp, which means a higher sequence. Minute
granularity keeps the number inside the 32-bit range clients assume while still advancing on
every republication.

## What a subscription can and cannot promise

The feed URL and the download URL are the same URL. The difference is entirely in what the
user's calendar does with it:

- **Download** (`&download=1`, served as an attachment) copies today's dates into the
  calendar once. Nothing re-fetches it; a later change never arrives.
- **Subscribe** (the `webcal:` form of the same URL) makes the calendar client re-fetch it
  on a schedule of its own choosing and apply the diff.

Only the second can ever tell a user that an event moved. The feed advertises
`REFRESH-INTERVAL;VALUE=DURATION:PT1H` and `X-PUBLISHED-TTL:PT1H`, but these are *hints*, and
the honest summary of what clients do with them is:

| Client | Refresh cadence | Surfaces a change? |
| --- | --- | --- |
| Apple Calendar (macOS/iOS) | user-selectable, down to every 5 minutes | yes — updated events are flagged |
| Outlook / Microsoft 365 | roughly every few hours, not configurable | updates silently |
| Google Calendar | slow and unpredictable (commonly 8–24 h); ignores the hints | updates silently |
| Thunderbird and most desktop clients | user-configurable | usually yes |

So: subscribers *do* get the corrected date, but "notification" is the client's business, and
on Google in particular the change may take the better part of a day to appear. This is a
property of URL-subscribed calendars generally, not of this implementation.

Getting genuinely prompt, push-style notification would mean one of:

- **iTIP/iMIP** — publishing events with `METHOD:REQUEST` and an `ATTENDEE` line per
  participant, and emailing the updates. This is what makes a meeting invitation pop up as
  "the organiser has changed this event". It requires Nanodash to hold participant email
  addresses and to send mail.
- **CalDAV** — running (or federating with) a CalDAV server, which supports scheduling and
  push.
- **Web push from Nanodash itself**, independent of the user's calendar.

None of these is in scope here. The design keeps the door open for the first: the events
already carry stable UIDs and monotonic sequence numbers, which is precisely what an iTIP
`REQUEST` needs.

## Absolute URLs

Feed URLs are built with `Utils.absolutePageUrl`, which strips any `;jsessionid=` Wicket
appended. Wicket adds one whenever the client has not returned a cookie — precisely the case
for a calendar client fetching a feed. Such a URL is bound to one visitor's session: it stops
working when the session expires, and sharing it hands out a live session identifier.

Scheme and host are taken from the incoming request. **Behind a TLS-terminating reverse
proxy this needs checking**: nothing in the application reads `X-Forwarded-Proto`, so unless
the servlet container is configured to honour it, the emitted feed URL will say `http://`.
The `webcal:` link survives that (the scheme is replaced anyway), but the Outlook subscribe
link and the copy-feed-URL entry would hand out an `http` address.

## Feed contents

`/calendar.ics?space=<IRI>`:

- if the space is an **Event**, the calendar holds that one event;
- otherwise it holds every **direct sub-space of type Event** that carries a start date,
  earliest first.

Past events are included. Dropping them would shrink the feed, but an event vanishing from a
feed causes some clients to delete it from the user's calendar, taking their history of the
series with it.

Nesting is one level deep: an Event nested under a Track under a Program does not appear in
the Program's feed. If nested programmes become common, `subEventSpaces` is the place to make
the walk recursive.

## UI

`CalendarMenu.forSpace` builds the dropdown and decides its label:

| Space | Label | Entries |
| --- | --- | --- |
| Event | *Add to calendar* | download `.ics`, Google, Outlook |
| Contains Events | *Subscribe to events* | webcal, Google, Outlook, copy feed URL, download all |
| Both | *Calendar* | both sets |
| Neither | — | menu is not rendered |

The menu is shown to everyone who can see the space, not only to users who have declared
participation: the date is the least private thing about an event, and a visitor deciding
*whether* to participate is exactly who needs it.
