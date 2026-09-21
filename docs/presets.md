# Presets for Nanodash

**Status:** ✅ Implemented — see [issue #302](https://github.com/knowledgepixels/nanodash/issues/302)

A **preset** is a named, publishable bundle of default views and roles that can
be applied to a resource page (a user, a space, or a maintained resource).
Instead of attaching views and roles to a resource one nanopublication at a
time, a maintainer publishes a preset once and then *assigns* it to as many
resources as needed.

This is the design for [nanodash issue #302](https://github.com/knowledgepixels/nanodash/issues/302).

## Overview

There are two separate concerns, each backed by its own nanopublication:

1. **Defining a preset** — declaring *what* the bundle contains (which views to
   show, which roles to set up) and which type of resource it is meant for.
2. **Assigning a preset** — stating that a *specific* resource should use a
   given preset.

Keeping these apart means one preset definition can be reused across many
resources, and an assignment can be added (or revoked) by a different user than
the one who defined the preset.

The whole design deliberately mirrors the existing **view display** mechanism
(`gen:ViewDisplay`, "Displaying a view for a resource"), so the same query and
aggregation logic applies, and the same activation/deactivation semantics carry
over.

All terms use the `gen:` namespace `https://w3id.org/kpxl/gen/terms/`.

## 1. Defining a preset

A preset is published as a nanopublication whose assertion describes a
`gen:Preset`. Like a resource view, it carries a stable *kind* (via
`dct:isVersionOf`) so that the identity survives across superseding versions.

```turtle
sub:preset a gen:Preset ;
    dct:isVersionOf sub:presetKind ;          # stable identity across versions
    rdfs:label "Nano Session" ;               # the preset name
    dct:description "..." ;                    # optional

    # which resource type(s) this preset is meant for (repeatable):
    gen:appliesToInstancesOf gen:Space ;       # or gen:IndividualAgent / gen:MaintainedResource
    gen:appliesToNamespace <...> ;             # optional, advanced

    # the bundled content (each repeatable and optional):
    gen:hasTopLevelView <a-resource-view> ;    # shown at the top level
    gen:hasView         <a-resource-view> ;     # shown by default
    gen:hasRole         <a-space-member-role> . # role definition to set up
```

The preset node is both an embedded (`nt:EmbeddedResource`) and introduced
local resource; the introduced `presetKind` is what other nanopubs and lookups
reference so that the link is version-independent — exactly as done for
resource views.

### Properties

| Property                   | Cardinality          | Range / value                                                          |
|----------------------------|----------------------|------------------------------------------------------------------------|
| `rdf:type`                 | required             | `gen:Preset`                                                           |
| `dct:isVersionOf`          | required             | the stable preset *kind*                                               |
| `rdfs:label`               | required             | the preset name (used as the nanopub label)                            |
| `dct:description`          | optional             | free text                                                              |
| `gen:appliesToInstancesOf` | repeatable           | `gen:IndividualAgent`, `gen:Space`, or `gen:MaintainedResource`        |
| `gen:appliesToNamespace`   | optional, repeatable | a URI prefix (advanced)                                                |
| `gen:hasTopLevelView`      | optional, repeatable | a `gen:ResourceView`                                                   |
| `gen:hasView`              | optional, repeatable | a `gen:ResourceView`                                                   |
| `gen:hasRole`              | optional, repeatable | a `gen:SpaceMemberRole`                                                |

`gen:hasView` and `gen:hasRole` are reused from the existing view-display and
space-role vocabulary rather than introducing preset-specific properties, so a
preset's views and roles are queryable with the same machinery already in
place. `gen:hasTopLevelView` distinguishes views that should be shown at the top
level of the page from the default `gen:hasView` placement.

Template: [Publishing a preset](https://w3id.org/np/RAjdBPJa3HQ1Oa5knoSQEs1ui6bf69iO8vGuEhoogRmcQ).

## 2. Assigning a preset to a resource

An assignment is a separate nanopublication that links a preset to a concrete
resource:

```turtle
sub:assignment a gen:PresetAssignment ;
    a gen:ActivatedPresetAssignment ;            # or gen:DeactivatedPresetAssignment
    gen:isAssignmentOfPreset <the-preset> ;
    gen:isAssignmentFor      <the-resource> .     # a space or maintained resource
```

The crucial design point — copied directly from view displays — is that **the
assignment is identified by the `(preset kind, resource)` pair, not by the
nanopublication's URI.** The `sub:assignment` node is a fresh local resource
minted in each nanopub; what ties two nanopubs together is that they describe an
assignment of the *same* preset for the *same* resource.

The key is the preset's stable **kind** (`dct:isVersionOf`), not the pinned
version the assignment references — exactly as a view display's identity is
`(view kind, resource)` and not the view version it points at. So assigning
another version of the same preset *replaces* the earlier assignment rather than
adding a second, competing one (see [Updating to a newer preset
version](#updating-to-a-newer-preset-version)). A preset version that declares no
kind keys on itself.

### Activation and cross-user deactivation

Activation state is expressed as an additional `rdf:type`:

- `gen:ActivatedPresetAssignment` (the default)
- `gen:DeactivatedPresetAssignment`

Because identity is by properties rather than by URI, **a different user — with
a different key — can deactivate an assignment they did not create**, simply by
publishing a new nanopublication that describes a `gen:PresetAssignment` for the
same `(preset, resource)` pair and types it as
`gen:DeactivatedPresetAssignment`. They do not (and cannot) supersede the
original nanopub, since `npx:supersedes` requires the original signing key.

Nanodash therefore resolves the effective state by **aggregating all
`gen:PresetAssignment` nodes for a given `(preset, resource)` pair**, considering
only assignments from agents who are authorized over the target (see
[Authority and aggregation](#authority-and-aggregation)) and letting the most
recent one win (latest-wins by publication time).

Template: [Assigning a preset to a resource](https://w3id.org/np/RA5shNOPHqtqUWkHnAWmff94G3wreqWUYYQFlHmrMTYzo).

## Authority and aggregation

Two rules govern which preset/view statements actually take effect on a page:

**1. Only authorized agents count.** When aggregating preset assignments — and
their resolved views and roles — only statements made by agents with authority
over the target resource are considered:

- for a space or maintained resource: its **admins and maintainers**;
- for a user page: **only the user themselves**.

Statements by anyone else are ignored for the purpose of what renders on the
page. (They remain valid nanopublications; they just don't drive the page's
default configuration.)

**2. Time ordering defines overriding.** The effective set of views on a page is
the union of preset-supplied views and directly-attached view displays, resolved
by publication time — latest-wins. Crucially, presets and individual view
displays live in **one shared pool** and override each other in **both
directions**:

- an individual `gen:ViewDisplay` (activated or deactivated) published *after* a
  preset assignment can deactivate or override a view the preset would otherwise
  contribute;
- conversely, a later preset assignment can override or re-activate a view that
  an earlier individual view display had set or removed.

So a preset is not a sealed bundle: each view it carries behaves as if it were an
individual view display contributed at the preset assignment's publication time,
and any later matching statement (preset-borne or standalone) for the same
`(view, resource)` pair supersedes it.

## Vocabulary summary

New terms proposed under `https://w3id.org/kpxl/gen/terms/`:

| Term                                | Kind     | Meaning                                                     |
|-------------------------------------|----------|-------------------------------------------------------------|
| `gen:Preset`                        | class    | a named bundle of default views and roles                   |
| `gen:PresetAssignment`              | class    | the assignment of a preset to a resource                    |
| `gen:ActivatedPresetAssignment`     | class    | marks an assignment as active (default)                     |
| `gen:DeactivatedPresetAssignment`   | class    | marks an assignment as deactivated                          |
| `gen:hasTopLevelView`               | property | preset → a view to show at the top level                    |
| `gen:isAssignmentOfPreset`          | property | assignment → the preset                                     |
| `gen:isAssignmentFor`               | property | assignment → the target resource                            |

Reused existing terms: `gen:hasView`, `gen:hasRole`, `gen:appliesToInstancesOf`,
`gen:appliesToNamespace`, `gen:IndividualAgent`, `gen:Space`,
`gen:MaintainedResource`, `gen:ResourceView`, `gen:SpaceMemberRole`,
`dct:isVersionOf`.

## Relation to view displays

The preset model is intentionally parallel to the view-display model, so the
implementation can largely follow the existing code paths:

| View displays                          | Presets                                  |
|----------------------------------------|------------------------------------------|
| `gen:ViewDisplay`                      | `gen:PresetAssignment`                   |
| `gen:ActivatedViewDisplay`             | `gen:ActivatedPresetAssignment`          |
| `gen:DeactivatedViewDisplay`           | `gen:DeactivatedPresetAssignment`        |
| `gen:isDisplayOfView`                  | `gen:isAssignmentOfPreset`               |
| `gen:isDisplayFor`                     | `gen:isAssignmentFor`                    |
| identity by `(view kind, resource)`    | identity by `(preset kind, resource)`    |
| "Displaying a view for a resource"     | "Assigning a preset to a resource"       |
| "Deactivating a view display ..."      | (covered by the deactivated type toggle) |

Reference view-display templates:
[Displaying a view for a resource](https://w3id.org/np/RAJnYnoOgXRJx31ad_Zm3__6jyvV6vuWCAKGFQCm4Xilo),
[Deactivating a view display for a user](https://w3id.org/np/RAZ47_4JquvEXk30HYnVeSgFRcQqHtpdibcfBOeqHI2j4).

## Decided

- **Conflict resolution / authority:** only assignments and view displays from
  agents authorized over the target are considered — admins and maintainers for a
  space or maintained resource, the user themselves for a user page. Among those,
  latest-wins by publication time. See
  [Authority and aggregation](#authority-and-aggregation).
- **Precedence:** preset-supplied views and directly-attached view displays share
  one pool and override each other in both directions, by publication time. A
  standalone view display can deactivate/override a preset's view and vice versa.

## Resolved decisions (as implemented)

These were open during design; the implementation has since settled them.

- **Top-level vs. default views:** distinct and built. `gen:hasTopLevelView`
  views are pinned to the resource's own page (shown at the top level);
  `gen:hasView` views leave their applicability to fall back to the view's own
  `appliesToInstancesOf` / `appliesToNamespace`. See `Preset` (`topLevelViews`
  vs `views`) and `ViewDisplay.forPresetView(…, topLevel, …)`.
- **Deactivation:** an assignment is deactivated by publishing a
  `gen:DeactivatedPresetAssignment` for the same `(preset kind, resource)` pair
  (latest-wins among authorized agents), so a different agent can deactivate one
  they did not create — mirroring view-display deactivation. The preset-assignments
  view exposes a per-row deactivate action; `PresetAssignment.isActive()` reads the
  type. (No separate "deactivating" template was needed.)
- **Assignment target granularity:** an assignment *references* a concrete preset
  version and that pin is what takes effect — a preset never auto-updates, which
  matters because presets carry roles. Assignment *identity*, though, is by preset
  kind (see above), so a newer version is adopted by assigning it, and the older
  assignment then loses on publication time.

## Updating to a newer preset version

Presets deliberately do not follow their kind forward on their own: the views and
especially the **roles** a resource picks up stay exactly the ones its admins
signed off on. Adopting a newer version is therefore an explicit act, and the
"🪟 Assigned presets" table on the About page makes it a one-click one
([issue #607](https://github.com/knowledgepixels/nanodash/issues/607)):

- The listing queries (`list-preset-assignments` and its ref-scoped twin) look up
  the newest non-invalidated version of the assigned preset's kind and report the
  verdict in a **`version` column**: `version_label` holds what the cell displays
  ("latest" or "⬆️ update available") and `version` the dates behind it. That way
  round because `QueryResultTable` displays a literal column's `_label` companion and
  puts the principal value in the hover tooltip — not the other way round. The newer
  version itself goes in `updatePreset`, which the action mapping hides from the table.
- Candidate versions are restricted to those published by the **same agent** as
  the assigned version, so an unrelated agent cannot advertise a version of
  someone else's preset in a space's About page.
- The views carry an "⬆️ update to latest version" entry action, gated to
  maintainers and above, that opens the page's own assignment template pre-filled
  with the newer version (`updatePreset:preset`). Publishing it makes the newer
  assignment the latest for that `(preset kind, resource)` pair; nothing needs to
  be deactivated first. Rows with no newer version carry an empty `updatePreset`,
  which hides the button for them.

### Who may assign, update and deactivate

The tiers differ by resource type, because the query service does:

- **Spaces: admins only.** `AuthorityResolver`'s ref-scoped assignment mirror and its
  preset-role attachment both require the publisher to hold `gen:hasAdmin` on the
  target ref, so a maintainer's assignment is never stamped into the space state and
  never reaches the space's "Assigned presets" table. The space view's three actions
  are therefore gated `gen:isVisibleTo gen:AdminRole`, matching what the server will
  honour — offering them to maintainers produced a publish whose views took effect
  while the listing and the roles ignored it.
- **Maintained resources and their parts: maintainers and above.** Those pages run the
  IRI-keyed listing, which accepts admins and maintainers alike, and preset-derived
  roles are not materialized for non-space targets at all
  (`presetAttachmentValidationUpdate` resolves no `?targetRef` and inserts nothing), so
  views are the only effect and they accept maintainers too.
- **User pages: the user themselves.** The owner counts as the sole admin of their own
  page and no one else holds a tier there, so the distinction does not arise.

Widening spaces to maintainers would mean a maintainer can attach role definitions to a
space by assigning a preset — the escalation surface presets are deliberately careful
about — so the narrower gate is the default, and changing it is a server-side decision
before it is a view-side one.

### What the query service already does — and where it still differs

Worth knowing, because it is not symmetric: **roles are already resolved by kind
server-side.** `AuthorityResolver.presetAttachmentValidationUpdate` maps an
assignment's preset reference to its `npa:presetKind` and then draws the roles from
the *latest live declaration of that kind* (steps 5–5c; see
`doc/design-preset-role-materialization.md`, "a superseded preset version's roles
never leak"). So the roles a resource picks up follow the newest preset version on
their own, whichever version an assignment pins — it is the **views** that stay
pinned, which is what makes the update action necessary in the first place.

What is still keyed on the pinned `(preset version, resource)` pair server-side is
**which assignment counts** — the latest-wins/anti-hijack filter in that same update
(step 7) and `presetDeactivationCheckWhere`. That is consistent with the update
action (assigning a newer version leaves the roles resolving to that version anyway),
but it diverges on **deactivation** while two versions of one kind are assigned:
Nanodash treats the newest row for the kind as decisive and drops the preset
entirely, whereas the materializer only sees that one *version's* assignment
switched off and keeps the role attachments alive through the older version's
assignment. Aligning it means keying those two filters on the kind as well — a
contained nanopub-query change; both blocks already have `?kind` in scope or one
join away.

Note also that "deactivate the old version and activate the new one in a single
nanopublication" is not an option: `SpacesExtractor.extractPresetAssignment` reads
only the first `gen:isAssignmentOfPreset` triple and emits one row keyed by the
nanopub's artifact code, so a second assignment node in the same nanopub is
silently ignored.

## Example nanopubs

Live instances now exist (so the assignment template's preset lookup returns
results):

- Preset definition — "Test preset" (a `gen:Preset` for `gen:Space`, bundling two
  roles and three views):
  [`RAYZhvi5...`](https://w3id.org/np/RAYZhvi5MXiwSw349j9-Gpjl9VjegdVnIdrki5U3HPiqo)
- Preset assignment — assigns "Test preset" to the `preset-test` space:
  [`RAofuHnw...`](https://w3id.org/np/RAofuHnwP_dJY3pwHDEoQeyLj56UMK8ANS7POG9g2fAFY)
