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
| Space *listing* ref-keyed | ✅ `SpaceRepository.build` reduces by **ref** (`reduceByRef`): one representative `Space` per IRI (the active ref) + captures all ref roots per IRI; fed by the live ref-aware `get-spaces` v3 (`GET_SPACES_REF`) |
| Per-space authority ref-scoped | ✅ admins, admin-pubkey-hashes, roles, members, observers all queried via `root_np` → `npa:forSpaceRef` (`Space.spaceQueryRef`), so multi-ref spaces don't merge authority across refs |
| Un-introduced members/observers | ✅ shown **with a ⚠️ flag** rather than hidden (validation = trust-approved `AccountState` from an accepted introduction); `Space.isMemberValidated()`, headerless `unverified_noheader` column in the Observers query |
| One `Space` per *ref* + disambiguation UI | ❌ `Space` is still IRI-identified (one representative per IRI). "N spaces claim this IRI" disambiguation is future |
| Sub-spaces / maintained-resources ref-scoped | ❌ still IRI-keyed (they work on the dual-keyed server; low-value, deferred) |

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
