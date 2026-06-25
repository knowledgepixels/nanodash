# Aligning Nanodash templates with SHACL

**Status:** 📋 Proposed — see [issue #508](https://github.com/knowledgepixels/nanodash/issues/508) (Part A) and [issue #509](https://github.com/knowledgepixels/nanodash/issues/509) (Part B)

Nanodash drives its nanopublication authoring forms from **templates** — a
small RDF vocabulary (`nt:`, `https://w3id.org/np/o/ntemplate/`, defined in
`org.nanopub.vocabulary.NTEMPLATE`) that describes form fields and the triples
they produce. This vocabulary overlaps substantially with
[SHACL](https://www.w3.org/TR/shacl/), the W3C standard for describing
constraints over RDF. This document proposes two complementary, independently
shippable pieces of work to build on the SHACL standard where it fits:

- **Part A — adopt SHACL's constraint and form-hint vocabulary inside
  templates**, so the constraint layer of a template is expressed with `sh:`
  (and `dash:`) terms rather than nanodash-specific ones. *(Mostly already
  prototyped — see "The existing bridge" below.)*
- **Part B — export a SHACL shape from a template**, so nanopublications can be
  validated by any standard SHACL engine, independently of nanodash.

The two parts are related but separable: A changes the *input* dialect a
template author can use; B adds an *output* artifact for external validation.

## Background: how the two models relate

Templates and SHACL are both RDF-native, self-describing, web-resolvable
schema artifacts, and both are increasingly used to drive form UIs (SHACL via
the [DASH](https://datashapes.org/dash) vocabulary). Their constraint
vocabularies map almost one-to-one. But they differ in **purpose** and
**structure**:

- **Purpose.** A template is *generative/prescriptive*: it is a graph skeleton
  *by example*, with some positions left as placeholders, that a form fills in
  to **construct** a new graph. SHACL is *descriptive/validating*: it describes
  what conformant graphs look like and is applied to data that already exists,
  in an open-world way.
- **Structure.** A template uses **RDF reification** (`rdf:subject` /
  `rdf:predicate` / `rdf:object`) to pin down *exact triples*, and a placeholder
  may sit in **subject, predicate, or object** position. A SHACL property shape
  only ever constrains the **object end of a fixed predicate** (`sh:path`); the
  focus node is the subject and the path cannot be a variable.

Constraint-vocabulary correspondence:

| Concept | Template (`nt:`) | SHACL (`sh:` / `dash:`) |
| --- | --- | --- |
| Field label | `rdfs:label` | `sh:name` |
| Description | `dct:description` | `sh:description` |
| Datatype | `nt:hasDatatype` | `sh:datatype` |
| Language | `nt:hasLanguageTag` | `sh:languageIn` / `sh:uniqueLang` |
| Regex | `nt:hasRegex` | `sh:pattern` (+ `sh:flags`) |
| Closed enumeration | `nt:possibleValue` | `sh:in` |
| Fixed value | fixed `rdf:object` on a statement | `sh:hasValue` |
| Value is a URI / literal | `nt:UriPlaceholder` / `nt:LiteralPlaceholder` | `sh:nodeKind` `sh:IRI` / `sh:Literal` |
| Class restriction on value | `nt:hasTargetNanopubType` (loosely) | `sh:class` |
| Optional | `nt:OptionalStatement` | `sh:minCount 0` |
| Repeatable | `nt:RepeatableStatement` | (no `sh:maxCount`) / `sh:maxCount > 1` |
| Required | non-optional statement | `sh:minCount ≥ 1` |
| Ordering | `nt:statementOrder` | `sh:order` |
| Grouping | `nt:GroupedStatement` | `sh:group` / `sh:PropertyGroup` |
| Default value | `nt:hasDefaultValue` | `sh:defaultValue` |
| Widget kind | placeholder subtype (e.g. `nt:GuidedChoicePlaceholder`) | `dash:editor` (e.g. `dash:AutoCompleteEditor`) |
| Dynamic value source | `nt:possibleValuesFrom` / `nt:possibleValuesFromApi` | DASH / SHACL-AF SPARQL |
| Schema target | `nt:hasTargetNanopubType` | `sh:targetClass` |

Concepts with **no SHACL equivalent** (they remain nanodash-specific in both
parts): resource minting and provenance roles (`nt:LocalResource`,
`nt:IntroducedResource`, `nt:EmbeddedResource`, `nt:hasTargetNamespace`), the
auto-filled magic slots (`nt:CREATOR`, `nt:ASSERTION`, `nt:NANOPUB`,
`nt:RootNanopubPlaceholder`), template/provenance/pubinfo roles
(`nt:hasDefaultProvenance`, `nt:hasRequiredPubinfoElement`,
`nt:hasNanopubLabelPattern`, `nt:hasTag`), the collapsed-by-default
`nt:AdvancedStatement`, and live value sources (`nt:possibleValuesFromApi`).

## The existing bridge

`Template.java` (`com.knowledgepixels.nanodash.template.Template`) already
contains a working, experimental SHACL ingestion path. `processTemplate()`
checks whether the assertion is typed as an `nt:*Template`; if not, it falls
through to **`processShaclTemplate()`**, which reads a `sh:NodeShape` and maps:

| SHACL input | Bridge behaviour |
| --- | --- |
| `sh:targetClass` | becomes the template's base node; synthesizes a `«shape»+subj` `nt:IntroducedResource` and an `?subj rdf:type «class»` statement |
| `sh:property` | each property shape → an `nt:hasStatement`; synthesizes a `«shape»+subj` `nt:LocalResource` + `nt:UriPlaceholder` subject |
| `sh:path` | → the statement's `rdf:predicate` |
| `sh:hasValue` | → fixed `rdf:object` |
| `sh:minCount ≤ 0` (or absent) | → adds `nt:OptionalStatement` |
| `sh:maxCount > 1` (or absent) | → adds `nt:RepeatableStatement` |
| object otherwise unspecified | synthesizes a `«stmt»+obj` `nt:ValuePlaceholder` |
| `nt:hasDatatype`, `nt:hasRegex`, `nt:possibleValue*`, `nt:hasDefaultValue`, `rdfs:label`, `nt:statementOrder` | reused as-is, *alongside* the `sh:` terms |

Two consequences matter for the plan:

1. The bridge **proves the constraint subset is co-expressible** and that
   `sh:` and `nt:` terms can coexist in one assertion graph.
2. It also **delineates the faithful subset**: the only template features it
   can receive from SHACL are exactly the ones that will survive being exported
   back out (Part B). What it cannot receive — predicate-position placeholders,
   grouped statements, live value sources, cross-placeholder joins — are
   precisely the constructs that resist a faithful SHACL projection.

Notably, no SHACL *engine* is invoked at runtime today: SHACL terms are parsed
once into the template's internal maps. SHACL is currently an **input dialect**,
not a validation runtime. Part B is what would make SHACL validation real.

## Part A — adopt SHACL constraint terms inside templates

**Goal.** Let template authors express the *constraint and form-hint* layer of
a template with `sh:` and `dash:` terms, keeping a thin `nt:` namespace only for
the generative and nanopub-specific concepts SHACL does not model. This builds
on the standard for ~70% of what a template says, and the bridge already does
much of the work.

### Worked example

The real template *"Defining a FAIR IN community"*
(`scratch/fair-community-template.trig`), today in reified `nt:` form:

```turtle
:assertion a nt:AssertionTemplate ;
    rdfs:label "Defining a FAIR IN community" ;
    nt:hasStatement :st0, :st1, :st2, :st3 .
:community a nt:LocalResource ; rdfs:label "This community" .
:name     a nt:LiteralPlaceholder ; rdfs:label "the name of your community" .
:comment  a nt:LiteralPlaceholder ; rdfs:label "description …" .
:website  a nt:UriPlaceholder     ; rdfs:label "a link to … website" .
:st0 a rdf:Statement ; rdf:subject :community ; rdf:predicate rdf:type    ; rdf:object icc:Community ; nt:statementOrder 0 .
:st1 a rdf:Statement ; rdf:subject :community ; rdf:predicate rdfs:label   ; rdf:object :name        ; nt:statementOrder 1 .
:st2 a rdf:Statement ; rdf:subject :community ; rdf:predicate rdfs:comment ; rdf:object :comment     ; nt:statementOrder 2 .
:st3 a rdf:Statement ; rdf:subject :community ; rdf:predicate rdfs:seeAlso ; rdf:object :website     ; nt:statementOrder 3 .
```

in the proposed hybrid `sh:` + `dash:` + `nt:` form:

```turtle
:assertion a nt:AssertionTemplate, sh:NodeShape ;
    rdfs:label "Defining a FAIR IN community" ;
    sh:targetClass icc:Community ;          # st0 (the rdf:type triple) AND the
                                            #   subject placeholder collapse here
    sh:property :st1, :st2, :st3 .

:st1 a sh:PropertyShape ;
    sh:path rdfs:label ; sh:name "is called" ;
    sh:datatype xsd:string ; dash:editor dash:TextFieldEditor ;   # nt:LiteralPlaceholder
    sh:minCount 1 ; sh:maxCount 1 ; sh:order 1 .

:st2 a sh:PropertyShape ;
    sh:path rdfs:comment ; sh:name "has the description" ;
    sh:datatype xsd:string ; dash:editor dash:TextAreaEditor ;    # nt:LongLiteralPlaceholder
    sh:order 2 .

:st3 a sh:PropertyShape ;
    sh:path rdfs:seeAlso ; sh:name "has the website" ;
    sh:nodeKind sh:IRI ; dash:editor dash:URIEditor ;             # nt:UriPlaceholder
    sh:order 3 .
```

### Widget-type mapping (`nt:` placeholder → `dash:editor`)

`sh:nodeKind` distinguishes URI vs. literal but not the *widget*. DASH editor
hints carry the rest:

| `nt:` placeholder | proposed `dash:editor` (+ `sh:` hint) |
| --- | --- |
| `nt:LiteralPlaceholder` | `dash:TextFieldEditor` + `sh:datatype` |
| `nt:LongLiteralPlaceholder` | `dash:TextAreaEditor` |
| `nt:LiteralPlaceholder` + `xsd:date` | `dash:DatePickerEditor` |
| `nt:LiteralPlaceholder` + `xsd:dateTime` | `dash:DateTimePickerEditor` |
| `nt:UriPlaceholder` | `dash:URIEditor` + `sh:nodeKind sh:IRI` |
| `nt:RestrictedChoicePlaceholder` | `dash:EnumSelectEditor` + `sh:in` |
| `nt:GuidedChoicePlaceholder` | `dash:AutoCompleteEditor` (+ `nt:possibleValuesFromApi`) |
| `nt:AgentPlaceholder` | `dash:AutoCompleteEditor` + nanodash agent role |

### What stays `nt:`

The minting/provenance/UI-only terms listed under "Background" have no SHACL
home and remain in the nanodash namespace. The subject placeholder and its
"short ID as URI suffix" label are **synthesized** by the bridge from
`sh:targetClass`; an author who needs an explicit subject label or an explicit
`nt:LocalResource` minting hint still supplies it with `nt:`.

### Compatibility and rollout

- **Non-breaking.** The `nt:` reified form keeps working unchanged; the hybrid
  form is an *additional* accepted dialect (the bridge already accepts it).
- **Incremental.** Extend `processShaclTemplate()` to also read `dash:editor`
  (today the widget type still comes from `nt:`), `sh:name`, `sh:order`,
  `sh:in`, `sh:pattern`, `sh:datatype`, `sh:defaultValue`.
- **Authoring.** The template editor can keep emitting `nt:`; hybrid templates
  are primarily for authors who hand-write SHACL or import existing shapes.

## Part B — export a SHACL shape for external validation

**Goal.** Generate, from a template, a SHACL shape that any standard engine
(rdf4j-shacl, pySHACL, TopBraid …) can use to validate nanopublications created
from that template — independently of nanodash, and producing a standard
`sh:ValidationReport`.

### Is it structurally possible?

Yes — as a **lossy projection over a well-defined subset**, not an
isomorphism. Crucially, the generative/descriptive mismatch is *not* the
blocker for B: validation does not require SHACL to construct anything, only to
describe conformant results, which is exactly its job. The existing inward
bridge already delineates the exportable subset (run its correspondence table
backwards).

**Projects faithfully into core SHACL (most production templates):**
- Fixed-subject, fixed-predicate, placeholder-object triples → `sh:PropertyShape`
  (path + datatype/nodeKind/pattern/in/cardinality). The bulk of every template.
- Fixed triples (constant S/P/O) → `sh:hasValue` (or `sh:targetClass` for types).
- Optional / required / repeatable → `sh:minCount` / `sh:maxCount`.
- Inline restricted choice (`nt:possibleValue`) → `sh:in`.
- A placeholder reused as the **subject** of several triples → property shapes
  on the **same** NodeShape (SHACL's focus-node model handles same-node
  co-reference natively); anchored nested groups → `sh:node` /
  `sh:qualifiedValueShape`.

**Needs SHACL-AF (SPARQL-based constraints) — possible but non-core:**
- **Predicate-position placeholders** (`?s ?relation ?o`): `sh:path` cannot be a
  variable. Only `sh:sparql` can validate these.
- **Arbitrary value-equality joins** between two *independent* placeholders:
  `sh:equals` covers the narrow property-pair case; richer joins need SPARQL.
- **Live value sources** (`nt:possibleValuesFromApi` / `nt:possibleValuesFrom`):
  `sh:in` is static; snapshot the values at export time, or use a SPARQL target.

**Dropped (harmless — not data-validity concerns):**
`nt:hasNanopubLabelPattern`, `nt:hasTag`, `nt:AdvancedStatement`, the
local/introduced/embedded minting roles, `nt:hasDefaultProvenance`,
`nt:hasRequiredPubinfoElement`.

### Two semantic caveats

1. **SHACL validates looser than the template generates.** A template emits
   *exactly* its triples; SHACL (open-world) only checks that required triples
   are present and constraints hold — it will not reject *extra* triples by
   default. `sh:closed` only closes declared paths per node-shape and does not
   capture the whole-graph "this is precisely the triple set" notion. So a
   generated shape answers *"is this graph conformant to the template's
   constraints?"* — **not** *"was it produced by template T?"*. The latter is
   already answered by the `nt:wasCreatedFromTemplate` pubinfo triple, and the
   looser semantics is usually what a validator wants anyway.
2. **Targetability varies.** When a template introduces a typed resource
   (`?x a SomeClass`), `sh:targetClass` targets cleanly. When it only relates
   pre-existing URIs with no new typed node, there is no natural target class;
   fall back to `sh:targetSubjectsOf` / `sh:targetObjectsOf` a fixed predicate.
   `sh:targetNode` is unusable (the minted IRI is not known in advance).

### Verdict and scope

Worth doing, scoped honestly as a **validation projection, not a round-trip**.
Generate core-SHACL shapes for the faithful subset, emit SHACL-AF `sh:sparql`
for predicate-placeholder and cross-placeholder-join cases, snapshot dynamic
enums, and drop the UI/provenance terms. Document each generated shape as
"structurally consistent with the template," not "generable by it."

## Open questions

- Should A target **core SHACL + DASH**, or also define a minimal nanodash
  editor-hint vocabulary to avoid a DASH dependency?
- For B, do we want shapes published **as nanopublications** themselves (so they
  are FAIR and resolvable), and keyed to the template via `dct:isVersionOf` /
  `nt:wasCreatedFromTemplate`?
- Should B emit **core-only** shapes (dropping the SPARQL-needing constructs and
  logging what was dropped) first, and add SHACL-AF later?
- Is there appetite to upstream the `sh:`/`dash:` reading into `nanopub-java`'s
  `NTEMPLATE`/template handling, so it is not nanodash-specific?

## Issues

- Part A — adopt SHACL constraint/form-hint vocabulary inside templates: [#508](https://github.com/knowledgepixels/nanodash/issues/508)
- Part B — export SHACL shapes from templates for external validation: [#509](https://github.com/knowledgepixels/nanodash/issues/509)
