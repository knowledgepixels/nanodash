# Section anchors

**Status:** ✅ Implemented — every view display rendered by `ViewList` carries an
HTML `id`, a "#" handle next to its title copies the link, and the fragment is
re-applied as Ajax-loaded sections arrive.

Display views generate **fragment identifiers**, so a single section of a page
can be linked to directly instead of only the page as a whole:

```
https://w3id.org/spaces/nanosuggestions#messages
```

(and equivalently on the Nanodash page for it,
`.../space?id=https://w3id.org/spaces/nanosuggestions#messages`).

## Where the anchor comes from

`ViewAnchors.forViewDisplays` derives one anchor per view display, in render
order:

1. a slug of the display's **title** — what the reader sees as the section
   heading, which is what makes a shared link self-explanatory;
2. failing that (the display carries no title of its own), a slug of the **label
   of its query**, because that is what the view panels then show as the heading;
   a title set to the empty string is not a missing title but the instruction to
   hide the heading, so it does not reach for the query label;
3. failing that, the **label part of the structural position** — `papers` of
   `"4.4.papers"`, see [structural-position](structural-position.md) — except for
   the meaningless default `"5.5.default"`;
4. failing that, the generic `view`.

Slugging (`ViewAnchors.slugify`) lower-cases, folds accents onto their base
letter (`Übersicht` → `ubersicht`), and turns every other non-alphanumeric run
into a single hyphen — so a leading emoji, which view titles often carry, simply
disappears: `💬 Messages` → `messages`. Emoji are dropped *before* that folding,
because the letterlike ones decompose into real letters and would otherwise leak
into the anchor: `ℹ️ Info` → `info`, not `i-info`. Slugs are capped at 60 characters at a
word boundary, and a digit-initial slug gets a `view-` prefix so it stays usable
as a CSS id selector.

A title written entirely in a non-Latin script leaves nothing behind, so it falls
through to rule 3 or 4 above — such sections are anchored by their structural
position label, or end up as `view`, `view-2`, … Percent-encoded fragments would
be the alternative; readable ASCII links won out.

Anchors are made **unique within the page** by appending `-2`, `-3`, … to later
duplicates, and the set of taken ids starts out holding the page chrome's own ids
(`content-pane`, `titlebar`, …) so a section can never shadow them.

Consequence: anchors are **as stable as the titles are**. Renaming a view breaks
links into it, the same as on any documentation site; moving a section elsewhere
on the page does not.

## Where it is applied

`ViewList` computes the anchors over the whole view-display list (uniqueness is
page-wide) before grouping them into sections, then sets each one as the `id` of
the **list item wrapping the view panel** — not inside the panel — so the anchor
is in the markup from the first render even while the panel itself is still
loading over Ajax.

That covers every page that renders views through `ViewList`: spaces, users,
maintained resources, projects, and resource parts.

Some pages assemble their view panels themselves instead (the list pages, the
explore page, …), so there is no list item to carry the anchor. They hand out
their own anchors with `ViewAnchors.Allocator` — same derivation, same
page-wide uniqueness — and `Allocator.anchor(panel, viewDisplay)` puts each one
on the panel, as its **markup id** plus the marker class `view-section`. Using
the markup id rather than a plain `id` attribute matters: a panel whose query
results are still loading replaces itself over Ajax, and Wicket addresses it by
exactly that id, so the anchor survives the swap.

Done so far for the About tab of a space (`AboutSpacePanel`) and the Explore
tab (`ExplorePanel`, which is the Explore tab of every resource type). Still
uncovered: the standalone explore/references/result-table/query pages,
`ProjectPage`, `ListPage`, and `ViewResultsPage`. (The user/space/query list
pages that were early adopters have been retired; their content lives on the
home page as regular `ViewList` sections now.)

## Linking and scrolling (`nanodash.js`)

- `addSectionAnchors()` appends a faint `#` handle to each section title
  (`.paneltitlerow > h4`, `.view-header-titlerow > h3`, …) of every section it
  finds — `.view-group > .listview[id]` for `ViewList` pages, `.view-section[id]`
  for the panels that are their own section — but only to sections that actually
  carry a fragment identifier, so a handle is never a link to the bare `#` (i.e.
  to the top of the page). It is a real link to
  the fragment, and clicking it additionally copies the **absolute** link to the
  clipboard with the usual toast. It is idempotent and re-runs after Wicket Ajax,
  so sections that appear later get their handle too.
- Most view displays load over Ajax **after** the initial render, so at the
  moment the browser handles the fragment its target usually does not exist yet.
  `startAnchorTracking()` / `scrollToAnchor()` therefore keep re-scrolling to the
  target as sections arrive, until the page settles (15 s) or the user scrolls
  themselves — whichever comes first. `hashchange` restarts the tracking.

`.view-group > .listview` and `.view-section` carry a `scroll-margin-top` so a
linked section doesn't land flush against the top of the viewport.

The handle also has to be the **topmost element** where it is drawn, which is not
free: a title row can carry positioned overlays — the nanopub-set view pins its
filter and menu into its row with `.view-selector.with-source` — and such an
overlay silently takes the click. It is kept off the handle from both sides: the
overlay shrinks to its own content (`width: auto`, it would otherwise inherit the
base rule's `width: 100%` and blanket the whole row), and `.section-anchor` sits
at `z-index: 1`.

## Where it lives

| Concern | Location |
| --- | --- |
| Anchor derivation + uniqueness | `ViewAnchors.java` |
| Setting the `id` on each section | `component/ViewList.java`, `ViewAnchors.Allocator` for pages building their own panels (e.g. `component/AboutSpacePanel.java`) |
| "#" handle, copy-link, scroll tracking | `script/nanodash.js` |
| Handle styling + scroll margin | `webapp/style.css` (`.section-anchor`) |
| Tests | `ViewAnchorsTest`, `component/ViewListAnchorTest` |
