# Space-ref identity (space IRI + root-definition NPID)

**Status:** 🚧 In progress — the identity model is decided and ref-aware detail
views ship today, but the per-ref `SpaceRepository` migration is still pending and
is coupled to the nanopub-query server roll-out.

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

Partly migrated:

| Aspect | State |
| --- | --- |
| `Space` identity carries the ref | ✅ `Space.getNanopubId()` returns the root NPID; `Space.getCoreInfoString()` = `id + " " + rootNanopubId` (`domain/Space.java`) |
| Admin seeded per root | ✅ `new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, rootNanopubId)` (`domain/Space.java`) |
| Ref-aware detail views | ✅ Info / heads views scope to a single ref via the `?_spaceNp_iri` placeholder, filled from `space.getNanopubId()` (wire param `spaceNp`, `component/AboutSpacePanel.java`) |
| Space *listing* keyed by ref | ❌ `SpaceRepository.build` still dedups by `space_iri`, first-row-of-`DESC(?date)` wins (`repository/SpaceRepository.java`) — i.e. **one `Space` per IRI**, so the globally-latest definition across all refs can hijack `rootNanopub` and flip-flop |

The remaining client work is the **`SpaceRepository` migration**: keep one `Space`
per *ref* (not per IRI), and move grouping-by-IRI / trust-gating / disambiguation
into display logic, threading the ref through every follow-up query (members,
roles, sub-spaces, maintained resources, …).

## Cross-repo dependency (why the listing change is on hold)

The server materializer and the published read queries have to move together:

- The full per-ref isolation design lives in
  `../nanopub-query/doc/design-spaceref-isolation.md`;
  `design-space-repositories.md` defines the ref format and the spaces graph.
- nanopub-query **1.15.0** (ref-only state) was deployed to `query.nanodash.net`
  and then **reverted**: it re-keyed `npa:` state edges (`isMaintainedBy`,
  `hasSubSpace`, `sameAsSpace`, authority rows) to refs, so the existing IRI-keyed
  published queries returned **empty-but-200** there. Because nanopub-java's
  `QueryCall` races the whole fleet and accepts the first 2xx, one such instance
  silently empties consumers fleet-wide — the 2026-06-12 mixed-fleet empty-home-page
  incident (`report-2026-06-12-mixed-fleet-spaceref-breakage.md`).
- **Phase 1.5** — a dual-key / bridge so bare-IRI reads return rows again (parity
  with 1.14.4), gated by `canary-checklist-spaceref-1.15.md`, which now exercises
  the `/api` read path, not just state shape — must land and pass before 1.15.x
  redeploys.

The released Nanodash (4.28.0) does **not** read the spaces repo, so a post-4.28.0
Nanodash can co-release with revised, ref-explicit grlc queries. The current
`get-spaces` head in code (`RAxGboS…/get-spaces`, IRI-keyed, dedup-by-IRI) is
therefore deliberately pinned; the ref-aware `get-spaces` will be published as a
**new independent nanopub** (no `npx:supersedes`, so running instances stay pinned),
pointed to on the co-release branch, then fork-merged at roll-out — the same pattern
used for the space-roles view.

## Status snapshot (measured 2026-06-12)

6 of 114 live spaces had multiple live refs (plantmetwiki ×4, PSE8-hackathon ×3,
incubator/project2 ×3, ReproNanopub / session31 / dggs4eo ×2). **All** had identical
root-admin sets across their refs (some with different *signers*), so a same-owner
collapse rule must compare **root-admin sets**, not signers; **zero** spaces would
hit a disambiguation UI today. The strays are cleanup candidates (republish with an
explicit `gen:hasRootDefinition` + invalidate), ideally before co-release so
duplicates never render.

## Related

- nanopub-query: `design-spaceref-isolation.md`, `design-space-repositories.md`,
  `canary-checklist-spaceref-1.15.md`,
  `report-2026-06-12-mixed-fleet-spaceref-breakage.md`
- [presets](presets.md) · [role-specific-views](role-specific-views.md)
