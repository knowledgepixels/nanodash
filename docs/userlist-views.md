# /userlist views — draft queries to publish

Goal: turn the three remaining hard-coded lists on `UserListPage` (👤 Human Users,
🤖 Software Agents, ❓ Non-Approved Users) into proper views, the same way
`topcreators` and `latestusers` already are.

This needs, per list, **(1) a published grlc query nanopub** (SPARQL below) and
**(2) a published View nanopub** (`gen:ItemListView`) referencing it. Then the Java
in `UserListPage` just needs the three `*_VIEW` constants set to the published View
nanopub IDs.

## Published (live, 2026-05-27)

Current (v2) IRIs — the v1 set was superseded to fix duplicate rows for users
with multiple `foaf:name` values (now `group by ?user_iri` + `sample(?name)`):

| List | Query nanopub (current) | View resource IRI (in `UserListPage`) |
|---|---|---|
| 👤 Human Users | `RAN3bBS3Hbeom7JNYBhKtGc-4RjkGYHBBHlnRAaGPvl7k/get-approved-human-users` | `RAeDwLoelA43CfcetS7LVQOAgQuDCw-Yf5naRYdmCmCXs/human-users-view` |
| 🤖 Software Agents | `RAFMW55TSw7rwAi-mLRsgnhOugd7ZYf4L5zvzFeoU9OWQ/get-approved-software-agents` | `RAr4qrDh77rNoRcodAoGDOJLEECu3sBvrUJPAhuK73e1c/software-agents-view` |
| ❓ Non-Approved Users | `RAOnj3-qfGOYazNRGoOiAZRck3PxGT8zJMzucI4DUGQU0/get-non-approved-users` | `RA8Xkr-SnsRqu0RBGExZ3Ms8J1TviL_1bRQVRnymaWafw/non-approved-users-view` |

All under the `https://w3id.org/np/` prefix. The view-kind IRIs (stable across
supersedes, used in `dct:isVersionOf`) remain under the v1 nanopubs
(`RAWM…/human-users-view-kind`, `RAe87…/software-agents-view-kind`,
`RA2p4…/non-approved-users-view-kind`). Signed TriG sources are in `../nanopub-skill/tmp/`.

The SPARQL below reflects the published v2 queries (one row per user).

## How approval/software become queryable

The blocker that previously forced these client-side — approval living only in the
registry `list.json` joined in `UserData` — is gone. The query service `trust` repo
exposes the current trust state as `npa:AccountState` rows (`npa:agent`, `npa:pubkey`
= sha256 hash, `npa:trustStatus`). `trustStatus = npa:loaded` ≈ approved. `isSoftware`
comes from `?user a npx:SoftwareAgent` in the intro nanopubs (IntroNanopub type repo).
All three queries below were validated live against query.knowledgepixels.com (~0.6s each).

Endpoints (grlc `endpoint`):
- IntroNanopub type repo: `https://w3id.org/np/l/nanopub-query-1.1/repo/type/77757cabf6184c51c20b8b0fe5dc5e1365b7f628448335184ad54319a0affdfc`
- Trust repo (federated via `SERVICE`): `https://w3id.org/np/l/nanopub-query-1.1/repo/trust`

The renderer (`QueryResultItemList`) keys off a column ending in `user_iri` (icon +
`UserPage` link; bot icon when the user is a `SoftwareAgent`). The `user_iri_label`
column is used as the link text, falling back to `User.getShortDisplayName` when blank.

---

## Query 1 — 👤 Human Users (approved, not software)

Endpoint: **trust repo** (drive from the small ~635-row approved set; `SERVICE` into intros).
Suggested `rdfs:label` / view `dct:title`: `👤 Human Users`

```sparql
prefix np: <http://www.nanopub.org/nschema#>
prefix npx: <http://purl.org/nanopub/x/>
prefix npa: <http://purl.org/nanopub/admin/>
prefix foaf: <http://xmlns.com/foaf/0.1/>

select ?user_iri (sample(?name) as ?user_iri_label) where {
  graph npa:graph { npa:thisRepo npa:hasCurrentTrustState ?g . }
  graph ?g { ?acct a npa:AccountState ; npa:agent ?user_iri ; npa:trustStatus npa:loaded . }
  service <https://w3id.org/np/l/nanopub-query-1.1/repo/type/77757cabf6184c51c20b8b0fe5dc5e1365b7f628448335184ad54319a0affdfc> {
    graph ?a { ?kd npx:declaredBy ?user_iri . }
    filter not exists { graph ?a2 { ?user_iri a npx:SoftwareAgent } }
    optional { graph ?a { ?user_iri foaf:name ?name } }
  }
} group by ?user_iri order by lcase(str(sample(?name)))
```

## Query 2 — 🤖 Software Agents (approved, software)

Identical to Query 1 but flip the software filter to `filter exists`.
Suggested label/title: `🤖 Software Agents`

```sparql
prefix np: <http://www.nanopub.org/nschema#>
prefix npx: <http://purl.org/nanopub/x/>
prefix npa: <http://purl.org/nanopub/admin/>
prefix foaf: <http://xmlns.com/foaf/0.1/>

select ?user_iri (sample(?name) as ?user_iri_label) where {
  graph npa:graph { npa:thisRepo npa:hasCurrentTrustState ?g . }
  graph ?g { ?acct a npa:AccountState ; npa:agent ?user_iri ; npa:trustStatus npa:loaded . }
  service <https://w3id.org/np/l/nanopub-query-1.1/repo/type/77757cabf6184c51c20b8b0fe5dc5e1365b7f628448335184ad54319a0affdfc> {
    graph ?a { ?kd npx:declaredBy ?user_iri . }
    filter exists { graph ?a2 { ?user_iri a npx:SoftwareAgent } }
    optional { graph ?a { ?user_iri foaf:name ?name } }
  }
} group by ?user_iri order by lcase(str(sample(?name)))
```

## Query 3 — ❓ Non-Approved Users (have an intro, no loaded account)

Endpoint: **IntroNanopub type repo** (drive from intros; `MINUS` the approved agents from trust).
Suggested label/title: `❓ Non-Approved Users`

```sparql
prefix np: <http://www.nanopub.org/nschema#>
prefix npx: <http://purl.org/nanopub/x/>
prefix npa: <http://purl.org/nanopub/admin/>
prefix foaf: <http://xmlns.com/foaf/0.1/>

select distinct ?user_iri (?name as ?user_iri_label) where {
  graph npa:graph {
    ?intronp npa:hasValidSignatureForPublicKey ?pk ; np:hasAssertion ?a .
    filter not exists { ?x npx:invalidates ?intronp ; npa:hasValidSignatureForPublicKey ?pk . }
  }
  graph ?a {
    ?kd npx:declaredBy ?user_iri .
    optional { ?user_iri foaf:name ?name }
  }
  minus {
    service <https://w3id.org/np/l/nanopub-query-1.1/repo/trust> {
      graph npa:graph { npa:thisRepo npa:hasCurrentTrustState ?g . }
      graph ?g { ?acct npa:agent ?user_iri ; npa:trustStatus npa:loaded . }
    }
  }
} group by ?user_iri order by lcase(str(sample(?name)))
```

---

## View nanopub shape (one per query, modeled on `latest-users`)

```turtle
sub:human-users a gen:ItemListView, gen:ResourceView ;
    dct:isVersionOf sub:human-users-kind ;
    dct:title "👤 Human Users" ;
    rdfs:label "Human Users View" ;
    gen:hasViewQuery <.../RA…#human-users-query> ;
    gen:hasViewQueryTargetField "resource" .
```

`UserListPage` calls the item-list builder directly, so the view type drives
nothing in code — but type it `gen:ItemListView` for correctness/discoverability.
The queries take no per-resource parameter (global lists), so `hasViewQueryTargetField`
is irrelevant here; the `QueryRef` is built with no params.

## Open decisions / caveats

- **Approval semantics:** `trustStatus = npa:loaded` is treated as approved. Decide how
  `contested` (4) / `skipped` (3) should map. The registry `list.json` (current client
  source) and the query `trust` repo derive from the same trust state but are separate
  snapshots — minor drift possible.
- **Sort parity:** the old lists sort by `User.getShortDisplayName`; these sort by
  `foaf:name` server-side (users without a name sort first and render via
  `getShortDisplayName`). Close but not identical ordering.
- **Per-key vs per-agent approval:** Non-Approved is computed per *agent* (no loaded
  account at all). The old client logic is per (agent, pubkey-hash); a user with both an
  approved and an unapproved key could differ at the edges. Validate against the live
  list before publishing.
- **Display width:** the three lists sit in `col-6` wrappers in `UserListPage.html`;
  check the rendered width once live and tune via the view's `gen:hasDisplayWidth` or
  `ViewDisplay.withDisplayWidth(...)` if needed.
