# Fill modes (use / supersede / derive / override)

**Status:** ✅ Implemented — the `USE`, `SUPERSEDE`, `DERIVE` and `OVERRIDE`
fill modes are wired end-to-end in the publish form. The `OVERRIDE` mode and
the derive root-reset fix landed with
[#527](https://github.com/knowledgepixels/nanodash/issues/527).

When you open the publish form pre-filled from an existing nanopublication, a
**fill mode** decides how the source nanopub's content is copied into the new
one: which identifiers are re-minted vs. kept, what provenance link is recorded,
and — the subject of [#527](https://github.com/knowledgepixels/nanodash/issues/527)
— which nanopub becomes the new one's **root definition**.

## The four modes

| Mode | URL param | Provenance link | Introduced IRIs | Root definition |
| --- | --- | --- | --- | --- |
| **Use** | `use` / `use-a` | none | re-minted | new nanopub |
| **Supersede** | `supersede` / `supersede-a` | `npx:supersedes` | **kept** | **kept** (same resource, new version) |
| **Derive** | `derive` / `derive-a` | `prov:wasDerivedFrom` | re-minted (new resource) | **new nanopub** |
| **Override** | `override` / `override-a` | `prov:wasDerivedFrom` | **kept** | **kept** |

The `*-a` variant of each param fills **only the assertion** (`fillOnlyAssertion`),
skipping provenance and pubinfo.

The mode is represented by the `PublishForm.FillMode` enum and carried on the
**assertion** `TemplateContext` (`setFillMode`). Provenance and pubinfo contexts
are not mode-aware — the identity-bearing placeholders (introduced resources and
the root-nanopub placeholder) live in the assertion.

### Use

A fresh nanopub seeded from a source or example. Every identifier from the
source is rewritten into the new nanopub's namespace, and no provenance link
back to the source is recorded.

### Supersede

A **new version of the same resource**. `npx:supersedes` points at the source.
The source's introduced resource IRIs are **kept** (same identity), and the root
definition is **kept** (a version chain shares one root). Offered only for your
own nanopubs. Menu action: `UpdateAction` ("update").

### Derive

A **new, distinct resource** based on an existing one. `prov:wasDerivedFrom`
points at the source. Introduced IRIs are **re-minted** — deriving is allowed to
change them — and, per #527, the new nanopub becomes **its own root
definition** rather than inheriting the source's. Offered for any nanopub. Menu
action: `DeriveAction` ("edit as derived nanopublication").

### Override

Like derive in that it records `prov:wasDerivedFrom`, but — like supersede — it
**keeps** the source's introduced resource IRIs **and** its root definition.
The intent is "publish a variant that claims the same identity/root, but as a
distinct, independently-authored nanopub" (e.g. a competing definition of the
same resource by a different author). Menu action: `OverrideAction` ("edit as
overriding nanopublication").

> Note: an override nanopub is currently **indistinguishable from a derived one
> at the RDF level** — it reuses the same `prov:wasDerivedFrom` pubinfo template
> (`RARW4MsFkHuwjycNElvEVtuMjpf4yWDL10-0C5l2MqqRQ`). Only the retained IRIs and
> root differ. If the two ever need to be told apart in queries, override would
> need its own pubinfo predicate/template.

## The root-definition bug (#527)

A template can mark one slot as the **root-nanopub placeholder**
(`nt:ROOT_NANOPUB_PLACEHOLDER`, `Template.isRootNanopubPlaceholder`): this is how
a nanopub declares which nanopub *defines* the resource it is about (e.g. a
Space or maintained resource pointing at its own root definition).

When filling from a source, `ValueFiller.transform` rewrites the source's own
URI to the `local:nanopub` sentinel, so a source that is its own root arrives at
the form carrying that sentinel in the root slot.

Before the fix, `TemplateContext.processValue` resolved the root slot the same
way for `DERIVE` and `SUPERSEDE`:

- sentinel `local:nanopub` → the **source** nanopub's URI;
- an explicit external root IRI → **kept** as-is.

So a derived nanopub kept the source's root definition — wrong for a *new*
resource. The fix short-circuits `DERIVE` to resolve the root slot to the new
nanopub (`targetNamespace`) regardless of the migrated value:

```java
} else if (template.isRootNanopubPlaceholder(iri)) {
    ...
    if (fillMode == FillMode.DERIVE) {
        // new resource → new nanopub becomes its own root definition (#527)
        iri = vf.createIRI(targetNamespace);
    } else if (rootValue.equals(LocalUri.of("nanopub").stringValue())) {
        Nanopub ref = getReferenceNanopub();
        if (ref != null && (fillMode == FillMode.SUPERSEDE || fillMode == FillMode.OVERRIDE)) {
            iri = vf.createIRI(ref.getUri().stringValue());   // keep source as root
        } else {
            iri = vf.createIRI(targetNamespace);
        }
    } else if (rootValue.isEmpty()) {
        iri = vf.createIRI(targetNamespace);
    } else {
        iri = vf.createIRI(rootValue);                        // keep explicit external root
    }
}
```

The root slot is rendered read-only (`ValueItem` routes root placeholders to
`ReadonlyItem`). In derive mode `ReadonlyItem.rootResolvesToThisNanopub()` makes
the form honestly display "this nanopublication" for that slot, even when an
external root value was migrated from the source.

## Where the modes are wired

- **`component/PublishForm.java`** — `FillMode` enum; maps the URL params to a
  mode (§ lines ~137–176); attaches the `npx:supersedes` / `prov:wasDerivedFrom`
  pubinfo template; skips default-license seeding for all fill-from modes.
- **`template/ValueFiller.java`** — copies the source graph and rewrites URIs;
  `transform(Value)` keeps introduced IRIs for `SUPERSEDE`/`OVERRIDE`, re-mints
  them otherwise.
- **`template/TemplateContext.java`** — `processValue` resolves the root-nanopub
  placeholder (above) and the introduced-resource placeholders per mode.
- **`component/ValueItem.java`** — renders introduced resources read-only in
  `SUPERSEDE`/`OVERRIDE` mode (their IRIs are fixed).
- **`component/ReadonlyItem.java`** — root-slot display, incl.
  `rootResolvesToThisNanopub()`.
- **`action/{DeriveAction,UpdateAction,OverrideAction}.java`** — the nanopub
  action-menu entries that build these URLs; registered in
  `action/NanopubAction.java`.
