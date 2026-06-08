# Session-bound ("magic") query parameters

**Status:** 🚧 In progress — phase 1 (binding infra + UI-field suppression) implemented (`MagicQueryParams`, wired into the view builders; registry `LOCALPUBKEY`/`SITEURL`). Entry-action visibility, the introductions cutover, and the magic-aware introductions query/view are pending.

A **magic query parameter** is a view-query placeholder that Nanodash fills
automatically from the current browser session, rather than from a value the
caller passes or the user types into a form. It exists so that a *data-driven
view* can branch on session/site-local state — the local signing key, the site
URL — that a SPARQL query keyed only on a resource IRI cannot otherwise see.

The motivating case is the **Introductions** listing on a user's About page
(`AboutUserPanel` / `ProfileIntroItem`). Today the owner gets a hand-built,
editable companion (`ProfileIntroItem`) *instead of* the read-only
introductions-view, because the editable workflow branches on the session's
local key (which introductions declare it, whether it is approved, which keys
are missing). Magic params let the query compute those flags, so most of that
custom panel can be replaced by a proper view. See the gap analysis at the end
for what becomes declarative and what stays custom.

This complements, and is independent of, the role-dependent action work: magic
params supply the *data*; role gating and per-row action visibility decide which
*buttons* render.

## How placeholders work today (background)

View queries are grlc/BASIL queries published as nanopublications, parsed by
`QueryTemplate` (nanopub-java) and wrapped by `GrlcQuery`. Every placeholder is
a SPARQL variable written with a **leading underscore** — the BASIL `?_param`
convention — plus optional type/cardinality markers. The conventions, verified
against `org.nanopub:nanopub:1.90.0`:

| Marker | Method | Meaning | Example |
| --- | --- | --- | --- |
| leading `_` | (prefix on every placeholder) | marks the variable as a placeholder | `?_user` |
| leading `__` | `isOptionalPlaceholder` (`startsWith("__")`) | placeholder is optional | `?__user` |
| `_iri` suffix | `isIriPlaceholder` (`endsWith`) | bind as an IRI, not a literal | `?_user_iri` |
| `_multi` / `_multi_iri` / `_multi_val` suffix | `isMultiPlaceholder` (`endsWith`) | multi-valued `VALUES` block | `?_keys_multi_val` |

`QueryTemplate.getParamName(raw)` strips the leading underscore(s) **and** the
type suffix to produce the wire/QueryRef parameter name:

```
getParamName("_user_iri")            = "user"
getParamName("_LOCALPUBKEY_multi_val") = "LOCALPUBKEY"
getParamName("__optionalfoo")        = "optionalfoo"
```

This is why callers pass plain names — `new QueryRef(queryId, "user", iri)` —
even though the SPARQL variable is `?_user_iri`.

Two behaviours we rely on:

- **Cache key.** `ApiCache` keys every response on `queryRef.getAsUrlString()`,
  i.e. the full set of parameters. Anything folded into the `QueryRef` therefore
  partitions the cache automatically — two viewers with different local keys get
  distinct cache entries, no collisions.
- **Graceful absence.** `GrlcQuery.expandQuery(params, false)` is non-strict:
  for a **multi-valued** placeholder with no value, the empty `VALUES` block is
  dropped and the query still runs. So an unbound magic param leaves the query
  valid (e.g. `?declares_local_key` comes back false everywhere), degrading to
  the plain read-only view for logged-out or non-owner viewers.

## What makes a parameter "magic"

A placeholder is magic **iff its parameter name is a registered magic name**.
Detection is pure registry membership — nothing else:

```java
isMagic(rawPlaceholder) = REGISTRY.containsKey(QueryTemplate.getParamName(rawPlaceholder));
```

### The registry

| Magic name | Declare in SPARQL as | Bound from | Notes |
| --- | --- | --- | --- |
| `LOCALPUBKEY` | `?_LOCALPUBKEY_multi_val` | `NanodashSession.get().getPubkeyString()` | the load-bearing one — drives every per-row flag and the create/derive key parameter |
| `SITEURL` | `?_SITEURL_multi_val` | `NanodashPreferences.get().getWebsiteUrl()` | `key-location` prefill; genuine non-derivable deployment state |

Declared as `_multi_*` so absence drops the `VALUES` block (see "graceful
absence"). When the session has no value (logged out, no key pair), the binding
is simply omitted.

Deliberately **not** in the registry:

- `LOCALPUBKEYSHORT` — not independent session state; it is a pure function of
  `LOCALPUBKEY` (`getShortPubkeyName(SHA-256(pubkey))`). Compute it in the query
  or in action-link expansion if ever needed; do not give a derived value its
  own slot.
- `LOCALINTRO` — only the deferred *include-keys* action needs the specific
  local-introduction IRI (for `supersede`). Every other flow derives from
  `LOCALPUBKEY` alone. Add it back only when include-keys goes declarative.
- `CURRENTUSER` — the introductions view is already keyed on the page user. Add
  it only when a view needs "who is viewing" distinct from "whose page," for the
  general role-action work.

### Naming: uppercase is a best practice, not a rule

Magic names are written in `SCREAMING_CASE` so they stand out to query authors
as "filled by the platform, no form field here." This is **style only** — the
binding layer never inspects case, just registry membership. A user-defined
placeholder may also be uppercase (e.g. `?_FOO_iri`); since `FOO` is not in the
registry it is treated as an ordinary placeholder (UI field rendered, value
supplied by the caller).

**Tradeoff:** the registered names are *reserved*. A query that genuinely
declared a non-magic parameter named `LOCALPUBKEY` or `SITEURL` would have it
auto-bound. This is the standard reserved-word cost and is acceptable for a
small, well-known set.

## Binding mechanism

### Where

A single choke point: **`QueryResultTableBuilder.build()`** (and the sibling
list / paragraph builders), immediately before `ApiCache.retrieveResponseAsync`.

This location is required, not incidental:

- It runs on the **request thread**, where `NanodashSession.get()` is valid. The
  actual fetch runs in `NanodashThreadPool` background threads where the Wicket
  session is **not** available — so binding must be eager here, before the
  `QueryRef` is handed off.
- It already holds `viewDisplay.getView().getQuery()`, so it can scan
  `getPlaceholdersList()` for magic names.
- Folding bindings into the `QueryRef` keeps the cache key correct for free.

### How

```java
// Augment a QueryRef with session-bound magic parameters, on the request thread.
Multimap<String, String> params = LinkedHashMultimap.create(queryRef.getParams());
for (String raw : view.getQuery().getPlaceholdersList()) {
    String name = QueryTemplate.getParamName(raw);   // e.g. "LOCALPUBKEY"
    MagicParam mp = REGISTRY.get(name);
    if (mp == null) continue;
    for (String value : mp.resolve()) {              // empty if session has no value
        params.put(name, value);                     // key = wire name (stem)
    }
}
queryRef = new QueryRef(queryRef.getQueryId(), params);   // ctor (String, Multimap)
```

Add bindings **deterministically** (e.g. sorted) so `getAsUrlString()` is stable
across requests and the cache key does not churn.

### UI-field suppression

`GrlcQuery.createParamFields` iterates placeholders to build editable
`QueryParamField`s for the publish/query forms. Skip magic placeholders there so
they never appear as user-editable inputs.

## Turning magic data into buttons (separate, reusable features)

Magic params provide row data; the pieces below (shared with the
role-dependent action work, not specific to introductions) turn that data into
conditional buttons. None of them add a new view-definition predicate.

### Per-row visibility: empty mapped value into a required target hides the button

An entry action's per-row button is **not rendered when its mapped value is
empty *and* the target is required**. There is no `visible-if` predicate;
visibility rides on the template's existing optionality plus conditional binding
in the query.

The mapped target decides what "required" means:

- **`param_X` → a non-optional template placeholder:** empty value hides the
  button. An *optional* placeholder tolerates an empty value, so the button
  stays (its mapped value was just a nice-to-have prefill).
- **fill-mode / structural key (`derive-a`, `supersede`):** inherently required
  — a derive/supersede with no nanopub is meaningless — so an empty value hides
  the button.

"Empty" means null or blank (`value == null || value.isBlank()`); an unbound
SPARQL variable comes back blank.

The query author expresses per-row visibility by **binding the action's target
column conditionally**, e.g.

```sparql
BIND( IF(?retractable, ?np_iri, "") AS ?retract_target )   # then map retract_target:nanopubToBeRetracted
```

so the compound conditions (`declares_local_key && count > 1`) live in the query
where they belong. Template optionality is read from `Template.isOptionalStatement`
/ the `OPTIONAL_STATEMENT` type (a placeholder is required iff it appears in at
least one non-optional statement); confirm the cheapest way to ask this — derive
it, or reuse the publish form's required-field logic.

Free wins: *include-keys* hides itself when there is nothing to include (empty
missing-keys column), and every key-dependent action vanishes when logged out
(`LOCALPUBKEY` unbound → empty).

### Multiple mappings per action + non-`param_` targets

Today an action carries a single `queryVar:templateParam` mapping that only sets
`param_X`. Two extensions, folded together because `derive` needs both:

- allow a **list** of mappings per action;
- allow a mapping to target a **non-`param_` URL key** (`derive-a`, `supersede`).

`derive` then declares two: `derive_target → derive-a` (conditional, drives
visibility) and `local_pubkey → public-key__.1`.

### Multi-value column → indexed params

Extend a mapping so a `_multi*` source column expands to
`param_<templateParam>__.1..N`. Only *include-keys* needs this.

### Echo-as-column (no code)

A query may `SELECT` a magic variable back out
(`?_LOCALPUBKEY_multi_val AS ?local_pubkey`) and feed it to an action via an
ordinary mapping (e.g. derive's key parameter).

## Worked example: the introductions view

Query keyed on `?_user_iri` plus magic `?_LOCALPUBKEY_multi_val`:

```sparql
SELECT ?np_iri ?date ?location ?keys_multi_val
       ?declares_local_key ?retractable ?derivable
       ?local_pubkey          # echoed for derive's key bundle
WHERE {
  # ... introductions of ?_user_iri ...
  BIND( EXISTS { ?np npx:hasKeyDeclaration/npx:hasPublicKey ?_LOCALPUBKEY_multi_val }
        AS ?declares_local_key )
  BIND( (?declares_local_key && ?localCount > 1) AS ?retractable )
  BIND( (!?declares_local_key && ?localCount = 0) AS ?derivable )
}
```

A conditionally-bound target column drives each row action's visibility:

```sparql
BIND( IF(?retractable, ?np_iri, "") AS ?retract_target )
BIND( IF(?derivable,   ?np_iri, "") AS ?derive_target )
```

Action declarations on the view nanopub:

| Action | Type | Mappings (empty target → button hidden) |
| --- | --- | --- |
| Create Introduction | result action (owner-gated) | echoed `local_pubkey` → `public-key`, `SITEURL` → `key-location` |
| retract | entry action | `retract_target → nanopubToBeRetracted` (required) |
| derive | entry action | `derive_target → derive-a` (required, fill-mode key) + `local_pubkey → public-key__.1` |

`retract_target` / `derive_target` are empty for rows where the action does not
apply, so the empty-into-required rule hides the button there. The table and
these three row actions all become declarative.

## Gap analysis: what stays custom

- **Recommended-Actions prose.** The natural-language guidance on the owner's
  About tab (keyed on approval / local-intro state) is not a view, table, or
  action. It stays a small owner-only companion.
- **include-keys.** The strained one. Beyond multi-value → indexed expansion it
  needs three correlated indexed parameters per missing key (`public-key`,
  `key-declaration`, `key-declaration-ref`); the two `key-declaration*` values
  are `getShortPubkeyName(SHA-256(pubkey))`, whose Nanodash-specific formatting
  does not map cleanly to SPARQL string functions. Ship create/derive/retract
  declaratively first; keep include-keys custom (or as a derive-only flow) until
  the short-name derivation is worth pushing into the query — at which point
  `LOCALINTRO` returns to the registry.

## Touch points

| Change | File |
| --- | --- |
| Magic registry + `isMagic` / binding helper | new `SessionQueryBindings.java` (or similar) |
| Call binding before fetch | `QueryResultTableBuilder.build()` (+ list / paragraph builders) |
| Suppress UI fields for magic params | `GrlcQuery.createParamFields` / `QueryParamField` |
| Empty-into-required hides entry action | `QueryResultTable` (entry-action loop), reading `Template` optionality |
| Multiple mappings per action + non-`param_` targets | `View` / action model, `QueryResultTable`, `QueryResultTableBuilder` |
| Multi → indexed expansion | `QueryResultTable` action mapping |

No new view-definition predicate is introduced: visibility rides on the
template's existing optionality. The placeholder conventions
(`QueryTemplate.getParamName` / `isMultiPlaceholder` / `isOptionalPlaceholder`)
are external (nanopub-java) and need no change.

## Phasing

1. **Magic-param binding + UI-field suppression.** ✅ Done — `MagicQueryParams`
   (registry `LOCALPUBKEY`/`SITEURL`, `isMagic`, request-thread `augment`), wired
   into the five view-builder constructors; `GrlcQuery.createParamFields` skips
   magic placeholders. Inert until a query declares one. *Still to do:* the wire
   smoke-test (grlc binds `?_LOCALPUBKEY_multi_val` from URL param `LOCALPUBKEY`)
   once a magic query is published in phase 3.
2. **Empty-into-required hides entry-action buttons.** Skip an entry action for a
   row when its mapped value is empty and the target is required (non-optional
   placeholder, or a fill-mode key). No new predicate; feeds the role-dependent
   action work too.
   - **2b. Multiple mappings per action + non-`param_` targets.** Folds in with
     phase 2; needed by `derive` (two mappings, one targeting `derive-a`).
3. **Republish the introductions view** with the magic query and the
   create/derive/retract actions; drop the bespoke table from `ProfileIntroItem`,
   keeping only the Recommended-Actions companion.
4. **Multi-expand + include-keys**, only if worthwhile (also returns `LOCALINTRO`
   to the registry).

## To verify before building

The convention semantics above are confirmed in-process against nanopub-java
1.90.0. The one unverified link is the full wire round-trip: that grlc /
nanopub-query binds SPARQL variable `?_LOCALPUBKEY_multi_val` from URL parameter
`LOCALPUBKEY` exactly as it already does for `user` → `?_user_iri`. It almost
certainly does (same `getParamName` canonicalization), but it is worth one
integration smoke-test — a throwaway query echoing the parameter — before
committing to the convention.
