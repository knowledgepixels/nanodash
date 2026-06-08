# Role-specific views

**Status:** 🚧 In progress — tier model + `gen:isVisibleTo` parsing/matching + client-side filter implemented (steps 1–4); pending the `get-space-roles` `roleType` republish (P0), authoring UI, and the server-side filter.

A view display may declare that it is **visible only to viewers holding a given
role, or a given role tier (class)**, relative to the resource's governing
space. Targeting a *tier* (e.g. Maintainer) matches anyone at that tier or
above; targeting a *specific role IRI* matches only holders of that exact role.

This builds directly on the role-tier model that **nanopub-query already
defines and materializes** (see
[`../nanopub-query/doc/design-space-repositories.md`](../../nanopub-query/doc/design-space-repositories.md),
§"Role types"). It is meant to be compatible with the session-bound query
parameters in [magic-query-params](magic-query-params.md); the two share the
`?_CURRENTUSER` binding for the optional server-side path.

## Semantics & scope

This is **relevance-gating, not a security boundary.** The view, its query, and
the underlying nanopubs are all public; hiding a maintainer-only view from a
normal visitor declutters their page, it does not protect data. That lets the
first cut filter **client-side** with role data the page already loads.

Applies to **spaces** and **maintained resources** (which resolve a governing
space via `getSpace()`). On a user page (`IndividualAgent`) there is no space, so
role-gating degrades to the existing owner-vs-visitor check.

## The role model it rides on

nanopub-query types every role with a **tier** — a subclass of
`gen:SpaceMemberRole` (`gen:` = `https://w3id.org/kpxl/gen/terms/`) — and
materializes it per space as `npa:hasRoleType <tier>` alongside `npa:role
<roleIri>`. The tiers form a downward-grant chain:

| Tier IRI | Meaning |
| --- | --- |
| `gen:AdminRole` | hardcoded singleton `…/adminRole` (the same IRI as Nanodash's `ADMIN_ROLE_IRI`), defines `gen:hasAdmin` |
| `gen:MaintainerRole` | granted by an admin |
| `gen:MemberRole` | granted by an admin or maintainer |
| `gen:ObserverRole` | granted by anyone above, or self-attested; **default** when a role declares no tier |

The nanopub-query design doc explicitly leaves *per-tier privilege
enforcement — what each tier may do inside a space —* to Nanodash. View
visibility by tier is exactly such a privilege, so the query layer hands us the
tier and this feature decides what to do with it.

### The visibility ladder

For matching, Nanodash ranks tiers, with an **"Everyone" floor below Observer**:

```
Everyone (rank 0, = no triple) < Observer (1) < Member (2) < Maintainer (3) < Admin (4)
```

"Everyone" means literally anyone, including logged-out viewers with no role —
distinct from Observer, which is the lowest *assigned* tier (self-attestation is
still a deliberate step). **"Everyone" is not a nanopub-query role type** and is
not added to that repo's tier set — those are *grant* tiers, and you never
"grant everybody." It exists only as the absence-of-restriction default here.

## The predicate

A single predicate carries both targeting modes; its object is either a tier IRI
or a specific role IRI:

```turtle
<viewDisplay> gen:isVisibleTo gen:MaintainerRole .          # tier: this tier or above
<viewDisplay> gen:isVisibleTo <…/newsletterEditorRole> .    # specific role IRI
# (no gen:isVisibleTo triple)                                # Everyone (default, backward-compatible)
```

Disambiguation is trivial and safe: the tier IRIs are a fixed known set
(`gen:AdminRole` / `gen:MaintainerRole` / `gen:MemberRole` / `gen:ObserverRole`);
any other IRI is a specific role. Multiple `gen:isVisibleTo` triples are **OR**.

An explicit "Everyone" option in the authoring UI simply *omits* the predicate —
no IRI is minted for it. (A Nanodash-local sentinel would only be worth it if we
ever needed to distinguish "deliberately public" from "unset," which for
visibility does not appear to matter.)

## Matching

```
visible(viewer, space, display):
  reqs = display.isVisibleTo            // set of IRIs; empty => Everyone
  if reqs empty                         -> visible
  if space == null                      -> visible only if viewer is the page owner
  for X in reqs:
    if X is a tier IRI and userTier(viewer, space) >= rank(X)   -> visible   // tier threshold
    if X is a role IRI and viewerHoldsRole(viewer, space, X)    -> visible   // specific role
  hidden
```

- **`userTier`** = the highest-ranked tier among the roles the viewer holds in
  that space (Everyone/rank-0 if none).
- **No admin override.** An admin who does not personally hold a specific role
  does **not** see a view gated to that role — the page shows exactly what each
  role is entitled to. The management escape hatch is **self-assignment**: an
  admin can grant any non-admin-tier role (and every custom role is
  maintainer/member/observer tier), so an admin who needs a specific-role view
  simply publishes a role-instantiation for themselves. No special-case code.

## Where the assignment lives

Primary carrier: the **`ViewDisplay`** nanopub — it already models "show this
view for this resource" with per-display modifiers (`appliesTo`, page size,
structural position, deactivation). Visibility is one more modifier, so the
underlying View stays reusable and unrestricted elsewhere.

Two parallels for consistency (same predicate, same matching):

- **View default** — a `View` may carry a default `gen:isVisibleTo` that a
  `ViewDisplay` overrides, mirroring how View defaults page size / width /
  position flow into ViewDisplay.
- **Preset-bundled view** — a preset's `gen:hasView` / `gen:hasTopLevelView`
  reference may carry the same visibility so bundled views gate too.

## What Nanodash needs (all additive — it has no tier awareness today)

1. **Learn tiers.** `get-space-roles` (or `get-space-members`) must return
   `roleType` (= `npa:hasRoleType`); `SpaceMemberRole` gains a `tier` field. This
   dovetails with the in-progress grlc spaces-repo migration — same direction,
   consuming the materialized `npass:` state.
2. **Tier IRIs + rank** in `KPXL_TERMS` (`AdminRole` / `MaintainerRole` /
   `MemberRole` / `ObserverRole`) and a small rank helper with the Everyone floor.
3. **Parse `gen:isVisibleTo`** in `ViewDisplay` (and `View` for defaults).
4. **`Space` helpers** `userTier(iri)` (max tier held) and
   `viewerHoldsRole(iri, roleIri)`, plus `ViewDisplay.isVisibleTo(...)`.
5. **Apply the filter** (see next).

## Filter point — and the magic-param tie-in

**v1 (standalone — implement here first).** Filter in the existing view-display
aggregation in `AbstractResourceWithProfile` (the `get-view-displays` consumer,
where latest-wins / deactivation already happen). After aggregation, drop
displays where `isVisibleTo(currentUser, governingSpace)` is false. Uses
already-loaded `Space` role data; no query changes; no dependency on the
magic-param work.

**v2 (compatible evolution).** Push the filter server-side into
`get-view-displays` by binding the viewer identity as the `?_CURRENTUSER` magic
parameter from [magic-query-params](magic-query-params.md) — the same binding
hook. The spaces repo already materializes member → role → tier, so the JOIN
(viewer's instantiations vs the display's required role/tier, honoring the tier
order) is cheap. Same vocabulary and data model; only the enforcement point
moves.

## How it layers with action-gating

Three independent layers, coarse → fine, all over the same role model:

1. **View-display visibility** (this doc) — whole section shows/hides by viewer
   role/tier.
2. **Result/entry action gating** — `ButtonList` admin/member routing, plus the
   empty-into-required rule from [magic-query-params](magic-query-params.md).
3. **Per-row** visibility via conditional query binding.

A view can be member-visible yet carry an admin-only action button; the layers
compose.

## Known gaps this closes

Today's action gating is incidental — it depends on how each table happens to be
wired, not on anything the view declares — so it is already inconsistent. A
concrete example to fix when this lands:

- On a user's About page (`AboutUserPanel`), the **profile** table passes
  `resourceWithProfile(IndividualAgent…)`, so its "update profile image / license"
  actions route to `ButtonList`'s admin slot and are correctly owner-only. But
  the **presets** and **view-displays** tables (`AboutUserPanel.java:74-78`) are
  built *without* `resourceWithProfile`, so their "add preset…" / "add view
  display…" actions fall into the unconditional regular-button slot
  (`QueryResult.java:61`, `ButtonList.java:24-26`) and **leak onto other users'
  About pages**. Publishing would be ignored server-side (authorized-agents-only),
  but the button should not be shown.

Left as-is deliberately: rather than patch the per-call wiring, the declarative
mechanism makes the view/action declare its required role once, so every table
honours it regardless of construction — removing this whole class of
wiring-dependent inconsistency.

## Touch points

| Change | File |
| --- | --- |
| Tier IRIs + rank, `gen:isVisibleTo` term | `vocabulary/KPXL_TERMS.java` |
| `roleType` / tier field | `SpaceMemberRole.java`, `get-space-roles` (or `get-space-members`) query |
| `userTier` / `viewerHoldsRole` | `domain/Space.java` |
| Parse `gen:isVisibleTo` (+ View default) | `ViewDisplay.java`, `View.java` |
| `isVisibleTo(...)` + apply filter | `ViewDisplay.java`, `domain/AbstractResourceWithProfile.java` |
| Visibility selector in display-admin UI | `AddViewDisplayButton` / display authoring |

## Phasing

1. **Consume tiers**: tier IRIs + rank in `KPXL_TERMS`; `SpaceMemberRole.tier`
   (and the `get-space-roles`/`get-space-members` column).
2. **Parse + match + client-side filter** in `AbstractResourceWithProfile` —
   functional end-to-end here.
3. **Authoring UI**: a visibility selector ("Everyone" / tier / specific role)
   on the display-admin control.
4. *(Later, with magic params)* server-side filter in `get-view-displays` via
   `?_CURRENTUSER`.

## Relationship to nanopub-query

This feature is purely a **consumer** of the spaces-repo role state. Tiers,
grant rules, and the per-space validated member→role materialization all live in
nanopub-query
([design-space-repositories.md](../../nanopub-query/doc/design-space-repositories.md)).
Nanodash adds only the *privilege* interpretation — "a viewer at tier ≥ T (or
holding role R) may see this view display" — which that design explicitly leaves
to Nanodash. No new server-side role type is introduced (the Everyone floor is a
Nanodash-side default, not a tier).
