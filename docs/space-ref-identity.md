# Space-ref identity (space IRI + root-definition NPID)

**Status:** 🚧 In progress — the identity model is decided, the ref-aware spaces
backend is live (nanopub-query 1.16.0 on all instances), and per-space authority is
ref-scoped end to end. Remaining: the one-`Space`-per-ref identity flip + multi-ref
disambiguation UI, sub-space / maintained-resource ref-scoping, and the roll-out
fork-merge of the new spaces-repo queries.

## The problem

A space is named by an IRI (e.g. `https://w3id.org/kpxl/…/my-space`). Anyone can
publish a nanopublication declaring a *root definition* for that IRI. If identity
were the IRI alone, a second, rival root for an existing IRI would silently extend
— or take over — the real space: its admins would merge with the real ones, its
label could win, and so on.

## The decision (2026-06-12)

Identity is the **space ref** = **space IRI + root-definition NPID**, formatted
`<NPID>_<SPACEIRIHASH>` (where `SPACEIRIHASH = Utils.createHash(<space IRI>)`),
minted server-side in nanopub-query's `SpacesExtractor`.

So multiple root definitions for one IRI are **distinct spaces that each claim the
same IRI** — like two profiles claiming one ORCID — not a conflict to be resolved
by picking a "true" winner.

Consequences:

- **Nanopub Query stays a neutral materializer:** it makes all refs available and
  never selects a winner. (Selecting the earliest `dct:created` was explicitly
  rejected — timestamps are publisher-asserted and forgeable.)
- **The trust repo decides which refs are worth *showing*** (root admins / approved
  signers), not which one "wins".
- **Grouping, trust-gating, and "N spaces claim this IRI" become pure display
  concerns** in Nanodash, not data-model questions.

## Where Nanodash is today

Backend + per-space authority are migrated; the visible identity model (one `Space`
per ref) is not yet.

| Aspect | State |
| --- | --- |
| `Space` identity carries the ref | ✅ `Space.getNanopubId()` returns the root NPID; `Space.getCoreInfoString()` = `id + " " + rootNanopubId` (`domain/Space.java`) |
| Admin seeded per root | ✅ `new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, rootNanopubId)` (`domain/Space.java`) |
| Ref-aware detail views | ✅ Info / heads views scope to a single ref via the `?_spaceNp_iri` placeholder, filled from `space.getNanopubId()` (`component/AboutSpacePanel.java`) |
| About-tab listing views ref-scoped | ✅ Members, roles, sub-spaces, maintained resources (and observers, pending) all drive off ref-scoped `root_np` queries, falling back to the IRI-keyed view query when the ref root is unknown; the view nanopubs are left untouched (observers pattern). `AboutSpacePanel` computes one `refRoot` (the pinned `?root=` ref, else the representative) and keys every table to it. Sources at `docs/queries/list-*-ref.trig` |
| Content-tab views ref-scoped | ✅ the rendered Content views use a 2-param ref companion of the *renderer* query, `GET_VIEW_DISPLAYS_REF` (space IRI + root_np). `AbstractResourceWithProfile` routes its view-display fetch through it: `Space` overrides `getViewDisplayRefRoot()` → its representative ref, so the shared singleton (and the default page) scope to that ref; `?root=`-pinned pages fetch the pinned ref's displays on demand via `getTopLevelViewDisplays(refRoot)`. Verified on FAIR2Adapt: pinned RATXFO3Y renders 4 view sections incl. trainings, pinned RAkcKdzx renders 3 (trainings dropped). (`buildViewDisplays` null-guards the flaky federated response so a cold/failed fetch yields empty rather than crashing.) |
| About-tab view-displays listing ref-scoped | ✅ via a spaces-only **2-concrete-param** companion query `LIST_VIEW_DISPLAYS_REF` (space IRI + root_np). View displays aren't materialised into the spaces repo, so the query is a 4-`SERVICE` federated join, and RDF4J only propagates **concrete `VALUES`** bindings into the sub-services — a single auto-detecting param fails (a service-derived resource IRI → 0 rows in ref mode). The two-param form keeps the IRI concrete (for display matching) and resolves the ref + gates `npa:forSpaceRef` *inside* the `/repo/spaces` service. Verified on FAIR2Adapt: root RATXFO3Y (0234 admin) → 7 incl. 0234-signed trainings-view; root RAkcKdzx (0234 not admin) → 6, trainings dropped. `AboutSpacePanel` passes both params (IRI-keyed fallback when the ref root is unknown). It's a fork of `list-view-displays` (a ~200-line query under active iteration) — regenerable by re-applying the 2-line change (root_np VALUES + `?passedRef npa:rootNanopub` + `forSpaceRef ?passedRef`). NB: can't be tested via raw `GET` (URL truncation) or direct POST of the full query (the `viewLatest` block 503s the direct endpoint); test the gate by direct POST with `viewLatest` stripped, the full query via the grlc API (publish first; flaky like the deployed query) |
| Preset-derived roles ref-scoped | ✅ already materialised per-ref: the spaces materialiser emits each activated `PresetAssignment`'s roles as synthetic `gen:RoleAssignment`s with `npa:forSpaceRef` + `npa:derivedFromPreset`, so the ref-scoped roles query already surfaces them |
| Preset *assignment listing* ref-scoped | ❌ the only remaining gap — the `PresetAssignment` record is keyed by `npa:forResource <IRI>` (raw spacesGraph, no `forSpaceRef`), so the "Assigned presets" table still merges across refs. Closing it is a small nanopub-query change (stamp `forSpaceRef` on `PresetAssignment` — the materialiser already resolves the ref when minting the roles) or a client join through the derived roles. Per-ref **authority gate** for preset-role materialisation is unverified (no multi-ref space has a preset to test) |
| Space *listing* ref-keyed | ✅ `SpaceRepository.build` reduces by **ref** (`reduceByRef`): one representative `Space` per IRI (the active ref) + captures all ref roots per IRI; fed by the live ref-aware `get-spaces` v3 (`GET_SPACES_REF`) |
| Per-space authority ref-scoped | ✅ admins, admin-pubkey-hashes, roles, members, observers all queried via `root_np` → `npa:forSpaceRef` (`Space.spaceQueryRef`), so multi-ref spaces don't merge authority across refs |
| Un-introduced members/observers | ✅ shown **with a ⚠️ flag** rather than hidden (validation = trust-approved `AccountState` from an accepted introduction); `Space.isMemberValidated()`, headerless `unverified_noheader` column in the Observers query |
| Disambiguation UI | ✅ a single collapsible yellow notice (`<details class="ref-conflict-notice">` in `SpacePage`): the summary carries the short text (conflict on the default page, or "Viewing 1 of N…" on a `?root=`-pinned page), and expanding it reveals the claimants table inline — every root definition + its admins as a table-view-styled `result-table` (`SpaceClaimantsPanel`). Each row carries badges: **current** (the ref you're viewing — the pinned `?root=`, or the representative when not pinned) and **default** (the representative). The current row has no "view this space" link (you're already there); the others link to their `?root=`-pinned page. No separate `?claimants=true` page. Backed by a single `list-space-claimants` query (IRI → ref/root/admins, `LIST_SPACE_CLAIMANTS`) instead of the per-ref `get-space-admins` fan-out; a directly-rendered query, **not** an assignable view-display (disambiguation chrome must stay neutral/un-gated, and it's cross-ref). `?root=`-pinned pages scope the whole page to one ref (carried across tabs via `ResourceTabs`). `Space` is still IRI-identified (one representative per IRI), but each ref is now reachable and self-consistent |
| Sub-spaces / maintained-resources ref-scoped | ✅ scoped via the ref-level `npa:hasSubSpace` / `npa:hasMaintainedResource` edges (subject = the ref); sub-spaces take an extra child-ref→`npa:spaceIri` hop. Confirmed meaningful on live data (e.g. the `nanopub` space's 5 maintained resources sit on one of its three refs) |

## Server status

The cross-repo dependency that once gated this is **resolved**:

- nanopub-query is at **1.16.0** on all live instances — authority is **dual-keyed**
  (rows carry both `npa:forSpace` and `npa:forSpaceRef`), so IRI-keyed reads still
  work *and* ref-scoped reads are available. (History: 1.15.0 ref-only was reverted
  after the 2026-06-12 mixed-fleet empty-home-page incident; 1.15.1 Phase-1.5 bridge
  restored bare-IRI reads; 1.16.0 shipped. See `../nanopub-query/doc/`.)
- All new spaces-repo queries are published as **independent nanopubs** (no
  `npx:supersedes`), so deployed instances are unaffected; the client repoints to
  them. `GET_SPACES` now points at the ref-aware v3 (`GET_SPACES_REF`). Sources of
  the published queries are in `docs/queries/`.
- The eventual roll-out will **fork-merge** the new query heads with the old ones —
  the same pattern used for the space-roles view.

## Status snapshot (2026-06-16)

7 of ~115 live spaces have multiple live refs at the `get-spaces` filter level
(plantmetwiki ×4, project2 / Nanopublications-Hackathon ×3, FAIR2Adapt / session31 /
dggs4eo / ReproNanopub ×2). Most are same-owner strays (identical root-admin sets
across refs), but **FAIR2Adapt now diverges**: its two refs have *different* admin
sets (one ref grants an admin the other doesn't) — exactly the case a disambiguation
UI would surface. Today the client shows the representative ref (the active definition
from `get-spaces`), and the ref-scoped authority queries already keep each ref's
admins/roles/members from bleeding into the others. The same-owner collapse rule
should compare **root-admin sets**, not signers. Strays are cleanup candidates
(republish with an explicit `gen:hasRootDefinition` + invalidate).

## Related

- nanopub-query: `design-spaceref-isolation.md`, `design-space-repositories.md`,
  `canary-checklist-spaceref-1.15.md`,
  `report-2026-06-12-mixed-fleet-spaceref-breakage.md`
- [presets](presets.md) · [role-specific-views](role-specific-views.md)
