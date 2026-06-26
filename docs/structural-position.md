# Structural position

**Status:** ✅ Implemented — the `gen:hasStructuralPosition` predicate is parsed,
views are grouped by section and sorted by the full string, and a short
position label is shown in the view-displays tables. The **format is a
convention**, not yet validated in code, and only the **primary digits 3–7**
(intro … outro) are in active use today; the rest are reserved.

A view's *structural position* declares **where it sits on a page** relative to
the other views: which broad section it belongs to, and how it orders within
that section. It is the ordering/grouping mechanism asked for in
[#279](https://github.com/knowledgepixels/nanodash/issues/279) (alongside the
sibling `gen:hasPageSize` / display-width settings from the same issue).

## The predicate

`gen:hasStructuralPosition` (`gen:` = `https://w3id.org/kpxl/gen/terms/`,
`KPXL_TERMS.HAS_STRUCTURAL_POSITION`) attaches a **string literal** position to
either a **view** or a **view display**:

```turtle
sub:myView      gen:hasStructuralPosition "4.4.papers" .   # default for every display of this view
sub:myDisplay   gen:hasStructuralPosition "4.5.concepts" . # overrides the view's position here
```

A *view display* (`ViewDisplay`) may set its own position to override the
position inherited from the *view* (`View`) it displays — so the same view can
sit in different places on different resources. Resolution order
(`ViewDisplay.getStructuralPosition()`):

1. the view display's own `gen:hasStructuralPosition`, else
2. the view's `gen:hasStructuralPosition`, else
3. the default `"5.5.default"` (secondary section).

## The format

The canonical, **strict** form is:

```
[1-9] "." [1-9] "." [a-zA-Z0-9._-]+
```

i.e. `<section>.<sub>.<label>`:

| Component | Allowed | Meaning |
| --- | --- | --- |
| `<section>` | a single digit `1`–`9` | the broad page **section** (see table below) — the grouping key |
| `<sub>`     | a single digit `1`–`9` | the **order within the section** |
| `<label>`   | `[a-zA-Z0-9._-]+`      | a free, human-meaningful **identifier** to distinguish elements (e.g. `info`, `concepts`, `papers`) |

Only the **two leading components are constrained to single digits**; everything
from the second dot onward is the free `<label>`. The label may contain
**letters, digits, hyphens, underscores, and dots** — so it can itself be
subdivided with further dot-separated segments to order tightly-related
siblings, e.g. `4.5.concepts.1` and `4.5.concepts.2`, which the lexicographic
sort (below) keeps adjacent and in order. Examples in use: `3.2.info`,
`4.4.papers`, `5.5.default`, `5.6.roles`.

This format is **strict**: both leading components are single digits `1`–`9`,
each followed by a dot, before the label begins. There is **no `0`** in either
digit position. (As of today this is a documented convention only — the code
does not yet reject malformed positions; see *Not yet enforced* below.)

### The section digit (first component)

The first digit names one of nine page sections, from the top of the page to
the bottom:

| Digit | Section | In use? |
| --- | --- | --- |
| `1` | preamble  | reserved |
| `2` | header    | reserved |
| `3` | **intro**     | ✅ |
| `4` | **primary**   | ✅ |
| `5` | **secondary** | ✅ |
| `6` | **tertiary**  | ✅ |
| `7` | **outro**     | ✅ |
| `8` | appendix  | reserved |
| `9` | footer    | reserved |

**We currently only use the primary digits 3 to 7** (intro through outro) — the
content band of a page. Digits `1`, `2`, `8`, and `9` (preamble, header,
appendix, footer) are **reserved for later** and should not be assigned yet.
The default `5.5.default` falls in the middle (secondary), so an
undeclared view sorts after intro/primary content and before outro content.

## How it is used

### Grouping — by section digit

`AbstractResourceWithProfile.filterViewDisplays()` collects the active view
displays and **sorts** them (`Collections.sort`, see below). `ViewList` then
walks that sorted list once and starts a new group whenever the **first
segment** (the section digit, the substring before the first `.`) changes
(`ViewList.java`):

```java
String pos = vd.getStructuralPosition();
int firstDot = pos.indexOf('.');
String key = firstDot > 0 ? pos.substring(0, firstDot) : pos;   // e.g. "4" from "4.4.1.papers"
```

Each section group is rendered as its own `<div class="row-section">` (a
horizontal stripe). Only the **first digit** groups; the `<sub>` and `<label>`
do not start new groups, they only order *within* a group. Because the grouping
is a single forward pass over consecutive equal keys, **the list must be sorted
first** — which it is.

### Ordering — lexicographic over the whole string

`ViewDisplay` is `Comparable`; it compares on the **full position string**:

```java
public int compareTo(ViewDisplay other) {
    return this.getStructuralPosition().compareTo(other.getStructuralPosition());
}
```

So ordering is plain lexicographic (`String.compareTo`) over the entire
position, which is exactly why the strict digit-dot-digit prefix matters:
`"3.2.info" < "4.4.papers" < "5.5.default" < "5.6.roles"`. Restricting each of
the two leading components to a **single digit** keeps lexicographic order equal
to numeric order for them. Numeric and dotted `<label>` tails are fine; just
note that the sort stays lexicographic there too, so `"...10"` sorts before
`"...2"` — zero-pad if you need exact numeric order across ten or more siblings.

### The short label in tables

The view-displays tables (About tab) show a compact position cell: the
**first 3 characters** of the position (e.g. `4.4` from `4.4.papers`), with the
full literal on hover. This is computed SPARQL-side as
`substr(?position, 1, 3)` in `list-view-displays*` (see
`docs/queries/`) and rendered as a tooltip cell (`QueryApiAccess`).

## Where it lives

| Concern | Location |
| --- | --- |
| Predicate IRI | `vocabulary/KPXL_TERMS.java` (`HAS_STRUCTURAL_POSITION`) |
| Parse on a view | `View.java` (`getStructuralPosition`) |
| Parse + inheritance + default on a display | `ViewDisplay.java` (`getStructuralPosition`, default `"5.5.default"`) |
| Sort (Comparable) | `ViewDisplay.compareTo` |
| Sort call site | `AbstractResourceWithProfile.filterViewDisplays` (`Collections.sort`) |
| Group by section digit + render stripes | `component/ViewList.java` |
| Short label column | `list-view-displays*.trig` (`substr(...,1,3)`) + `QueryApiAccess` |

## Not yet enforced / future

- **No validation.** Nothing rejects a position that breaks the strict format
  (e.g. `"0.1.x"`, `"10.2.x"`, a missing digit, or a non-label tail). Malformed
  positions are treated as opaque strings: they still sort and group, just not
  where you would expect. Adding a validator (regex `^[1-9]\.[1-9]\.[a-zA-Z0-9._-]+$`)
  at authoring time is the obvious next step.
- **No semantics on the digits.** The code does not interpret "intro" vs
  "outro"; the section meaning is convention only. Grouping/sorting is purely
  structural.
- **Reserved sections.** Digits `1`, `2`, `8`, `9` (preamble, header, appendix,
  footer) are intentionally unused for now.
