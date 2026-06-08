# Role-specific view actions

**Status:** 🚧 In progress — per-action `gen:isVisibleTo` gating implemented (tier model, matching, and filtering in the action renderers), and the `get-space-roles` `roleType` republish (P0) is published. Remaining: an authoring template field for `gen:isVisibleTo`, and the optional server-side path.

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

For matching, Nanodash ranks tiers, with an **"Everyone" floor below Observer**:

```
Everyone (rank 0, = no triple) < Observer (1) < Member (2) < Maintainer (3) < Admin (4)
```

"Everyone" means literally anyone, including logged-out viewers with no role —
distinct from Observer, the lowest *assigned* tier. **"Everyone" is not a
nanopub-query role type** (those are *grant* tiers, and you never "grant
everybody"); it exists only as the absence-of-restriction default here.

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
# (no triple)                                      # Everyone (default, additive)
```

The object is either a tier IRI or a specific role IRI; disambiguation is the
fixed tier set (`gen:AdminRole` / `gen:MaintainerRole` / `gen:MemberRole` /
`gen:ObserverRole`), anything else is a specific role. Multiple triples are **OR**.

## Matching

```
isViewerEntitled(reqs, viewer, governingSpace, viewerIsOwner):
  if reqs empty                      -> entitled (Everyone)
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

## Known gap this addresses

Today's incidental routing already leaks: on a user's About page
(`AboutUserPanel.java:74-78`) the **presets** and **view-displays** tables are
built without `resourceWithProfile`, so their "add preset…" / "add view display…"
actions fall into the unconditional regular bucket (`QueryResult.java:61`,
`ButtonList.java:24-26`) and show on *other* users' pages. Declaring
`gen:isVisibleTo` on those actions (e.g. owner/admin only) gates them regardless
of how the table was wired — the declarative fix for this class of bug.

## Touch points

| Change | File | Status |
| --- | --- | --- |
| Tier IRIs + `gen:isVisibleTo` term | `vocabulary/KPXL_TERMS.java` | done |
| `roleType` → tier field, rank/`isTier`, `isViewerEntitled` | `SpaceMemberRole.java` | done |
| `roleType` column | `get-space-roles` query republish (P0) | published |
| `userTier` / `viewerHoldsRole` | `domain/Space.java` | done |
| Parse `gen:isVisibleTo` on action nodes | `View.java` (`getActionVisibleTo`) | done |
| Filter actions in the renderers | `QueryResultTable(Builder)`, `QueryResultList(Builder)`, `QueryResultPlainParagraphBuilder` | done |
| `gen:isVisibleTo` field in the view-creation template | nanopub publish | todo |
| Server-side path (optional) | `get-view-displays` / action queries + `?_CURRENTUSER` | todo |

## Phasing

1. **Tier model** — IRIs, `SpaceMemberRole.tier`/rank/`isViewerEntitled`,
   `Space.userTier`/`viewerHoldsRole`, the `get-space-roles` `roleType` column. ✅
2. **Per-action parse + filter** — `View.getActionVisibleTo`, gates in the five
   action renderers (additive). ✅
3. **Authoring** — add an optional `gen:isVisibleTo` tier picker to the
   view-creation template (`…declaring-a-resource-view`), so authors set it when
   defining a view. Republish; no Nanodash change (templates are discovered
   dynamically). Specific-role targeting stays an advanced/hand-authored case.
4. *(Optional, later)* server-side enforcement via the `?_CURRENTUSER` magic
   parameter, once [magic-query-params](magic-query-params.md) lands.

## Relationship to nanopub-query

This feature is purely a **consumer** of the spaces-repo role state. Tiers, grant
rules, and the per-space validated member→role materialization all live in
nanopub-query
([design-space-repositories.md](../../nanopub-query/doc/design-space-repositories.md)).
Nanodash adds only the *privilege* interpretation — "a viewer at tier ≥ T (or
holding role R) may use this action" — which that design explicitly leaves to
Nanodash. No new server-side role type is introduced (the Everyone floor is a
Nanodash-side default, not a tier).
