# Role-specific view actions

**Status:** ✅ Implemented — per-action `gen:isVisibleTo` gating (tier model, matching, and filtering in the action renderers), the `get-space-roles` `roleType` republish (P0), and the authoring template field are all published. Only an optional performance tweak remains (see Phasing).

A view's **action button** can declare that it is shown only to viewers holding a
given role, or a given role **tier** (class), relative to the resource's
governing space. Targeting a *tier* (e.g. Maintainer) matches anyone at that tier
or above; targeting a *specific role IRI* matches only holders of that exact role.

This gates individual *actions*, not whole views — a view is shown to everyone,
but (say) its "retract" or "add member" button only appears for maintainers. It
builds on the role-tier model that **nanopub-query already defines and
materializes** (see
[`../nanopub-query/doc/design-space-repositories.md`](../../nanopub-query/doc/design-space-repositories.md),
§"Role types").

## Semantics & scope

This is **relevance-gating, not a security boundary.** The template behind an
action and the underlying nanopubs are public; hiding a button only declutters —
publishing is still authorised server-side. That lets the gate run **client-side**
with role data the page already loads.

The governing space is the resource the action is rendered for: the space itself
for a space page, the owning space for a maintained resource, and none for a user
page (`IndividualAgent`). A **user page is treated as a degenerate space whose
sole admin is the owner** — so the owner holds the admin tier (any tier-gated
action shows only to them) and no one else holds any role. Specific-role gates are
unholdable on a user page and therefore match nobody. (When agents can later
*observe* a user, `userTier` returns Observer for them and observer-gated actions
start matching automatically — no special-casing.)

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
enforcement — what each tier may do inside a space —* to Nanodash. Gating an
action by tier is exactly such a privilege, so the query layer hands us the tier
and this feature decides what to do with it.

### The visibility ladder

For matching, Nanodash ranks tiers, with a `gen:EveryoneRole` floor below Observer:

```
gen:EveryoneRole (0) < Observer (1) < Member (2) < Maintainer (3) < Admin (4)
```

`gen:EveryoneRole` means literally anyone, including logged-out viewers with no
role — distinct from Observer, the lowest *assigned* tier. It is a
**Nanodash-side visibility sentinel, not a nanopub-query grant tier** (those are
admin/maintainer/member/observer — you never "grant everybody"). It earns an
explicit IRI because the view-creation template **cannot leave the per-action
visibility statement optional inside a repeated action group**, so it needs a
concrete "no restriction" value to use as the default. A hand-authored action
that simply omits `gen:isVisibleTo` is also visible to everyone — omission and
`gen:EveryoneRole` are equivalent.

## The predicate — on the action node

`gen:isVisibleTo` attaches to the **action node inside the view nanopub** — the
IRI that is the object of `gen:hasViewAction` and carries `gen:hasActionTemplate`
plus the `gen:ViewAction` / `gen:ViewEntryAction` type. Not on the view itself,
and **not** on the shared action template (which is reusable across views).

```turtle
sub:myview gen:hasViewAction sub:retractAction .
sub:retractAction a gen:ViewEntryAction ;
    gen:hasActionTemplate <…/retract-template> ;
    gen:isVisibleTo gen:MaintainerRole .          # tier: this tier or above
# sub:retractAction gen:isVisibleTo <…/someRole>  # or a specific role IRI
# sub:retractAction gen:isVisibleTo gen:EveryoneRole  # everyone (the authored default)
# (no triple at all)                              # also everyone (hand-authored)
```

The object is either a tier IRI or a specific role IRI; disambiguation is the
fixed tier set (`gen:AdminRole` / `gen:MaintainerRole` / `gen:MemberRole` /
`gen:ObserverRole`), anything else is a specific role. Multiple triples are **OR**.

## Matching

```
isViewerEntitled(reqs, viewer, governingSpace, viewerIsOwner):
  if reqs empty                      -> entitled (Everyone)
  if reqs contains gen:EveryoneRole  -> entitled (everyone, incl. anonymous)
  if governingSpace == null:                            // user page = owner is sole admin
    tier = viewerIsOwner ? Admin : Everyone
    return any tier-IRI X in reqs with tier >= rank(X)  // specific roles unholdable here
  if viewer == null                  -> not entitled
  for X in reqs:
    if X is a tier IRI and governingSpace.userTier(viewer) >= rank(X) -> entitled
    if X is a role IRI and governingSpace.viewerHoldsRole(viewer, X)  -> entitled
  not entitled
```

- **`userTier`** = the highest-ranked tier among the roles the viewer holds in
  that space (Everyone/rank-0 if none).
- **No admin override.** An admin who does not personally hold a specific role
  does **not** see an action gated to that role. The escape hatch is
  **self-assignment**: an admin can grant any non-admin-tier role (every custom
  role is maintainer/member/observer tier), so an admin who needs the action
  publishes a role-instantiation for themselves. No special-case code.

Implemented as `SpaceMemberRole.isViewerEntitled(reqs, viewer, space, owner)` plus
a convenience overload `isViewerEntitled(reqs, resourceWithProfile)` that resolves
viewer/space/owner from the rendered resource.

## Where it's enforced

The action renderers drop an action whose `gen:isVisibleTo` the viewer does not
satisfy, **before** the button reaches `ButtonList`:

- result actions: `QueryResultTableBuilder.addViewActions`,
  `QueryResultListBuilder`, `QueryResultPlainParagraphBuilder`
- entry (per-row) actions: `QueryResultTable`, `QueryResultList`

This is **additive**: an action without `gen:isVisibleTo` renders exactly as
before. It composes with — does not replace — the existing `ButtonList` routing
(see next).

## Relationship to today's `ButtonList` routing

Existing action visibility is coarse and wired by resource *type*, not declared:
`ButtonList` (`ButtonList.java:24-46`) has three buckets — regular (everyone),
member (space member), admin (space admin / user-page owner) — and `QueryResult`
/ `QueryResultTable` route a view's actions into a bucket based on whether the
resource is an `IndividualAgent`, `Space`, etc. That is why entry actions are
never gated and result actions are owner-gated only on user pages.

`gen:isVisibleTo` is the precise, declarative gate layered on top. For now it only
*adds* a filter; a later step could let a declared action fully replace the
resource-type routing (and fix the leak below at the source).

## Known gap this addresses — fixed

Incidental routing used to leak: the **presets** and **view-displays** tables in
`AboutUserPanel` / `AboutSpacePanel` / `AboutResourcePanel` were built without
`resourceWithProfile`, so their "add preset…" / "add view display…" actions fell
into the unconditional regular bucket (`QueryResult.java:61`,
`ButtonList.java:24-26`) and showed on *other* users' (and visitors') pages.

Fixed in two parts:

- the panels now pass `resourceWithProfile` (owner-gates the user-page case via
  the `IndividualAgent` admin bucket, and lets the per-action filter resolve the
  governing space on space/resource pages);
- the two views were republished with `gen:isVisibleTo gen:MaintainerRole` on
  their action (plus complete `"void"` action fields so the action group
  round-trips on edit), gating space/resource pages too. Current latest heads:
  `preset-assignments-view` → `RA4fgqTAYcaKiHNA8NZ1JgMlJI4JAgfh7B9YOJsfRQIFE`,
  `view-displays-view` → `RABs0d67G0oOPlZZ28Y-x-U6_W6geJxpFO8K8fwCaPB0k`
  (picked up automatically via `View.get`'s latest-version resolution — no
  constant change needed).

So those actions now show only to admins/maintainers (or the owner on a user
page) on every page type — the declarative fix for this class of bug, and the
feature's first real use.

**Watch out for forked version chains.** The view-displays-view chain had two
latest heads — an earlier republish was built on a stale base (`RAVVUjFM…`)
while a separate June-5 head (`RAqh-ZN9…`, with a newer `list-view-displays`
query) had already branched from it. With two heads, `getLatestVersionId`
resolves ambiguously, so the gate appeared not to take effect on some pages.
Fixed by publishing one version (`RABs0d67…`) that `npx:supersedes` **both**
heads — merging the newer query with the gating/void — collapsing the fork to a
single head. When republishing a view, resolve the actual current head(s) first
(query `get-latest-version-of-np`) rather than assuming the constant's IRI is
latest.

## Touch points

| Change | File | Status |
| --- | --- | --- |
| Tier IRIs + `gen:isVisibleTo` term | `vocabulary/KPXL_TERMS.java` | done |
| `roleType` → tier field, rank/`isTier`, `isViewerEntitled` | `SpaceMemberRole.java` | done |
| `roleType` column | `get-space-roles` query republish (P0) | published |
| `userTier` / `viewerHoldsRole` | `domain/Space.java` | done |
| Parse `gen:isVisibleTo` on action nodes | `View.java` (`getActionVisibleTo`) | done |
| Filter actions in the renderers | `QueryResultTable(Builder)`, `QueryResultList(Builder)`, `QueryResultPlainParagraphBuilder` | done |
| `gen:isVisibleTo` field in the view-creation template | nanopub `RA8_hijwsfGCryMYtjtEpec21ZSNY68-qmL0bHRWR0sWM` | published |

## Phasing

1. **Tier model** — IRIs, `SpaceMemberRole.tier`/rank/`isViewerEntitled`,
   `Space.userTier`/`viewerHoldsRole`, the `get-space-roles` `roleType` column. ✅
2. **Per-action parse + filter** — `View.getActionVisibleTo`, gates in the five
   action renderers (additive). ✅
3. **Authoring** ✅ — published as
   `RA8_hijwsfGCryMYtjtEpec21ZSNY68-qmL0bHRWR0sWM` (supersedes the
   `…declaring-a-resource-view` chain). Each action in the repeatable `st50` group
   now carries a `gen:isVisibleTo` `nt:GuidedChoicePlaceholder`:
   - fixed `nt:possibleValue`s for the tiers (`EveryoneRole`, `ObserverRole`,
     `MemberRole`, `MaintainerRole`, `AdminRole`);
   - `nt:possibleValuesFromApi …find-things?type=…SpaceMemberRole` to offer
     published specific roles (the same source the role-assignment template uses);
   - free text still allowed for any other role IRI.

   The statement **cannot be optional** (a known limitation: optional statements
   inside a repeated group aren't supported yet), so it carries
   `nt:hasDefaultValue gen:EveryoneRole` — the explicit "no restriction" value.
   Republishing requires the template's original **Nanodash-web signing key** (it
   was created there, not with the local CLI key), so it must be superseded via
   Nanodash-web or by signing with that key. No Nanodash code change (templates are
   discovered dynamically).
4. *(Optional, later — performance only)* have a query return the viewer's tier
   in a space directly, so Nanodash needn't load the full role set to compute
   `userTier`. This is **not** a security boundary — action gating is client-side
   relevance-gating over public data, so there is nothing to "enforce"
   server-side; it would only save a fetch on busy pages.

## Relationship to nanopub-query

This feature is purely a **consumer** of the spaces-repo role state. Tiers, grant
rules, and the per-space validated member→role materialization all live in
nanopub-query
([design-space-repositories.md](../../nanopub-query/doc/design-space-repositories.md)).
Nanodash adds only the *privilege* interpretation — "a viewer at tier ≥ T (or
holding role R) may use this action" — which that design explicitly leaves to
Nanodash. No new server-side role type is introduced: `gen:EveryoneRole` is a
Nanodash-side visibility sentinel (the rank-0 "no restriction" default), never a
grantable nanopub-query tier.
