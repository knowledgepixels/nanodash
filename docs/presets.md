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
assignment is identified by the `(preset, resource)` pair, not by the
nanopublication's URI.** The `sub:assignment` node is a fresh local resource
minted in each nanopub; what ties two nanopubs together is that they describe an
assignment of the *same* preset for the *same* resource.

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
| identity by `(view, resource)`         | identity by `(preset, resource)`         |
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
  `gen:DeactivatedPresetAssignment` for the same `(preset, resource)` pair
  (latest-wins among authorized agents), so a different agent can deactivate one
  they did not create — mirroring view-display deactivation. The preset-assignments
  view exposes a per-row deactivate action; `PresetAssignment.isActive()` reads the
  type. (No separate "deactivating" template was needed.)
- **Assignment target granularity:** assignments reference a preset (version)
  node, and resolution follows the supersedes chain to the latest version —
  exactly as views do (`PresetAssignment.getPreset()` → `Preset.get()` →
  `getLatestVersionId`). The version-independent `presetKind` (`dct:isVersionOf`)
  is captured too (`Preset.getPresetKindIri()`).

## Example nanopubs

Live instances now exist (so the assignment template's preset lookup returns
results):

- Preset definition — "Test preset" (a `gen:Preset` for `gen:Space`, bundling two
  roles and three views):
  [`RAYZhvi5...`](https://w3id.org/np/RAYZhvi5MXiwSw349j9-Gpjl9VjegdVnIdrki5U3HPiqo)
- Preset assignment — assigns "Test preset" to the `preset-test` space:
  [`RAofuHnw...`](https://w3id.org/np/RAofuHnwP_dJY3pwHDEoQeyLj56UMK8ANS7POG9g2fAFY)
