# Date placeholders

**Status:** ✅ Implemented — [#726](https://github.com/knowledgepixels/nanodash/issues/726)

## Goal

Let the person filling in a form decide how precise a date is, not the person who wrote the
template.

A literal placeholder fixes its datatype up front with `nt:hasDatatype`, and the form offers
the field for that datatype. A template that asks for an `xsd:dateTime` makes an author who
only knows "Tuesday afternoon" invent a time; one that settles for `xsd:gYear` cannot record
the day for the records that have it. A nanopublication cannot be edited afterwards, so a
literal published at the wrong precision stays that way.

## The type

```turtle
sub:date a nt:DatePlaceholder ;
  rdfs:label "Date of the talk" .
```

The field takes a value at whatever precision the author has, and the literal is typed by what
was entered:

| entered | published |
| --- | --- |
| `2026` | `"2026"^^xsd:gYear` |
| `2026` · November | `"2026-11"^^xsd:gYearMonth` |
| `2026` · November · 16 | `"2026-11-16"^^xsd:date` |
| `2026` · November · 16 · 09:00 · UTC+01:00 | `"2026-11-16T09:00:00+01:00"^^xsd:dateTime` |

A template can narrow the precisions it takes by listing their datatypes:

```turtle
sub:date a nt:DatePlaceholder ;
  nt:hasDatatype xsd:date, xsd:dateTime ;
  rdfs:label "Date of the talk" .
```

With none listed, all four are taken. A listed datatype that is not one of the four is ignored
(and logged). `nt:DatePlaceholder` is a literal placeholder, so it does not need
`nt:LiteralPlaceholder` next to it.

## The field

One field, in `LiteralFlexibleDateItem`, that offers every part up to the finest precision the
template takes from the start: year, month, day, time of day and timezone for a placeholder
that takes all four precisions, so it is as open as the template allows. A part finer than the
finest precision taken is never offered, so a template that takes `xsd:gYear` and
`xsd:gYearMonth` asks for no day.

The parts the *coarsest* precision taken needs have to be filled in and cannot be left out: a
placeholder that takes `xsd:date` and `xsd:dateTime` needs year, month and day ("choose month",
"choose day"), while time and timezone stay optional. The other parts may be left out ("no
month", "no day", an empty time), and how far the parts are filled in decides the precision and
so the datatype.

The time and timezone are entered with the same controls as an `xsd:dateTime` field: Kendo's
time picker (minutes, and seconds once the value has them or they are typed) and `ZoneChoice`,
the timezone dropdown that `AjaxZonedDateTimePicker` uses too. The timezone can always be
picked when the template takes `xsd:dateTime`; it only goes into the value together with a time.

What is checked:

- **That no part is skipped**: a day without a month, say, is reported on submit rather than
  dropped from the value.
- **That it is a point in time**: a real month, a day the month has (leap years included), and
  a time of the day.
- **That the precision is one the template takes.** This is only decided on submit, so the
  parts can be filled in in any order.
- **`nt:hasRegex`**, against the lexical form that would be published.

The timezone of a date and time starts at the offset the user's own timezone has *at the date
entered*, so a November date entered in October gets winter time. The user's timezone is the
one the browser reports, or the server's until the browser has said. The `xsd:dateTime` field
does the same now: its preselected timezone used to be the offset of today, whatever date was
picked.

## Existing values

A form opened over an existing nanopublication (supersede, derive, override) takes a value of
any precision the template accepts, whatever the field holds at that moment: the old version
may have been published with less, or more, than the author knows now. A value of a precision
the template does not take is treated like any other value that does not fit its
placeholder. A timezone the old value carried is kept, and so is the absence of one on a date and
time, which the timezone dropdown then offers as "no timezone".

`Template.getDatatype` returns null for a date placeholder. No single datatype stands for the
precisions it takes, and code that reads the datatype of a placeholder would otherwise take
one of them for the only one allowed. `Template.getDatePrecisions` lists them instead.

## Ordering

Result tables order these values sensibly already: `Utils.compareValues` compares run by run,
so `2026` sorts before `2026-11`, which sorts before `2026-11-16`, and all of them by time.
Dates and times in different timezones are ordered by their written form, not by the instant,
as before.

## Not covered

- `xsd:gMonthDay`, `xsd:gMonth`, `xsd:gDay`, and a time without a date. They do not fit the
  year → month → day → time sequence the field is made of. A plain `nt:LiteralPlaceholder`
  with that `nt:hasDatatype` still covers them.
- A date and time without a timezone, for new values. The timezone always has a value, the
  user's own by default.
- Durations, from/to ranges, a "now" default and timezone selection, all in
  [#444](https://github.com/knowledgepixels/nanodash/issues/444).

## Vocabulary

`nt:DatePlaceholder` is `https://w3id.org/np/o/ntemplate/DatePlaceholder`, declared in
`Template` next to the other terms awaiting a home in nanopub-java's `NTEMPLATE`.

The other option discussed in the issue was to repeat `nt:hasDatatype` on an ordinary
`nt:LiteralPlaceholder`. That would have changed what an existing term means for every reader
of it. A new type leaves existing templates as they are: `nt:hasDatatype` keeps its meaning on
every other placeholder, and only lists the accepted precisions on this one.
