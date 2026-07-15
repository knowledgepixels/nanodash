# Language-tag picker for literal placeholders

**Status:** design — not implemented.

## Goal

Let the user filling a template choose the language tag of a literal at fill time,
instead of the tag being fixed per placeholder by the template author. Motivating
shape: a label/description template usable for any language —

```
sub:st1  <subj>  rdfs:label    [label]    (user enters "Haus" and picks @de)
sub:st2  <subj>  rdfs:comment  [comment]  (user enters text and picks a language)
```

Today this needs one template (or one placeholder) per language, or falls back to the
`nt:ValuePlaceholder` escape hatch where the user types raw `"Haus"@de` syntax.

Deliberately **out of scope** for the first version:

- A single language selector shared across several fields ("fill label *and* comment
  in language X") — see *Considered and rejected / deferred* below.
- Optional tagging (user may leave the literal untagged): a picker-enabled placeholder
  always produces a language-tagged literal (`rdf:langString`). Plain-or-tagged
  ambivalence complicates validation and unification for little gain.

## Background: the current model

- **Fixed tag per placeholder:** a literal placeholder can carry `nt:hasLanguageTag`
  (0..1); the tag is parsed into `languageTagMap` (`Template.java:912-913`, normalized
  via RDF4J `Literals.normalizeLanguageTag`). On submit the tag is appended to the
  entered text (`TemplateContext.java:411-412`). The form shows it as a static
  "(en)"-style label next to the input (`LiteralTextfieldItem.java:97-99`, the
  `.litlang` span in `LiteralTextfieldItem.html`). The user cannot pick or change it.
- **Unification enforces the fixed tag:** filling from an existing nanopub requires
  the value's tag to equal the template's tag exactly
  (`LiteralTextfieldItem.java:152-157`, `ReadonlyItem.java:412-416`); a value with a
  different tag fails to unify and the display falls back to the generic
  (template-less) rendering.
- **Read side is already tag-agnostic:** viewing/superseding a language-tagged literal
  displays its tag even when the template declared none
  (`LiteralTextfieldItem.java:177-179`, `ReadonlyItem.java:465-468`).
- **Escape hatch:** `nt:ValuePlaceholder` accepts a full literal serialization
  `"text"@tag` with any tag matching `[0-9a-zA-Z-]{2,}` (`Utils.java:921,966-968`).
- **Dormant vocabulary:** `nt:hasLanguageAttribute` exists in the vocabulary
  (`org.nanopub.vocabulary.NTEMPLATE`, nanopub-java) with the same javadoc as
  `nt:hasLanguageTag` and **zero consumers** in nanodash or nanopub-java. This design
  leaves it untouched (see rejected option 2).

## Vocabulary

Three additions/refinements, all on the literal placeholder itself:

1. **`nt:LanguageTaggedLiteralPlaceholder`** (new class) — declares that the
   placeholder's value is an `rdf:langString` whose tag the user selects at fill
   time. Used *in addition to* `nt:LiteralPlaceholder` or
   `nt:LongLiteralPlaceholder` (which decide textfield vs. textarea):

   ```
   sub:comment a nt:LiteralPlaceholder, nt:LanguageTaggedLiteralPlaceholder ;
       rdfs:label "comment" .
   ```

2. **`nt:possibleLanguageTag`** (new predicate, 0..n literals) — restricts the
   picker's choices to the given tags. Absent → the full built-in language list is
   offered. A deliberate parallel to `nt:possibleValue`, but a separate predicate:
   `nt:possibleValue` describes possible *values of the placeholder*, and overloading
   it for tags would be ambiguous the day literal placeholders get value suggestions.

   ```
   sub:comment nt:possibleLanguageTag "en", "de", "fr" .
   ```

3. **`nt:hasLanguageTag`** (existing, 0..1) — semantics depend on the placeholder
   type:
   - on a plain literal placeholder (no new type): fixed tag, exactly as today —
     **unchanged**;
   - on a `nt:LanguageTaggedLiteralPlaceholder`: the *default* pre-selection of the
     picker. This double duty is the backward-compatibility hinge: an old client that
     doesn't know the new type sees an ordinary fixed-tag field with a sensible tag
     (see *Backwards compatibility*).

Mutual exclusion with `nt:hasDatatype` already holds structurally (an RDF literal has
either a tag or a datatype); parsing should warn if both are declared on one
placeholder and ignore the datatype, mirroring the existing precedence in
`TemplateContext.processValue` (`TemplateContext.java:394-416`, language branch only
reachable when datatype is null — for the new type make that precedence explicit).

**Vocabulary home:** the constants belong in `NTEMPLATE` (nanopub-java), next to
`HAS_LANGUAGE_TAG`. Since `Template` is slated to move to nanopub-java anyway
([nanopub-java#91](https://github.com/Nanopublication/nanopub-java/issues/91)),
contribute the two terms upstream first; until a release is available nanodash can
mint them locally with `vf.createIRI(NTEMPLATE.NAMESPACE, "…")`.

### Considered and rejected / deferred

1. **Multi-valued `nt:hasLanguageTag` = choice set.** Attractive (no new terms;
   single value degenerates to today's fixed tag) but unsafe with deployed parsers:
   old code does `languageTagMap.put(subj, …)` per triple (`Template.java:912-913`),
   so the *last-parsed* tag silently wins and old clients would publish literals fixed
   to an arbitrary member of the set — valid RDF with the **wrong language**. Also no
   way to express "any language", and RDF's unordered multi-values give no natural
   default.
2. **Companion language placeholder** linked from the literal placeholder via the
   dormant `nt:hasLanguageAttribute` (the tag is itself a restricted-choice-style
   placeholder). This is the natural encoding for a *shared* selector (one dropdown
   driving label + comment) and would reuse the `possibleValue` machinery. Rejected
   for v1: the language placeholder never occurs in any statement position, and the
   whole scope/repetition machinery is keyed on statement occurrence
   (`TemplateContext.initStatements`/`narrowScopeMap`, `StatementItem.getIriSet`,
   `RepetitionGroup.transform`'s `__N` suffixing) — a statement-less placeholder needs
   new plumbing for scoping, rendering position, unification (what if an existing
   nanopub has `label@de` but `comment@en`?), and read-only display. Deferred; the
   per-field design below doesn't preclude adding it later with exactly this
   vocabulary.
3. **Status quo escape hatch** (`nt:ValuePlaceholder` with typed `"…"@tag`): works
   but is raw-syntax UX and gives up literal-specific affordances (regex, datatype
   guard, quote rendering).

## UI

Replace the static "(en)" label slot in `LiteralTextfieldItem.html` (the `.litlang`
span) with, for picker-enabled placeholders, a compact searchable dropdown — same
select2 stack as `RestrictedChoiceItem` (`org.wicketstuff.select2.Select2Choice`):

- **Choices:** the `nt:possibleLanguageTag` set if declared; otherwise a built-in
  list generated from `java.util.Locale.getISOLanguages()` (ISO 639-1, ~180 entries).
- **Display:** `tag — display name`, e.g. `de — German`, via
  `Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)`; the stored value is
  always the bare tag.
- **Free entry:** when no `possibleLanguageTag` restriction is declared, allow typing
  a tag not in the list (select2 tag mode) so region/script subtags like `en-GB` or
  `zh-Hant` work. Validate with the BCP47-ish pattern
  `[a-zA-Z]{2,8}(-[0-9a-zA-Z]{1,8})*` and normalize with
  `Literals.normalizeLanguageTag` before use.
- **Default:** pre-select `nt:hasLanguageTag` if declared; otherwise empty
  ("language…" prompt). (Pre-selecting from the browser locale
  (`Session.get().getLocale()`) is a possible refinement — leave out of v1 so the
  choice is always conscious.)
- **Read-only display** (`ReadonlyItem`) keeps the current "(de)" text form — no
  dropdown needed there.

Since `LiteralTextareaItem extends LiteralTextfieldItem`
(`LiteralTextareaItem.java:12`) and only overrides `initTextComponent`, one
implementation in `LiteralTextfieldItem` covers both textfield and textarea, provided
`LiteralTextareaItem.html` gets the same markup slot.

## State model: where the chosen tag lives

The literal text lives in an `IModel<String>` registered in
`TemplateContext.componentModels` keyed by the placeholder IRI — already
repetition-suffixed (`__N`) by `RepetitionGroup.transform` before the component is
constructed (`StatementItem.java:611-626`), and prefillable from a URL parameter named
by the IRI postfix (`LiteralTextfieldItem.java:51-61`).

The tag gets a **second model under a derived key**: `<placeholder-iri>__lang`,
derived *after* repetition suffixing (so repetition 2 of `…#comment` stores its tag
under `…#comment__2__lang`). Properties this inherits for free:

- **Per-repetition tags:** each repetition group has its own derived key, so
  repetition 1 can be `@de` and repetition 2 `@fr`.
- **URL-parameter prefill:** the same postfix-named parameter convention gives
  `?comment=Haus&comment__lang=de` with no extra code beyond the usual
  `hasParam`/`getParam` lookup in the component constructor.
- **No collisions with the repetition machinery:** `finalizeStatements` probes only
  numeric suffixes (`postfix + "__" + i`, `TemplateContext.java:120-128`), and
  `Template.transform` strips only `__[0-9]+$` (`Template.java:1182-1196`), so a
  trailing `__lang` passes through both untouched. Add a
  `TemplateContext.getLanguageModelKey(IRI)` helper so the convention lives in one
  place, and have `StatementItem.getIriSet`/`removeFromContext` treat the derived
  model as owned by the literal component (created and cleaned up by it), not as a
  placeholder of its own.

## Semantics

For a placeholder `p` typed `nt:LanguageTaggedLiteralPlaceholder`:

1. **Publishing:** on submit, the emitted object is
   `vf.createLiteral(text, chosenTag)`. Text filled but no tag selected is a
   validation error ("select a language"); tag selected but text empty counts as
   empty (the tag alone triggers nothing — mirrors how an untouched optional field
   behaves). Both empty is fine iff the field is optional.
2. **Validation:** the dropdown's required-flag mirrors the text component's
   (`optional` parameter, `LiteralTextfieldItem.java:63`); the text-without-tag case
   additionally needs a form-level (cross-component) validator because Wicket's
   per-component `required` can't see the sibling.
3. **Restriction:** if `nt:possibleLanguageTag` is declared, the chosen tag must be
   in the set (enforced by the dropdown; re-checked server-side).
4. **Unification (view / derive / supersede):** a value `v` unifies with `p` iff `v`
   is a literal with *some* language tag, the tag is in the `possibleLanguageTag` set
   (when declared), the regex (if any) matches, and the text agrees with any
   already-entered text — i.e. the fixed-tag equality check at
   `LiteralTextfieldItem.java:154-157` and `ReadonlyItem.java:412-416` is **relaxed to
   set membership / any-tag** for the new type. `unifyWith` sets both models (text and
   tag). Tag comparison is on normalized tags (`Literals.normalizeLanguageTag`), as
   the existing checks already do.
5. **Default-tag subtlety:** `nt:hasLanguageTag` on a picker placeholder is a
   default, **not** a constraint — unification must not require it. All three
   enforcement sites therefore branch on the type, best captured in one
   `Template`-level helper (e.g. `isLanguageTagSelectable(iri)`) next to
   `getLanguageTag` (`Template.java:238-241`).

## Code changes

1. **Vocabulary constants** — `LANGUAGE_TAGGED_LITERAL_PLACEHOLDER`,
   `POSSIBLE_LANGUAGE_TAG` in `NTEMPLATE` (upstream) or a local holder until the next
   nanopub-java release.
2. **`Template`** — parse the new predicate into a `possibleLanguageTagMap`
   (normalize each tag) alongside the `HAS_LANGUAGE_TAG` branch
   (`Template.java:910-919`, and the SHACL twin at `Template.java:1106-1108`); add
   `isLanguageTagSelectable(IRI)` (type check via the existing
   `typeMap`/`transform` pattern) and `getPossibleLanguageTags(IRI)`. Warn on
   `hasDatatype` + picker type on the same placeholder.
3. **`LiteralTextfieldItem`** (+ its HTML, + `LiteralTextareaItem.html`) — when
   `isLanguageTagSelectable`: create/look up the `__lang` model, prefill from the
   `<postfix>__lang` param, render the select2 dropdown in place of the static label,
   register it in `context.getComponents()` (needed for AJAX refresh and
   `removeFromContext`), wire required/cross-field validation, and relax
   `isUnifiableWith`/`unifyWith` per semantics 4–5. The existing fixed-tag and
   plain-literal paths stay byte-identical.
4. **`TemplateContext.processValue`** — in the literal-placeholder branch
   (`TemplateContext.java:391-417`): if selectable, read the `__lang` model and
   create the tagged literal; keep the existing fixed-tag branch for plain
   placeholders.
5. **`ReadonlyItem`** — relax the tag-equality check (`ReadonlyItem.java:412-416`)
   to the same set-membership/any-tag rule; display needs no change
   (`ReadonlyItem.java:465-468` already shows the value's tag).
6. **Later, out of band** — surface the new type and `possibleLanguageTag` in the
   template-creation template so authors can declare pickers without hand-editing
   TriG; revisit `nt:hasLanguageAttribute` if the shared-selector extension (rejected
   option 2) is picked up.

## Touch points checked and *not* affected

- **`Utils` literal serialization** (`Utils.java:920-974`) — already round-trips
  `"…"@tag`; `ValueFiller` feeds statements to unification as parsed `Literal`s, so no
  serialization change is needed.
- **`LiteralDateItem` / `LiteralDateTimeItem`** — selected on datatype
  (`ValueItem.java:72-75`), disjoint from language-tagged literals by RDF semantics.
- **`GuidedChoiceItem` / `RestrictedChoiceItem`** — choice placeholders produce IRIs
  or plain literals; out of scope (a language-tagged restricted choice would be a
  separate feature).
- **Repetition machinery** — the derived `__lang` key is invisible to it (see *State
  model*); `StatementItem`'s backtracking fill is untouched because matching stays
  per-literal-component.

## Backwards compatibility

- **Old templates, new code:** no deployed template carries the new type, so every
  existing template parses and renders identically; the fixed-tag path is untouched.
- **New templates, old code:** an old nanodash ignores the unknown type and
  `possibleLanguageTag` triples (extra `rdf:type`s are harmless to the
  `typeMap`-based checks; unknown predicates fall through the parse loop). Two
  degradation modes, both non-broken:
  - with a default `nt:hasLanguageTag` declared → old clients show today's fixed-tag
    field and publish with the default tag — well-formed, just not selectable. This
    is why template authors should **declare a default** on shared templates;
  - without a default → old clients show a plain untagged field and publish an
    untagged literal — well-formed but unfortunate for multilingual use.
  Viewing a picker-published nanopub in old code: unification succeeds when the tag
  happens to equal the old client's view of the template (default tag present and
  matching), otherwise falls back to the generic statement display — same degradation
  class as [optional statements in groups](optional-statements-in-groups.md). So:
  don't add the picker to high-traffic shared templates until updated parsers are
  deployed where those templates are opened.
- **nanopub-java port:** vocabulary and the unification relaxation belong to the
  `Template`/engine code being re-hosted
  ([nanopub-java#91](https://github.com/Nanopublication/nanopub-java/issues/91)) —
  design the tag-set membership rule into the ported class rather than porting
  fixed-tag-only and changing it twice.

## Implementation plan

Each step compiles and is testable on its own:

1. **Vocabulary + template layer:** constants, parsing, `isLanguageTagSelectable`,
   `getPossibleLanguageTags`, datatype-conflict warning. Unit tests on a fixture
   template (picker with/without restriction and default).
2. **Form UI + publish path:** dropdown component with model/param wiring,
   validation, `processValue` change. Manual check in the dev jetty: enter text, pick
   tag, publish; verify `@tag` in the published TriG; repetition with two languages;
   `?comment__lang=de` prefill.
3. **Fill/unification:** relaxation in `LiteralTextfieldItem` and `ReadonlyItem`.
   Tests: view a nanopub with `@fr` under an unrestricted picker (unifies, tag
   shown); under a restricted set excluding `fr` (falls back to generic display);
   supersede round-trip keeps the tag editable.
4. **End-to-end:** publish a test template (label + comment pickers, `en` default),
   fill → publish → reopen → supersede with changed language on the dev instance.
5. **Later:** template-creation template support; shared-selector extension via
   `nt:hasLanguageAttribute` if demand materializes.
