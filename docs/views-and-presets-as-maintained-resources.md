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

A kind can be maintained by several spaces **side by side**, so the durable unit is the
`(kind, space)` pair — not the kind alone. Resolving "latest" against the union of every
maintaining space's members would be wrong: a member of D2 could override a definition meant
to be governed by D1. Each version must therefore name the space that governs it.

**The pin lives in the view definition, not the assignment.** A view version nanopub
self-describes its pair: `dct:isVersionOf → kind` and `gen:governedBy → space`. The publisher —
a member of that space — declares it once at publish time, and every assignment that pins the
version inherits the pair for free. An assignment (view display) still just references a
concrete version IRI and carries no space triple. Latest-resolution then floats **within
`(kind, space)`**: the newest version of the kind that names the same space and is signed by a
member+ of it. (This replaces the earlier idea of pinning the space on the assignment, which
would let two assignments of the same version disagree about its governance.)

**`gen:governedBy` is a label, not a grant.** A self-declared space must never be
self-authorizing — otherwise anyone forks a version, stamps it "governed by ⟨a space I
control⟩," and wins. Two independent, already-validated anchors keep it honest:
1. `<kind> gen:isMaintainedBy <space>` — admin-gated and materialized — confirms the space is
   entitled to govern the kind.
2. the winning version is signed by a member+ of that space.
The `gen:governedBy` triple only selects *which* pair; authority comes entirely from (1)+(2).
A version naming a space that doesn't maintain the kind, or signed by a non-member, is skipped.

**`gen:governedBy` points at the space ID, not a space-ref (root-def-pinned).** The link
records the durable space IRI and defers root-def resolution to *resolution time*. A space may
legitimately need to change its root definition (e.g. after proving control of its URL
domain); if every view definition had frozen the old root-def ref, all those links would break
on migration. Instead, resolution maps the space IRI → its currently-authoritative root def,
then gates on that root's membership. The cost: that mapping must be trustworthy — today it
leans on the materialized representative root; the intent is to ground it more solidly later
(domain-control proof) so a conflicting claimant can't hijack the IRI. This is the one
deliberately-deferred piece.

**Degrades safely.** A version with no `gen:governedBy` falls back: a single-maintainer kind
still resolves (the space comes from the kind); a multi-maintainer kind with no declaration
does **not** float — the pinned version stands. So R's admin, by assigning, delegates update
authority to the named space's members without ever risking a cross-space override.

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
