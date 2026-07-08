# Templates: embedded identity & space governance

**Status:** design / not yet implemented

## Goal

Two stacked changes to how templates are identified and updated:

1. **Embedded identity** — let a template be identified by an embedded IRI inside its
   nanopub (like views and queries), instead of overloading the assertion graph URI as
   the template node and the nanopub URI as the external reference.
2. **Space governance** — extend the views model from
   [views-and-presets-as-maintained-resources.md](views-and-presets-as-maintained-resources.md)
   to templates: a template's **kind** becomes a space-maintained resource, and the
   space's members decide which version is canonical.

Part 1 is a feature on its own, but it is also the right preliminary for part 2: with
embedded identity, templates become structurally identical to views, and the whole
governance kit (kind minting, resolver query shape, resolution code) ports verbatim
instead of needing an np-URI-shaped variant that turns legacy later.

## Background: template identity today

Templates are the odd one out among definition nanopubs:

- **Views** embed the instance (`sub:my-view`, via `npx:embeds`) and introduce a stable
  kind (`npx:introduces <np>/my-view-kind`), linked by `dct:isVersionOf`.
- **Queries** are identified by an embedded IRI (`<np>/query-name`).
- **Templates** use the **assertion graph URI** as the template node
  (`Template.java` anchors `templateIri = templateNp.getAssertionUri()`) and the
  **nanopub URI** as the external reference (`Template.getId()` returns
  `nanopub.getUri()`; `nt:wasCreatedFromTemplate`, `gen:hasActionTemplate`, and
  `template=` URLs all point there).

Latest-version resolution for templates is `QueryApiAccess.getLatestVersionId()`
(supersedes-chain, same-pubkey-gated), mainly consumed by `PublishForm`. The same-key
gate is the pain point this design ultimately removes: a template published under a key
the current maintainers no longer control (e.g. the view-creation template
`RA8_hijw…`, signed by the Nanodash-profile key) is **update-locked** — it can only be
derived from, never superseded.

## Part 1: embedded template identity

### Nanopub shape (new-style templates)

```turtle
sub:assertion {
  sub:template a nt:AssertionTemplate ;
    dct:isVersionOf <np>/…-template-kind ;   # kind minted from day one, see below
    rdfs:label "…" ;
    nt:hasStatement sub:st01 ; …
}
sub:pubinfo {
  this: npx:embeds sub:template ;
    npx:introduces <np>/…-template-kind .
}
```

- The template node is a regular embedded IRI, typed `nt:AssertionTemplate` /
  `nt:ProvenanceTemplate` / `nt:PubinfoTemplate` — no longer the assertion graph.
- References from other nanopubs point at the embedded IRI, not the nanopub URI.
- **Kind from day one:** any template opting into embedded identity also mints its
  stable kind IRI and links it with `dct:isVersionOf` in the same supersede. Each
  template chain then migrates once, not twice — turning on governance later is just
  registering the kind and adding `gen:governedBy` in the next version.

### Code changes (nanodash)

- **`Template` parsing:** replace the assertion-URI anchor with "find the node typed
  as a template in the assertion; fall back to the assertion URI for legacy
  templates." Precedent in the same class: `processShaclTemplate` already discovers
  its node dynamically (via `sh:targetClass`). The legacy branch stays permanently.
- **`TemplateData.getTemplate(id)`:** accept both forms — strip an embedded id to its
  nanopub id with the same regex `View.get` uses, load the nanopub, pick the node.
- **`Template.getId()` / reference emission:** return and propagate the embedded IRI
  for new-style templates, so `nt:wasCreatedFromTemplate`, `gen:hasActionTemplate`,
  and `template=` URLs naturally carry it.
- **Latest-resolution:** `getLatestVersionId(npId)` + map to the latest nanopub's
  single embedded template IRI — the same ~20 lines as `View.resolveLatestVersion`.
- Parse `dct:isVersionOf` → template kind (used by part 2).

### Coordination

- **nanopub-java port ([nanopub-java#91](https://github.com/Nanopublication/nanopub-java/issues/91)):**
  `Template` is slated to move to nanopub-java. The dual-mode parser should be
  designed into the ported class rather than ported as legacy and changed twice — a
  timing argument for doing this now, alongside or ahead of the port. Downstream
  consumers of the library then inherit the new parser automatically.
- **Ecosystem check:** listing queries (`get-assertion-templates` etc.) and the
  `npx:hasNanopubType` inference that keys the type repos must be verified against
  the new shape; other template consumers (older Nanodash deployments, other tools
  parsing template internals) need updated parsers before the shape reaches
  widely-used templates.

## Part 2: template kinds as space-maintained resources

Direct port of the views model — same vocabulary, same trust anchors, same
"pin is the floor" semantics:

1. **Registration** (nanopubs only, zero new infrastructure): the kind is declared a
   maintained resource of a space (`<kind> a gen:MaintainedResource ;
   gen:isMaintainedBy <space>`, admin-gated; the materializer already validates it
   and emits the governing space-ref — verified working for view kinds).
2. **Version declaration:** a governed template version carries `dct:isVersionOf
   <kind>` + `gen:governedBy <space>` on the embedded template node. Governance stays
   strictly opt-in per version; versions without `gen:governedBy` keep supersedes/
   same-key resolution unchanged.
3. **Resolver query:** a sibling of `get-latest-governed-version` (`RABmOzHjD…`) —
   identical spaces-repo gate (kind maintained by space, member+ tier, validated
   pubkey), with the SERVICE arm going to the template type repo(s) instead of the
   `gen:ResourceView` one. Wrinkle: assertion/provenance/pubinfo templates are three
   types → either three queries, a UNION over type repos, or SERVICE against `full`
   (the constant kind IRI is selective; needs the usual timeout testing — constants
   inside SERVICE are what keep the view resolver fast).
4. **Code branch:** parse `gen:governedBy` in `Template`; a governed-aware
   `getLatestTemplateVersionId(id)` — load the pin, governed → query (fails closed to
   the pin), else existing `getLatestVersionId`; swap the `PublishForm` call sites.
   The "new version" banner and `template-version=latest` redirect then work for
   governed floats automatically. Zero materializer change — templates confer no
   server-side authority; publishing still happens under the user's own key with a
   preview (same trust boundary as views).

### The two fiddly parts

- **Listing dedup:** cross-key `npx:supersedes` is invalid, so a governed version by
  a different member never removes the old head from its own chain — template
  listings would show both until the listing queries get a governed arm (the same
  in-SPARQL pattern as the six view-displays queries: run-once governed-latest
  sub-select per `(kind, space)` pair + precedence binds). The template listing
  surface is broader, so roll out resolution first and add governed arms
  incrementally. **Alternative:** with a second definition type adopting
  `(kind, space)`, the type-agnostic canonical-version edge sketched in
  nanopub-query#138 (`npa:isCanonicalVersionOf`, materialized on state rebuild)
  becomes more attractive — one edge join would replace all per-query governed arms
  for views *and* templates. Reconsider before hand-writing governed arms into
  half a dozen template listing queries.
- **One-time re-pointing of pins:** a pre-governance pin has no `gen:governedBy` and
  resolves supersedes-based, never seeing the governed line (by design — governance
  is never inferred from the kind). Bringing an existing template under governance
  means publishing a first governed version and superseding the references that pin
  the old chain once (as the three view displays were repointed on 2026-07-03).

## Backwards compatibility

**Safe direction — everything published today keeps working, ever:**

- Legacy templates parse bit-for-bit identically (permanent fallback branch).
- Existing references (np-URI `nt:wasCreatedFromTemplate` in published nanopubs, old
  `template=` URLs, `gen:hasActionTemplate` pins) still resolve.
- Governance is opt-in per version; the resolver query deploys before anything uses
  it; failures degrade to current behavior ("pin is the floor").

**The real caveat — new-style templates break *old software*:** an old parser anchors
on the assertion URI, finds nothing, and fails to open a new-style template — including
a legacy template whose supersede chain crosses over to the new shape. Affected: not-
yet-updated Nanodash deployments, and any other tool parsing template internals.
Round-trip caution: a nanopub created from a new-style template records the embedded
IRI; old code trying to "update/derive" from it may work accidentally (the artifact
code is extractable) but must not be relied on.

Non-breaking behavior shifts: old clients don't *see* governed floats (stale, not
broken); listings show duplicates until dedup lands; registering a kind as a
maintained resource reroutes its IRI from `ResourcePartPage` to
`MaintainedResourcePage`.

**Compatibility is therefore sequencing, not design:** code ships first (ideally via
the nanopub-java port), the new shape starts with test templates, high-traffic
templates migrate only once updated parsers are deployed where it matters, and
existing templates never migrate implicitly — only by a deliberate supersede.

## Rollout order

1. Dual-mode parser in `Template`/`TemplateData` (coordinated with nanopub-java#91).
2. First new-style test template end-to-end (publish → open → fill → round-trip).
3. Governed resolver query published + governed branch in code (inert until used).
4. First governed template pair: register a kind, publish a governed version — the
   update-locked view-creation template (`RA8_hijw…`) is the natural candidate.
5. Listing dedup: governed arms in template listing queries, or revisit
   nanopub-query#138 for the shared canonical-version edge.
