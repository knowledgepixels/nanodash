# Session-bound ("magic") query parameters

**Status:** ✅ Complete. Phases 1–3 are implemented and live: binding infra + UI-field suppression, empty-into-required entry-action visibility (with multiple / non-`param_` mappings), and the introductions + recommended-actions listings republished as views — `ProfileIntroItem` removed entirely. The wire convention is **verified live** against `query.knowledgepixels.com` (absent → all rows, flags empty; present → per-row `declares_local_key`/`retract_target` set for intros declaring the local key). The one `ProfileIntroItem` action that couldn't be expressed as a view action — *include keys* — was **dropped** (2026-06-09), so nothing remains custom.

A **magic query parameter** is a view-query placeholder that Nanodash fills
automatically from the current browser session, rather than from a value the
caller passes or the user types into a form. It exists so that a *data-driven
view* can branch on session/site-local state — the local signing key, the site
URL — that a SPARQL query keyed only on a resource IRI cannot otherwise see.

The motivating case was the **Introductions** listing on a user's About page
(`AboutUserPanel`). The owner used to get a hand-built, editable companion
(`ProfileIntroItem`) *instead of* the read-only introductions-view, because the
editable workflow branches on the session's local key (which introductions
declare it, whether it is approved). Magic params let the query compute those
flags, so the whole custom panel — both the introductions table and the
recommended-actions companion — became views, and `ProfileIntroItem` was removed
entirely. See the introductions-view design below for how.

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
| `_multi` / `_multi_iri` suffix | `isMultiPlaceholder` (`endsWith`) | multi-valued `VALUES` block | `?_keys_multi` |

> **Wire caveat (verified empirically against `query.knowledgepixels.com`).** The
> live grlc service does **not** match nanopub-java's conventions one-for-one:
> - A **single** (non-multi) placeholder is *text-substituted* into the SPARQL, so
>   it cannot be tested with `bound(?_x)` (you get `bound("literal")` → malformed).
> - A **multi** placeholder is bound through an explicit, author-written
>   `values ?_x_multi {}` block in the SPARQL; absence empties the block gracefully
>   (no error, all rows returned), presence fills it. This is the only form that is
>   both bindable *and* absent-tolerant — so magic params use it.
> - The `_multi_val` suffix is **not** recognized by the service; use plain `_multi`
>   for literals and `_multi_iri` for IRIs.
>
> **Possible future (service-side, nanopub-query/grlc).** Today there is no form
> that is optional, *single-valued*, **and** bindable: to get absent-tolerant +
> bound you must use `_multi`, modeling a singular value (e.g. `LOCALPUBKEY`) as a
> set. We could let an **optional placeholder `?__x` (no `_multi`) opt into a
> `values ?__x {}` block** too — the presence of the block (not the `_multi`
> suffix) becoming the "bind vs. text-substitute" signal, with single ⇒ cardinality
> ≤ 1 (a second value errors instead of silently cross-joining). That would let the
> magic params be `?__LOCALPUBKEY` / `?__SITEURL` / `?__CURRENTUSER_iri` with honest
> cardinality and drop the "single dressed as multi" caveat. Not needed for the
> current work; revisit when touching the service. Tracked in
> [nanodash#481](https://github.com/knowledgepixels/nanodash/issues/481).

`QueryTemplate.getParamName(raw)` strips the leading underscore(s) **and** the
type suffix to produce the wire/QueryRef parameter name:

```
getParamName("_user_iri")           = "user"
getParamName("__LOCALPUBKEY_multi") = "LOCALPUBKEY"
getParamName("__optionalfoo")       = "optionalfoo"
```

This is why callers pass plain names — `new QueryRef(queryId, "user", iri)` —
even though the SPARQL variable is `?_user_iri`.

Two behaviours we rely on:

- **Cache key.** `ApiCache` keys every response on `queryRef.getAsUrlString()`,
  i.e. the full set of parameters. Anything folded into the `QueryRef` therefore
  partitions the cache automatically — two viewers with different local keys get
  distinct cache entries, no collisions.
- **Graceful absence.** A **multi** placeholder with no value leaves its
  author-written `values ?__NAME_multi {}` block empty and the query still runs
  (verified live: all rows returned, no error). So an unbound magic param leaves
  the query valid (e.g. `?declares_local_key` comes back empty everywhere),
  degrading to the plain read-only view for logged-out or non-owner viewers.
  Because the variable may be unbound, comparisons against it must be
  `coalesce`-guarded (`coalesce(str(?pubkey) = str(?__LOCALPUBKEY_multi), false)`)
  rather than `bound()`-tested.

## What makes a parameter "magic"

A placeholder is magic **iff its parameter name is a registered magic name**.
Detection is pure registry membership — nothing else:

```java
isMagic(rawPlaceholder) = REGISTRY.containsKey(QueryTemplate.getParamName(rawPlaceholder));
```

### The registry

| Magic name | Declare in SPARQL as | Bound from | Notes |
| --- | --- | --- | --- |
| `LOCALPUBKEY` | `?__LOCALPUBKEY_multi` | `NanodashSession.get().getPubkeyString()` | the load-bearing one — drives every per-row flag and the create/derive key parameter |
| `SITEURL` | `?__SITEURL_multi` | `NanodashPreferences.get().getWebsiteUrl()` | `key-location` prefill; genuine non-derivable deployment state |
| `CURRENTUSER` | `?__CURRENTUSER_multi_iri` | `NanodashSession.get().getUserIri()` | the viewer's agent IRI; lets a view tell "is the viewer the page user" — the owner gate the recommended-actions view needs (the create case can't rely on key-match) |

Declared as **optional multi** (`__…_multi`) with an explicit empty
`values ?__NAME_multi {}` block, so absence empties the block gracefully (see
"graceful absence"). When the session has no value (logged out, no key pair), the
binding is simply omitted and the variable stays unbound.

Deliberately **not** in the registry:

- `LOCALPUBKEYSHORT` — not independent session state; it is a pure function of
  `LOCALPUBKEY` (`getShortPubkeyName(SHA-256(pubkey))`). Compute it in the query
  or in action-link expansion if ever needed; do not give a derived value its
  own slot.
- `LOCALINTRO` — only the dropped *include-keys* action would have needed the
  specific local-introduction IRI (for `supersede`). Every live flow derives from
  `LOCALPUBKEY` alone.
(`CURRENTUSER` was initially parked here, but the recommended-actions view needs
it for its owner gate, so it is now in the registry above.)

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

- allow **multiple** mappings per action — but as a **single whitespace-separated
  literal** in one `gen:hasActionTemplateQueryMapping` value, *not* a repeated
  statement: the action is declared inside the view-creation template's repeated
  action group, which can't itself contain a repeated/optional statement. (Same
  space-separated convention used for role `regularProperties`.) `View` splits the
  literal (`parseMappingLiteral`);
- allow a mapping to target a **non-`param_` URL key** via an `@` prefix
  (`@derive-a`, `@supersede`).

`derive` then declares one literal with two mappings:
`"derive_target:@derive-a local_pubkey:public-key__.1"` — the first drives
visibility (conditional target), the second supplies the key.

### Echo-as-column (no code)

A query may `SELECT` a magic variable back out
(`(sample(?__LOCALPUBKEY_multi) AS ?local_pubkey)`) and feed it to an action via
an ordinary mapping (e.g. derive's key parameter).

## Phase 3 design: the introductions view (concrete)

### The split

`ProfileIntroItem` today is **[Recommended-Actions companion] + [the full
introductions table with per-row action plumbing]**. Phase 3 turns **both** into
views and removes `ProfileIntroItem` entirely:

- The **table** (date / location / keys / np + per-row `retract`/`derive`)
  becomes a proper **view**, shared by owner *and* non-owner — superseding the
  read-only `introductions-view` (`RAElH_0Za…`, query `get-user-introductions`
  `RAJTZRxP…`, keyed on `user`, no actions today).
- The **Recommended-Actions companion** becomes a second **multi-row view**: one
  row per applicable recommendation, owner-gated via `?_CURRENTUSER = ?_user_iri`
  (see below). No custom Java remains.

### The recommended-actions view (multi-row)

A list/item-list view keyed on `?_user_iri` + `?__CURRENTUSER_multi_iri` + `?__LOCALPUBKEY_multi`,
emitting **one row per applicable recommendation** via a `UNION` of conditional
branches — each branch produces a row only when its condition holds:

| Recommendation row | Condition (all also require `?_CURRENTUSER = ?_user_iri`) | Row's action |
| --- | --- | --- |
| Create an introduction | `localCount = 0` | Create Introduction (entry action) |
| Retract redundant intros | `localCount > 1` | — (the retract buttons live in the table) |
| Get your intro approved | `!approved && localCount = 1` | link to the intro / `/userlist` |
| (key not in any intro / approved status) | informational | — |

Zero applicable → zero rows → the section is empty/hidden. A **non-owner** fails
`?_CURRENTUSER = ?_user_iri` on every branch → zero rows → nothing shown (the
read-only intro table is enough). The owner gate **must** be `CURRENTUSER` here,
not `LOCALPUBKEY` key-match: the "create" case has no intro yet, so there is no
key to match — a non-owner would otherwise see "create an introduction" on the
page (their own `localCount` is also 0).

Each actionable row carries its action as an **entry action** with target columns
(empty-into-required gates the button); e.g. Create Introduction maps the intro
template's `public-key`/`key-declaration`/`key-location` from
`local_pubkey`/`local_pubkey_short`/`SITEURL` (all `param_`, so phase-2 **2b**
multiple-mappings, no fill-mode key). The approval/wording text is built in the
query (`IF`/`CONCAT`); approval status comes from the trust repo (queryable).

### The query (extends the real `get-user-introductions`)

Today it groups intros of `?_user_iri` into `?date ?location ?keys ?np`. The v2
version (verified live) adds an optional-multi `?__LOCALPUBKEY_multi` with its own
`values {}` block and derives the action flags/targets. Sketch (see the working
file for the exact text):

```sparql
select (max(?date0) as ?date) (sample(str(?keyLocation)) as ?location)
       (group_concat(distinct ?keyHash; separator=", ") as ?keys) ?np ("^" as ?np_label)
       (if(sum(?isLocal) > 0, "true", "") as ?declares_local_key)
       (if(sum(?isLocal) > 0 && ?localCount > 1, str(?np), "") as ?retract_target)
       (if(sum(?isLocal) = 0 && ?localCount = 0 && max(?lpkBound) > 0, str(?np), "") as ?derive_target)
where {
  values ?__LOCALPUBKEY_multi {}          # filled when present, empty (graceful) when absent
  # … the intro graph patterns, binding ?pubkey / ?keyHash per key declaration …
  bind(coalesce(if(str(?pubkey) = str(?__LOCALPUBKEY_multi), 1, 0), 0) as ?isLocal)
  bind(if(bound(?__LOCALPUBKEY_multi), 1, 0) as ?lpkBound)
  {                                        # subquery: how many of the user's intros declare the local key
    select (count(distinct ?lnp) as ?localCount) where {
      values ?__LOCALPUBKEY_multi {}       # needs its own values block (subquery scope)
      # … the user's intros / key declarations, binding ?lpubkey …
      filter(coalesce(str(?lpubkey) = str(?__LOCALPUBKEY_multi), false))
    }
  }
} group by ?np ?localCount order by desc(max(?date0))
```

Comparisons are `coalesce`-guarded, **not** `bound()`-tested, because the live
service text-substitutes single placeholders but binds the multi var via the
`values` block (and the subquery needs its **own** `values` block — outer VALUES
doesn't reach into it). When `?__LOCALPUBKEY_multi` is unbound (logged out / no
key) every flag column comes back empty — which, via the action mappings below,
hides both editable actions for non-owners and logged-out viewers without any
role check. **Verified:** absent → all intros, flags empty; present → `retract_target`
set on each intro declaring the local key (`localCount > 1`), `derive_target` set
only when the local key is declared in none of the user's intros.

### Owner-gating falls out of `LOCALPUBKEY` — no `gen:isVisibleTo` needed

A non-owner viewing user X's page is logged in with *their own* key, which X's
intros don't declare → `declares_local_key` false, `localCount` 0 → both targets
empty → buttons hidden. The owner's own key *does* match → shown for applicable
rows. So the magic-param flags gate **both** owner-ness and per-row applicability;
the role-gating `gen:isVisibleTo` is not used on these actions.

### View entry actions

| Action | Template | Mappings (every *required* target empty → button hidden) |
| --- | --- | --- |
| retract | `RA0QOsYN…` (retract) | `retract_target → nanopubToBeRetracted` (`param_`, required) |
| derive | `RAT8ayO6…` (intro) | `derive_target → derive-a` (fill-mode key, required) **+** `local_pubkey → public-key__.1` **+** `local_pubkey_short → key-declaration__.1` / `key-declaration-ref__.1` **+** `SITEURL → key-location__.1` |

`retract` shows only for the viewer's own redundant intros; `derive` only when the
viewer holds a local key not yet in any of the user's intros (its `derive_target`
*and* `local_pubkey` mappings must both be non-empty).

### What phase 2 must support (pinned by the above)

- **retract → phase-2 core only:** a single `param_` mapping, hidden when its
  value is empty and the placeholder is required. No 2b.
- **derive → 2b:** *multiple* mappings per action, one targeting a **non-`param_`
  fill-mode key** (`derive-a`); the action is hidden if **any** required mapped
  value is empty (so an empty `derive_target` *or* empty `local_pubkey` hides it).
- **Create Introduction → 2b:** an entry action on the "create" recommendation
  row, with *multiple* `param_` mappings (`local_pubkey → public-key`,
  `local_pubkey_short → key-declaration`/`-ref`, `SITEURL → key-location`); no
  fill-mode key. Hidden if any required mapping is empty (logged out → no
  `local_pubkey` → hidden). Same 2b machinery as derive.

So phase 2 = the **empty-into-required core** + **2b (multiple mappings,
non-`param_` targets)** — which covers retract, derive, and Create Introduction.
With both the table and the recommendations modelled as views, `ProfileIntroItem`
is removed entirely; nothing stays custom. (The one action that did not fit the
model, *include-keys*, was dropped — see "Dropped: include keys" below.)

A copy-maintenance note: with the recommendations modelled as a view, the advisory
wording lives in a query nanopub (`IF`/`CONCAT`), so tweaking it means republishing
the query — the accepted trade for going fully declarative.

## Touch points

| Change | File |
| --- | --- |
| Magic registry + `isMagic` / binding helper | `MagicQueryParams.java` |
| Call binding before fetch | `QueryResultTableBuilder.build()` (+ list / paragraph builders) |
| Suppress UI fields for magic params | `GrlcQuery.createParamFields` / `QueryParamField` |
| Empty-into-required hides entry action | `QueryResultTable` (entry-action loop), reading `Template` optionality |
| Multiple mappings per action + non-`param_` targets | `ViewActionMappings`, `View` / action model, `QueryResultTable`, `QueryResultTableBuilder` |

No new view-definition predicate is introduced: visibility rides on the
template's existing optionality. The placeholder conventions
(`QueryTemplate.getParamName` / `isMultiPlaceholder` / `isOptionalPlaceholder`)
are external (nanopub-java) and need no change.

## Phasing

1. **Magic-param binding + UI-field suppression.** ✅ Done — `MagicQueryParams`
   (registry `LOCALPUBKEY`/`SITEURL`, `isMagic`, request-thread `augment`), wired
   into the five view-builder constructors; `GrlcQuery.createParamFields` skips
   magic placeholders. Inert until a query declares one. Wire smoke-test ✅ done —
   grlc binds `?__LOCALPUBKEY_multi` from URL param `LOCALPUBKEY` (verified against
   `get-user-introductions` v2, present and absent).
2. **Empty-into-required hides entry-action buttons (+ 2b).** ✅ Done —
   `View` holds multiple mappings per action (`getTemplateQueryMappings`);
   `Template.isRequiredField` answers "is this placeholder required"; the shared
   `ViewActionMappings.applyEntryMappings` applies an action's mappings per row
   (`@target` → raw URL key for fill-mode keys like `@derive-a`, else
   `param_target`) and hides the button when any *required* mapped value is empty.
   Wired into `QueryResultTable` and `QueryResultList` entry-action loops. Inert
   until a published view declares such mappings (phase 3). *Validate in phase 3:*
   `isRequiredField` against the real retract/intro templates.
3. **Republish the introductions view(s)** with the magic queries and the
   retract/derive entry actions, plus the multi-row recommended-actions view;
   remove `ProfileIntroItem` entirely. ✅ Done — live: the `get-user-introductions`
   query (with the strict local-key definition described below) + its view, and the
   recommended-actions query + view; `ProfileIntroItem` (and its
   `ProfileImageItem`/`ProfileLicenseItem`/`PubkeyItem` siblings) deleted.
4. ~~**Multi-expand + include-keys**~~ — **dropped** (decided not needed,
   2026-06-09). See "Dropped: include keys" below.

## Wire round-trip: verified

The full wire round-trip is **confirmed** against `query.knowledgepixels.com`
using `get-user-introductions` v2 (signed, passed inline via `_nanopub_trig`):

- **Absent** (`user=…`, no `LOCALPUBKEY`): HTTP 200, all intros returned, every
  flag column empty — the empty `values ?__LOCALPUBKEY_multi {}` block degrades
  gracefully (no "missing non-optional placeholder" error).
- **Present** (`user=…&LOCALPUBKEY=<pubkey>`): HTTP 200; `declares_local_key="true"`
  and `retract_target=<np>` exactly on intros whose key set includes the local
  key's hash; intros declaring only other keys come back with empty flags.

Note the corrections this surfaced vs. the in-process nanopub-java conventions:
single placeholders are text-substituted (so `bound()` breaks), `_multi_val` is
not recognized, and a multi placeholder needs an author-written `values {}` block
(one per subquery scope). All captured in the wire caveat above.

## Retract/derive gating: match the UI's notion of a "local introduction"

The first published intro query computed `retract_target`/`derive_target` off
whether the local key was merely **declared** in an introduction. That is looser
than the old `ProfileIntroItem`, whose `isIntroWithLocalKey`
(`NanodashSession.java`) — the canonical definition — requires **all three**:

1. the introduction is **signed by** the local key (`el.getPublicKeyString()`),
2. its local-key declaration has **no key-location, or one matching the site URL**
   (with the legacy `nanobench`→`nanodash` rewrite), and
3. it **declares** the local key.

Declaration alone over-counts: a user's local key can appear as a *declared* key
in introductions signed by *other* keys or located at *other* sites (common when
one ORCID bundles several keys, or the same key is reused across sites). Offering
"retract" on an introduction you didn't sign is also pointless — your retraction
wouldn't validate against it.

`get-user-introductions` **v6**
(`RA00h6v4MM0fU55lHYlUsGCQBKOmT1AC5v7BXlUHo-d3k`, view v8
`RAKCj5_P_w1r4mzaoaR5XVRjyLafPuFt034b7UK5Ve-H0`) ports the strict definition into
SPARQL:

- per row, `?signedByLocal = (str(?introPubkey) = str(?__LOCALPUBKEY_multi))`
  (`?introPubkey` is the signing key from `npa:hasValidSignatureForPublicKey`),
  and `?localDeclOk = ?isLocal && (!bound(?keyLocation) || keyLocation = SITEURL ||
  replace(keyLocation,"nanobench","nanodash") = SITEURL)`;
- a row is a *local introduction* iff `max(?signedByLocal) > 0 && sum(?localDeclOk)
  > 0`; `retract_target` fires on it when `?localCount > 1`, `derive_target` on a
  non-local row when `?localCount = 0`;
- the `?localCount` subquery counts distinct introductions under the **same**
  three-part definition (signed-by + declares + location-ok), each test
  `coalesce(...,false)`-guarded so it degrades to 0 when `LOCALPUBKEY` is absent.

Wire-verified against the live type-repo (Tobias, `SITEURL=localhost:37373` and
`=nanodash.knowledgepixels.com`): retract dropped from **8** (declares-only) to
**2** (the introductions actually signed by + declaring the local key); derive
stayed 0. Owner gate unchanged.

## Dropped: "include keys"

**Decision (2026-06-09): include keys is not needed — dropped, not built.** It was
the one `ProfileIntroItem` action that couldn't be expressed as a view action; rather
than build the indexed-expansion infra for it, we removed it. The per-row
"include keys…" button and its recommended-action bullet were deleted from
`ProfileIntroItem` (the legacy `/profile` component), and it is absent from the
migrated About page. The remaining recommended actions (create, derive, retract,
get-approval, update-approved) all carry over.

**What it did.** When you had **exactly one** local introduction (`localCount ==
1`), each *other* introduction that declared key(s) your local introduction lacked
got an "include keys…" button. It opened the intro template with `supersede=<your
local intro>` plus an indexed quad of params per missing key, superseding your
local introduction to add those keys.

**Why it didn't fit.** The view-action mapping maps **one** query column to **one**
template param. "Include keys" needs a **variable number** of *indexed* params
filled from a **set** of missing keys, plus a `supersede` URL key — neither is
expressible in the current model. Building that indexed-expansion infra for a
single, low-value action wasn't worth it, so the action was dropped rather than
deferred.
