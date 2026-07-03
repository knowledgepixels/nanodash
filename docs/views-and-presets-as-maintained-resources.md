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
   of the **defining** space — authority-scoped latest-wins. No `npx:supersedes` chain needed
   (versions that don't opt into space governance keep today's supersedes/same-pubkey
   resolution; see below).

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

**Membership is checked at resolution time.** The winning version must be signed by someone
who is *currently* a member+ of the governing space, not someone who was at publish time. A
publisher leaving the space de-canonicalizes their versions from then on; resolution falls
back to the newest remaining valid version, or ultimately to the pin. This avoids needing
historical membership snapshots, at the cost of resolution results shifting when membership
changes.

**The pin is the floor.** If no valid floating candidate exists, the assignment's pinned
version stands — without re-validating the pin itself. Its legitimacy comes from the gated
assignment (R's admin chose it), not from the space-based checks; those only decide whether
anything *newer* may replace it.

**`gen:governedBy` points at the space ID, not a space-ref (root-def-pinned).** The link
records the durable space IRI and defers root-def resolution to *resolution time*. A space may
legitimately need to change its root definition (e.g. after proving control of its URL
domain); if every view definition had frozen the old root-def ref, all those links would break
on migration. Instead, resolution maps the space IRI → its currently-authoritative root def,
then gates on that root's membership. The cost: that mapping must be trustworthy — today it
leans on the materialized representative root; the intent is to ground it more solidly later
(domain-control proof) so a conflicting claimant can't hijack the IRI. This is the one
deliberately-deferred piece.

**Two resolution modes, chosen by the version.** If the pinned version declares
`gen:governedBy`, resolution is space-based as above. If it doesn't, the existing mechanism
applies unchanged: follow the `npx:supersedes` chain restricted to the same pubkey. Space
governance is thus strictly opt-in per version — it is never inferred from the kind's
maintainer set. (Inferring it would be fragile: a maintenance claim is gated only by the
*claiming* space's admin, so a later, unrelated claim on the kind could silently change how
already-published versions resolve.) So R's admin, by pinning a declared version, delegates
update authority to the named space's members without ever risking a cross-space override;
by pinning an undeclared version they delegate to the original publisher's key, as today.

**"Latest" is a claim by the space, not a verifiable ordering.** Without a supersedes chain,
any member+ of the governing space can republish older content with a fresh timestamp and win
resolution. That is inside the stated trust boundary — members speak for the space — and the
registry's rejection of future-dated nanopubs limits timestamp games, but "canonical" should
be read as *what the space currently endorses*, not as a tamper-proof version order.

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

## Minimal implementation (views)

Views only; presets and the materializer are untouched (pinned presets keep working via the
existing supersedes path).

1. **Vocabulary** (1 line): add `GOVERNED_BY` (`gen:governedBy`) to `KPXL_TERMS.java`, next
   to the existing `MAINTAINED_RESOURCE`.
2. **Parse it in `View`** (~5 lines): in the constructor's statement loop (where
   `dct:isVersionOf` → `viewKind` is parsed), also capture `gen:governedBy` → a
   `governingSpace` IRI field + getter. `ViewDisplay` stays untouched — assignments carry no
   space triple.
3. **One new published query — `get-latest-governed-version(kind, space)`.** The whole
   resolver lives here; returns at most one row:
   1. resolve the space IRI → its representative root ref via the spaces graph
      (`npa:viaNanopub` → `npa:forSpaceRef` → `npa:rootNanopub`);
   2. check `<kind> gen:isMaintainedBy <space>` against the materialized
      maintained-resources state (anchor 1);
   3. among versions with `dct:isVersionOf <kind>` + `gen:governedBy <space>`, keep those
      whose signer pubkey is a validated member+ (admin/maintainer/member — not observer) of
      that ref (anchor 2);
   4. `ORDER BY DESC(?created) LIMIT 1`.

   Host it on the spaces repo with a SERVICE clause out to the main repo for the
   version/pubkey lookup — ref-resolution and role filtering stay local, and both parameters
   are bound, so the federated scan is narrow. Plus a constant in `QueryApiAccess`.
   (Fallback if the query planner chokes: split into two cheap queries and join in Java.)
4. **Resolution branch in `View.resolveLatestVersion`** (~15 lines); the surrounding
   stale-while-revalidate memo machinery is reused unchanged:
   - load the pinned version first (load-then-resolve instead of today's resolve-then-load,
     to read `governedBy` off the pin);
   - no `governedBy` → existing supersedes-chain path, verbatim;
   - has `governedBy` → call the query; row → use that version; empty/error/timeout → pin
     stands, un-revalidated ("pin is the floor", fails closed).
5. **Publishing side — nanopub changes, no code**: supersede the view-creation/supersede
   template to add an optional `sub:view gen:governedBy <space>` statement; optionally
   pre-fill the space when "add view" is launched from a space context. Declaring the kind
   as maintained needs nothing new — the existing admin-gated `gen:isMaintainedBy`
   assignment and materializer validation already cover it.

Out of scope for the minimal version: preset auto-update (the materializer additions above),
grounding the space-IRI→root-ref mapping beyond the representative root (the
deliberately-deferred piece; the query reuses today's representative root), and UI surfacing
of "this view floats under space X".

Deployable in any order except: the query must be live before any view version carries
`gen:governedBy` — and since no existing version does, current behavior is unchanged from
day one.

## Big picture

One model — *definition kind = space-maintained resource, canonical = latest authorized
version, assignment commits to a `(kind, space)` pair*. Views ride it entirely client-side;
presets ride it too, plus one small server-side gate because they confer authority.
