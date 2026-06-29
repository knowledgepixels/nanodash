# Views & presets as space-maintained resources

**Status:** design / not yet implemented

## Goal

Let a space **govern a definition** — decide which version of a view or preset is
authoritative — instead of leaving definitions as free-floating nanopubs anyone can fork.

## Core idea

1. A definition's **kind** IRI (its `dct:isVersionOf` target) is declared a maintained
   resource of a space: `<kind> a gen:MaintainedResource ; gen:isMaintainedBy <space>`. The
   existing materializer already validates this (admin-gated) and emits a governing
   space-ref. It also appears in the space's maintained-resources listing.
2. The **canonical version** is the newest one signed by a representative (admin/maintainer)
   of the **defining** space — authority-scoped latest-wins. No `npx:supersedes` chain needed.

Two distinct spaces are involved: the **defining** space (D) governs which version is
authoritative; the **applying** resource (R) where a definition is assigned is gated
separately by R's own admin. Roles a preset grants land on R, not D — so the signer tier on
D's side is about *who speaks for D*, not privilege amplification.

## Per-pair governance & commit-at-assignment

A kind can be maintained by several spaces **side by side**; the durable unit is the
`(kind, space-ref)` pair. An assignment references a **concrete version IRI** (not the kind),
which self-describes both its kind and its defining space — pinning one unambiguous pair.
Updates then float to the latest authorized version **within that committed pair**. So R's
admin, by assigning, **delegates update authority to D's representatives** — trusting D's
authoritative-version process, not its whole membership.

## Views vs presets — the one real difference

- **Views are display-only.** "Latest authorized version" can be resolved at render time
  (client-side / a query). **Zero materializer change.**
- **Presets grant roles = server-conferred authority.** The materializer expands a preset
  into real role grants that drive tiers and admin closures, so the authorized-version
  decision must be enforced **where the grant is produced**. Auto-updating presets therefore
  needs a small materializer change; without auto-update, presets can stay pinned and adopt
  new versions via a normal (gated) re-assignment — also zero materializer change.

## Minimal materializer change (presets, auto-update only)

The float machinery mostly exists already. The additions: (1) gate the winning declaration on
the **maintaining space's** admins (today it's ungoverned — safe only because presets don't
float yet); (2) revoke role grants dropped by a new authoritative version, not just add new
ones; (3) stamp the publisher on the declaration so (1) can check it.

## Big picture

One model — *definition kind = space-maintained resource, canonical = latest authorized
version, assignment commits to a `(kind, space)` pair*. Views ride it entirely client-side;
presets ride it too, plus one small server-side gate because they confer authority.
