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
| **Partial** | `partial` / `partial-a` | none | re-minted | new nanopub |
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

### Partial

Identical to **Use**, except that source content which does not match the
template being filled is **discarded silently** instead of being listed under
*"Some content from the existing nanopublication could not be filled in:"*.

Use it when filling deliberately **across** templates, where the source is only
a seed for the form and a mismatch is expected rather than a warning. The
motivating case is a view action that opens one template's form seeded from a
nanopub made with another: e.g. the Nanosuggestions "assign" action, whose
source may be the nanopub that *reported* the issue — its title, description,
status and space links have no counterpart in the assign template, so every
such form would otherwise carry a warning listing them.

Every other mode keeps reporting unmatched content, which is what you want when
source and target template are meant to line up: there, leftovers mean content
would be silently lost. The distinction is deliberately **explicit in the
parameter** rather than inferred (e.g. from the source's
`nt:wasCreatedFromTemplate` differing from the target): filling across templates
is not by itself evidence that the author wants the difference hidden.

Discarding also covers the pubinfo catch-all — unmatched pubinfo is dropped
rather than swept into the hand-coded statements template, which would publish
it after all.

### Supersede

A **new version of the same resource**. `npx:supersedes` points at the source.
The source's introduced resource IRIs are **kept** (same identity), and the root
definition is **kept** (a version chain shares one root). Offered only for your
own nanopubs. Menu action: `UpdateAction` ("update").

Keeping the introduced IRI applies **however the template declares the introduced
resource** — as a sub-IRI of the template nanopub (`sub:class`), or as an absolute
IRI in a foreign namespace carrying the `~~ARTIFACTCODE~~` marker (the idiom used
by "Defining a biochementity"). The pin lives in `TemplateContext#processValue`
and is keyed on `isIntroducedResource` alone; it must **not** be gated on
`isLocalResource`, which `Template#tagIfUntypedLocal` only ever attaches to
sub-IRIs of the template nanopub. Gating it there made supersede/override silently
mint a *new* resource for marker-style templates — regression test:
`OverrideIntroducedResourceIriTest`.

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
overriding nanopublication"), offered only for a nanopub that introduces or describes
a resource other than one it embeds
([#920](https://github.com/knowledgepixels/nanodash/issues/920)): for any other,
there is nothing for an override to keep, and it would make the same nanopub as
deriving.

> Note: an override nanopub is currently **indistinguishable from a derived one
> at the RDF level** — it reuses the same `prov:wasDerivedFrom` pubinfo template
> (`RARW4MsFkHuwjycNElvEVtuMjpf4yWDL10-0C5l2MqqRQ`). Only the retained IRIs and
> root differ. If the two ever need to be told apart in queries, override would
> need its own pubinfo predicate/template.

## Parameters override the source (#73)

A link can combine a fill with `param_<name>` (and `prparam_`, `piparam<n>_`)
arguments: *"open this nanopub for superseding, but with this field saying
that"*. The parameters are applied while the form's fields are built, before the
source's statements are unified with them, so a parameter value that differs
from the source's used to make the field **reject** the source's value, and the
statement it belongs to fail to unify.

`TemplateContext.fill` isolates each statement, so the failure no longer aborts
the whole fill, but the rollback still cost more than the overridden value: the
other fields of that statement's **repetition group** were emptied, and the
statement was reported under *"Some content from the existing nanopublication
could not be filled in:"* although it had been replaced on purpose.

The form is now filled from the source differently (#936): the source is read with
nanopub-java's `SourceReader` for the form's templates, and each value becomes the value its
field starts with (`TemplateContext.setSourceValue`), under the same name a parameter has.
This happens before the fields are built, and a field takes a parameter's value over the
source's (`TemplateContext.getValue`). The statement counts as read, so there is no leftover
warning, and its siblings and further repetitions keep the source's values. A field under a
space-dependent prefix ignores a parameter, but takes the source's value, under the base the
form has (see `template/SourceFill.java`).

Regression test: `ParamOverridesFillTest`.

## The root-definition bug (#527)

A template can mark one slot as the **root-nanopub placeholder**
(`nt:ROOT_NANOPUB_PLACEHOLDER`, `Template.isRootNanopubPlaceholder`): this is how
a nanopub declares which nanopub *defines* the resource it is about (e.g. a
Space or maintained resource pointing at its own root definition).

When filling from a source, a source that is its own root arrives at the form with
its own URI in the root slot (`SourceFill`; until #936, `ValueFiller` wrote the
`local:nanopub` sentinel, which `ReadonlyItem` turned into that URI). Without a source,
the slot holds the sentinel.

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
- **`component/ViewActionMappings.java`** — a view action can open a fill mode from
  the page it is shown on: mapping the `@sourceNp` page source to `@override` makes the
  action reopen the nanopub the view is showing. See docs/magic-query-params.md.
- **`template/SourceFill.java`** — reads the source for the form's templates with
  nanopub-java's `SourceReader` and gives the fields their values; keeps introduced
  IRIs for `SUPERSEDE`/`OVERRIDE`, re-mints them otherwise. (`template/ValueFiller.java`
  still fills the read-only view of a nanopublication.)
- **`template/TemplateContext.java`** — `processValue` resolves the root-nanopub
  placeholder (above) and the introduced-resource placeholders per mode.
- **`component/ValueItem.java`** — renders introduced resources read-only in
  `SUPERSEDE`/`OVERRIDE` mode (their IRIs are fixed).
- **`component/ReadonlyItem.java`** — root-slot display, incl.
  `rootResolvesToThisNanopub()`.
- **`action/{DeriveAction,UpdateAction,OverrideAction}.java`** — the nanopub
  action-menu entries that build these URLs; registered in
  `action/NanopubAction.java`.
- **`template/TemplateContext.java`** — `getValue` gives a field a parameter's value over
  the source's (#73).
