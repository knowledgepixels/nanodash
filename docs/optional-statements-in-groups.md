# Optional statements inside statement groups

**Status:** design draft — not implemented.

## Goal

Allow individual member statements of a `nt:GroupedStatement` to be marked
`nt:OptionalStatement`: the member's triple is emitted only if its placeholders are
filled, while the group's other members keep their own requirements. Deliberately
**out of scope**: repeatable statements inside groups and nested groups — the group is
the unit of repetition (the `__N` repeat-suffix machinery and the fill algorithm both
assume that), and lifting either restriction would break that invariant.

Motivating shape: a group like

```
sub:st1  <subj>  a  <SomeClass>          (required)
sub:st2  <subj>  rdfs:label  [label]     (required)
sub:st3  <subj>  rdfs:comment [comment]  (optional)
```

where the comment line should be skippable without making the whole group optional or
splitting the group apart (which loses the visual unit and, for repeatable groups, the
per-repetition coupling).

## Background: the current model

- **Vocabulary:** a group is a statement node typed `nt:GroupedStatement` whose members
  are linked via `nt:hasStatement` (`Template.java:832`, members removed from the
  top-level list at `Template.java:901-907`). Members are ordinary statement IRIs, so a
  member can already carry `a nt:OptionalStatement` — **no vocabulary change is
  needed**; only the consuming code ignores the flag today.
- **Precedent in code:** `Template.isRequiredField()` (`Template.java:596-611`) already
  checks `isOptionalStatement(member)` on group members — one corner of the codebase
  anticipates exactly this semantics.
- **Everything else is group-level.** `StatementItem` treats a group as all-or-nothing:
  - *Form validation:* every input component gets its required-flag from
    `RepetitionGroup.isOptional()` (group-level; `ValueItem.java:53-80`).
  - *Publishing:* `StatementItem.addTriplesTo` skips or fails the whole group based on
    `hasEmptyElements()` over all members (`StatementItem.java:154-165`, `619-626`).
  - *Fill/unification:* `RepetitionGroup.assignParts` (`StatementItem.java:698-714`)
    is a backtracking matcher that requires **every** member to consume a statement;
    read-only display then shows only matched `StatementItem`s
    (`NanopubItem.java:499-503`).
- **SHACL templates:** `processShaclTemplate` maps `sh:minCount 0` →
  `nt:OptionalStatement` per property (`Template.java:1070-1072`); today those land as
  top-level statements only. Per-member optionality lets SHACL-derived templates group
  properties of one node without losing min-count semantics.

## Semantics

For a member statement `st` of group `g`:

1. **Effective optionality:** `st` is effectively optional iff
   `isOptionalStatement(st) || isOptionalStatement(g)`. A group-level flag continues to
   mean "the whole group may be absent" (unchanged); a member-level flag means "this
   triple may be absent while the rest of the group is present."
2. **Publishing:** when the form is submitted, an effectively-optional member whose
   subject/predicate/object don't all resolve is silently dropped; a required member
   with an unresolved element is an error (as today). A **partially** filled optional
   member (one placeholder filled, another empty) is dropped — this mirrors the
   existing behavior of top-level optional statements, where `hasEmptyElements()`
   triggers on any empty element.
3. **Repetition:** in a repeatable group, the member-optionality applies per
   repetition group independently — repetition 1 may include the optional triple and
   repetition 2 omit it.
4. **Filling (view / derive / update):** a group matches a set of statements if all
   required members can be assigned; optional members are assigned if a consistent
   candidate exists and skipped otherwise. Matching is preferred over skipping.
5. **Validity constraint (new):** a group must have **at least one required member**.
   An all-optional group is degenerate: it would "match" zero statements, which makes
   repetition detection loop forever (see below) and makes the group's presence
   meaningless. Treat it at parse time as if the group itself were optional and one
   member required, or reject it — decision below.

## Code changes

### 1. Form mode: validation flag + "(optional)" mark — easy

- `ValueItem` already receives the member's `statementPartId`; change the required-flag
  passed into input components from `rg.isOptional()` to
  `rg.isOptional() || template.isOptionalStatement(statementPartId)`
  (`ValueItem.java:53-80`). Add a small helper on `RepetitionGroup`
  (e.g. `isOptionalPart(IRI partId)`) so the logic lives in one place.
- "(optional)" mark: the `RepetitionGroup` constructor currently places one mark on the
  group's last line when the group is optional (`StatementItem.java:389-394`). Add a
  per-line mark for member-optional lines (same label, rendered on that member's row).
  Keep the group-level mark behavior unchanged.
- The `nanopub-optional` CSS class is applied at `StatementItem` level
  (`StatementItem.java:130-132`) — for member-level optionality apply it (or a new
  `nanopub-optional-part` class) on the member's `StatementPartItem` row instead, so
  the greyed-out styling matches what is actually optional.

### 2. Publishing — easy

In `StatementItem` (`StatementItem.java:154-165`, `589-626`):

- `hasEmptyElements()` → only report true for **required** members with an unresolved
  element; empty optional members are fine.
- `RepetitionGroup.addTriplesTo()` → before emitting a member's triple, if the member
  is effectively optional and any of its three elements is unresolved, skip that
  member (continue with the rest of the group).
- The group-level early return in `StatementItem.addTriplesTo` stays as is (it now
  fires only when a *required* element is empty, thanks to the `hasEmptyElements`
  change).

### 3. Fill/unification — the careful part

`RepetitionGroup.assignParts(partIndex, available)` gets a skip branch
(`StatementItem.java:698-714`):

- For each part, try all candidate statements first (as today). If none leads to a
  full assignment **and the part is effectively optional**, additionally try
  `assignParts(partIndex + 1, available)` without consuming a statement.
- Match-over-skip ordering keeps fills maximal: an optional member is only left
  unfilled when no consistent assignment exists.
- Track skipped parts on the `RepetitionGroup` (e.g. `Set<IRI> unmatchedParts`) filled
  during a successful `fill()` — needed by rendering (below). `matches()` needs no
  extra state since it restores models anyway.

Two subtleties this drags in:

- **Repetition loop guard.** `StatementItem.fill()` loops `while (true)` spawning
  trial repetition groups until one fails to match (`StatementItem.java:259-294`). If
  skipping counted as matching, a repetition where *every* member is skipped would
  match without consuming anything → infinite loop. Guard: a repetition group's
  `matches()`/`fill()` must consume **at least one statement** — equivalently, at
  least one member must actually be assigned. Combined with the "≥1 required member"
  validity constraint this cannot regress existing templates (a required member always
  consumes).
- **Read-only rendering of skipped members.** Matched groups currently render all
  member rows; a skipped optional member would show as an empty row. In read-only
  contexts (`NanopubItem`), hide rows whose part is in `unmatchedParts`. In editable
  fill contexts (derive/update via `PublishForm`), do **not** hide them — render them
  as empty editable fields, exactly like an unmatched top-level optional statement is
  rendered today.
- Ambiguity note: in repeatable groups, optional members increase assignment
  ambiguity (a statement could fill repetition 1's optional slot or start repetition
  2). The backtracker guarantees a *consistent* assignment is found whenever one
  exists; match-over-skip plus statement order makes the common cases deterministic.
  Pathological templates (same predicate on optional and required members with
  interchangeable placeholders) may fill in a different-but-valid arrangement — same
  class of ambiguity that repeatable statements already have.

### 4. Template parsing / validity — small

In `Template` post-processing (near `Template.java:901-910`):

- Enforce the **≥1 required member** rule. Preferred handling: log a warning and
  *treat an all-optional group as if the group itself were `nt:OptionalStatement` with
  all members required* — degrades an authoring mistake to today's semantics instead
  of hard-failing templates in the wild. (Alternative: `MalformedTemplateException`;
  rejected because template parsing failures are user-facing and there may be no way
  to fix someone else's template.)
- No parser change otherwise: `isOptionalStatement` already answers for member IRIs.

### 5. Out-of-scope guards — small

While at it, make the exclusions explicit instead of silently misbehaving:

- A group member typed `nt:RepeatableStatement`: ignore the flag with a warning
  (current behavior is to ignore it implicitly — `isRepeatable()` is only ever asked
  about the group IRI).
- A group member typed `nt:GroupedStatement` (nested group): ignore with a warning;
  members of the inner group already get detached from the top level, so a nested
  declaration today produces confusing half-rendered state.

## Touch points checked and *not* affected

- `willMatchAnyTriple()` (`StatementItem.java:235-237`) — uses `matches()` on a dummy
  statement; with the ≥1-consumed guard a group still needs to match the dummy triple
  with some member, unchanged in practice.
- `isEmpty()` / `RepetitionGroup.isEmpty()` — used to detect untouched optional items;
  member-level optionality doesn't change "no narrow-scope placeholder has a value".
- `Template.isRequiredField()` — already member-aware; becomes *correct* rather than
  aspirational once the rest honors the flag.
- `getFirstOccurrence()` (`Template.java:201-219`) — iterates members already.
- Placeholder shared between an optional member and a required statement elsewhere:
  the required occurrence keeps its required validator; models are shared, so filling
  either fills both. Same situation exists today across top-level statements — no new
  behavior.

## Backwards compatibility

Safe in both directions:

- **Old templates, new code:** no template published today has member-level
  `nt:OptionalStatement` flags that the code was hiding — the flag was simply never
  honored inside groups. All existing templates parse and behave identically (the new
  code paths only activate on the member-level flag).
- **New templates, old code:** an old nanodash (or any other template consumer)
  ignores the member-level flag and treats the member as required — stricter, never
  invalid. Forms demand a value that the author meant as optional; published nanopubs
  are still well-formed. Fill of a nanopub that legitimately omitted the optional
  triple will fail to match the group in old code and fall back to the generic
  (template-less) statement display — degraded, not broken. So: don't add the flag to
  high-traffic shared templates until updated parsers are deployed where those
  templates are opened.
- **nanopub-java port:** `Template` and the unification engine are slated to move to
  nanopub-java ([nanopub-java#91](https://github.com/Nanopublication/nanopub-java/issues/91)).
  Same argument as embedded template identity
  ([template-identity-and-governance.md](template-identity-and-governance.md)): design
  member-level optionality into the ported class rather than porting group-level-only
  and changing it twice. The unification skip-branch belongs to the engine that is
  being re-hosted — coordinate.

## Implementation plan

Suggested order — each step compiles and passes tests on its own:

1. **Template layer:** effective-optionality helper
   (`isOptionalStatementEffective(member, group)` or equivalent), all-optional-group
   normalization with warning, out-of-scope-flag warnings. Unit tests on a fixture
   template with a mixed group.
2. **Publish path:** `hasEmptyElements` + `addTriplesTo` member-aware skipping. Tests:
   submit with optional member empty (triple absent), partially filled (dropped),
   filled (present); required member empty still errors.
3. **Form UI:** per-member required-flag into input components, per-line "(optional)"
   mark, optional-part styling. Manual check in the dev jetty.
4. **Fill path:** skip branch in `assignParts` with match-over-skip ordering,
   ≥1-consumed repetition guard, `unmatchedParts` tracking, read-only row hiding.
   Tests: view a nanopub omitting the optional triple (group matches, row hidden);
   derive/update from it (row shown editable and empty); repeatable group where
   repetition 1 has the optional triple and repetition 2 doesn't; all-optional group
   fixture confirming no infinite loop.
5. **End-to-end:** publish a test template with a mixed group (e.g. the
   class/label/optional-comment shape above), fill → publish → reopen → update
   round-trip on the dev instance.
6. **Later, out of band:** revisit the SHACL mapping so `sh:minCount 0` properties can
   live inside groups; consider surfacing member-level optionality in the template
   creation template once the feature is proven.
