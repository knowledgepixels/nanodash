# Views as space-maintained resources

**Status:** design / not yet implemented
**Goal:** let a space *govern a view definition* — control which version of a view is
authoritative — by declaring the view as a maintained resource of that space.

## Motivation

Today governance lives at the **assignment** layer: a space controls *where* a view is
displayed (view displays, `npa:hasGoverningSpaceRef`), but the view *definition* itself is a
free-floating nanopub anyone can fork. We want governance at the **definition** layer: a
space decides which version of a view-kind is canonical, so a stray fork by an
unauthorized agent is never treated as "the view".

## Core idea

1. A view-**kind** IRI (the `dct:isVersionOf` target shared across a view's versions) is
   declared a maintained resource of a space:

   ```
   <kind> a gen:MaintainedResource ; gen:isMaintainedBy <space> .
   ```

2. The canonical version of that kind is resolved by **authority-scoped latest-wins** — no
   `npx:supersedes` chain required:

   > canonical = newest (`dct:created`) nanopub that (a) is `dct:isVersionOf <kind>` and
   > (b) is signed by an admin/maintainer of the governing space.

   An authorized publisher updates the view simply by publishing a fresh nanopub
   re-expressing the same kind; it wins by being the newest *authorized* one. A fork by an
   unauthorized agent never enters the candidate set, regardless of any supersedes claim.

This mirrors how presets already resolve "latest declaration per kind" (`dct:isVersionOf` +
`MAX(dct:created)`, authorization-scoped). It is *more* robust than supersedes-based
resolution, since `npx:supersedes` is publisher-asserted and not authority-backed.

## Per-pair governance (primary model)

The materializer already keys maintained resources **per `(resource, space)` pair** and
emits a ref-valued `npa:hasGoverningSpaceRef`. So a single kind IRI can be
`isMaintainedBy` **multiple spaces simultaneously** — two declarations, two edges, side by
side. There is no global "who owns kind K" decision and no timestamp winner-picking
(avoiding the trap in `space-ref-identity.md`).

The durable unit you commit to is therefore the **`(kind, space-ref)` pair**:

- **Assigning** a view (a view display) pins a specific pair. The assigned view IRI names
  its maintaining space (see "Triple placement"), which selects the pair.
- **Resolution** is space-scoped: from the assigned view IRI, read the kind *and* the
  committed space-ref, then resolve the latest authorized version of that kind **under that
  space-ref**. Same kind assigned via two spaces → two independent update streams.

Global-single-maintainer is just the degenerate case (a kind maintained by exactly one
space).

### Trust story

Assigning a view = **explicitly delegating update authority to the chosen space**. The
system never guesses the canonical maintainer; the assigner chooses, per assignment.
Consistent with "selection is trust/namespace-delegated, not timestamp"
(`space-ref-identity.md`).

## Triple placement

The maintenance triple is about the **kind**, with the **kind IRI as subject** (never a
per-version nanopub IRI).

Carrying `<kind> gen:isMaintainedBy <space>` (alongside the always-present
`<version> dct:isVersionOf <kind>`) **on every version nanopub** is recommended — not
redundant. It makes the view IRI self-describing: whoever holds the view IRI has the
governing space locally in-hand, which is what lets an assignment pin a `(kind, space-ref)`
pair. It is also resilient to ingest ordering and idempotent in materialized state (the
`isMaintainedBy` edge is the same triple however many nanopubs assert it).

**Caveat — the inline triple is not self-validating.** The materializer's admit gate is
admin-only and re-evaluated per nanopub. A version published by a non-admin maintainer
still carries the inline triple, but its maintenance *declaration* is not admitted (the edge
already stands from an admin's assertion). Consumers must therefore resolve governance from
the **materialized** `npa:isMaintainedBy` edge, treating the inline triple as a hint, not a
fact.

## Two implementation paths

### A. No materializer change (start here)

Relies entirely on existing materialized state.

- **Published nanopubs:**
  - The kind declaration: `<kind> a gen:MaintainedResource ; gen:isMaintainedBy <space>`
    (existing extractor validates the admin gate, emits `npa:hasGoverningSpaceRef` for free).
  - A **federated** consumer query nanopub that joins the registry
    (`dct:isVersionOf = kind` + `npx:signedBy`) against the governing space-ref's
    admin/maintainer roster and returns the latest authorized version.
- **Risk:** the federated `SERVICE` join is the known grlc timeout / global-scan failure
  mode (`docs/queries`, and the view-displays ref-scoping history). Acceptable for proving
  the model end-to-end; promote to path B if it doesn't hold up.

### B. Materializer precompute (promote if A is too slow)

- **nanopub-query:** new admit tier for maintained view-kinds — among nanopubs sharing a
  `dct:isVersionOf` kind whose kind IRI is `isMaintainedBy` a space, keep those signed by a
  space admin/maintainer, take `MAX(dct:created)`, emit a `hasAuthorizedVersion` edge.
  Modeled on the preset latest-wins pass.
- **Consumer query:** a simple, single-store query nanopub reading `hasAuthorizedVersion` —
  no federation, fails closed.

Both paths keep the selection logic **in published nanopubs**, not in nanodash Java. The
only way to keep that query non-federated is for the join to have happened server-side
(path B).

## nanodash changes (same for both paths)

- **`View.get` / latest-resolution:** for governed view-kinds, resolve the canonical version
  via the consumer query (committed `(kind, space-ref)` pair) instead of walking the
  supersedes chain. Resolve against the materialized edge; **fail closed** if the committed
  pairing was never validly established (fall back to pinning the exact referenced version,
  don't float to a different authority).
- **View identity:** key on the `dct:isVersionOf` kind IRI, not the per-version nanopub IRI.
  Audit nanopub-IRI pinning sites — notably `GrlcQuery.get` / `hasViewQuery` pinning.
- **Assignment:** pin the committed `(kind, space-ref)` explicitly in the view-display
  assignment (the assigned view IRI is the *source* of the pin, not a permanent dependency
  on that nanopub staying resolvable).
- **Publish UI + rendering:** declare/manage a view-kind as a space-maintained resource. A
  maintained view-kind **appears in the space's maintained-resources listing** like any other
  maintained resource (optionally flagged as a view); no separate resource type is required.
