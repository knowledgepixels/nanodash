package com.knowledgepixels.nanodash;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for accessing and managing API queries.
 * Provides methods to retrieve query results, manage query IDs, and fetch the latest versions of nanopublications.
 */
public class QueryApiAccess {

    private QueryApiAccess() {
    }  // no instances allowed

    // Query IDs (full id = RA.../query-name)
    public static final String GET_LATEST_NANOPUBS_FROM_PUBKEYS = "RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys";
    public static final String GET_LATEST_NANOPUBS_FROM_USERID = "RAuy4N1h4vZ1wgBUMvTiWw2y_Y0_5oFYRTwdq-xj2qqNM/get-latest-nanopubs-from-userid";
    public static final String GET_LATEST_USERS = "RAr27GmRUKQmvPbfmB34N9l9lX-xYK7nQhvOMbQCk3byI/get-latest-users";
    public static final String GET_MOST_RECENT_NANOPUBS = "RAYNg6rfvXIVvJY2u8oS0EEjxnVvimLLVZG1rOar_nWIY/get-most-recent-nanopubs";
    public static final String GET_PUBLISHER_VERSION = "RAPGhXDRzeGu-Qk0AkjleEtxMxqAvJ-dZn7985gzAbyhs/get-publisher-version";
    public static final String GET_LATEST_VERSION_OF_NP = "RAiRsB2YywxjsBMkVRTREJBooXhf2ZOHoUs5lxciEl37I/get-latest-version-of-np";
    // Minimal single-lookup probe for whether a given nanopub has been loaded by the
    // answering Nanopub Query instance; used to time post-publish refreshes (issue #629).
    public static final String CHECK_NANOPUB_LOADED = "RAxqXyhP1fnjvDdX-K0z9TgnwoXf462FxV1wEAWRm_gos/check-nanopub-loaded";
    public static final String GET_ALL_USER_INTROS = "RAjHh6P11QFUaoPiMRBavdAnTq4YMJW4PB85oVFSBfYjU/get-all-user-intros";
    public static final String GET_ALL_USER_PROFILE_PICS = "RAtcodMPmTrmBvdOqwYIrNNFDO74f8B_xo0qsOcKlCwTA/get-all-user-profile-pics";
    // Profile pictures of spaces and maintained resources (issue #632), declared as
    // schema:image on the resource IRI. Unlike the user pictures above -- self-declared and
    // read from a subject-agnostic query -- these are gated on the declaring nanopub being
    // signed by a current admin of the governing space ref, so a third party cannot set a
    // space's picture. Ordered newest first; the first row wins.
    public static final String GET_RESOURCE_PROFILE_PICTURE = "RALK8_WQPtAbMUv2IvHeZyUU1WjD77V4a3hb0KsiZI0tI/get-resource-profile-picture";
    public static final String GET_ALL_USER_DEFAULT_LICENSE = "RA-_IwzReR2_HfTLz4YcNM6Mh3Vt16y0RUS12tpJTN9FI/get-all-user-default-license";
    public static final String GET_MONTHLY_TYPE_OVERVIEW_BY_PUBKEYS = "RAhI-C2KsqS_IvnxwyBrbMFsoj65dhLWE_CBo_KtcVEVA/get-monthly-type-overview-by-pubkeys";
    public static final String GET_INTRODUCING_NANOPUB = "RALZXWg5lZoJoQ0VHL5mpDgNxYpqU6FoDLWGp4rs8A6b8/get-introducing-nanopub";
    public static final String FULLTEXT_SEARCH = "RAxdh5xkc6K6SMLY23yKu__zTWJPXeRFc0qgNNxkbOkpY/fulltext-search";
    public static final String FIND_THINGS = "RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things";
    public static final String GET_NEWER_VERSIONS_OF_NP = "RAqmmNSxQaRNWRYH0o4Da3GSOwvoFLObhXfAGUCOqEtfw/get-newer-versions-of-np";
    public static final String GET_LATEST_THING_NANOPUB = "RAzXDzCHoZmJITgYYquLwDDkSyNf3eKKQz9NfQPYB1cyE/get-latest-thing-nanopub";
    // Node-anchored variants (label/tag/unlisted read off the typed template node, so
    // templates with embedded identity list correctly); derived from, not superseding,
    // the RA6bgrU3/RA4bt3MQ/RAMcdiJp originals, which are update-locked to another key.
    // v3 dedups governed version pairs: a version declaring dct:isVersionOf +
    // gen:governedBy is listed only if it is its (kind, space) pair's current governed
    // winner (in-SPARQL Option A of the listing-dedup problem; the nanopub-query#138
    // canonical-version edge would replace these arms).
    // v4 (RAi6EPio/RA4ynLpm/RAxzYV8P, superseding RAoEo6jL/RAl2C9PT/RAGxzVO9) keeps the same
    // semantics but materializes the governed winners once, in a single-row group_concat
    // sub-select that shares no variables with the outer query, and tests membership by string
    // containment. The v3 arms joined that sub-select under an optional: correct only because
    // RDF4J's left join is non-standard (under strict SPARQL semantics the unbound cross-join
    // would drop every non-governed template), and at risk of re-evaluating the federated
    // spaces-repo SERVICE per row. Verified identical row counts (656/26/38) against v3.
    public static final String GET_ASSERTION_TEMPLATES = "RAi6EPio6sbvJ06mqfYm_QBmisWQnJ8cvzm-DKRHKPGUg/get-assertion-templates";
    public static final String GET_PROVENANCE_TEMPLATES = "RA4ynLpmZXQjnMQzvm7OPt-q8uPPXU8qMxSHm4oSxlw5Y/get-provenance-templates";
    public static final String GET_PUBINFO_TEMPLATES = "RAxzYV8Pr9vgTcajVMKrZ4GRO8xjxYgEzHCLN_BE0FQfs/get-pubinfo-templates";
    public static final String GET_LATEST_ACCEPTED_BDJ = "RAkoDiXZG_CYt978-dZ_vffK-UTbN6e1bmtFy6qdmFzC4/get-latest-accepted-bdj";
    public static final String GET_LATEST_BIODIV_CANDIDATES = "RAgnLJH8kcI_e488VdoyQ0g3-wcumj4mSiusxPmeAYsSI/get-latest-biodiv-candidates";
    public static final String GET_LATEST_ACCEPTED_DS = "RATpsBysLf8yXeMpY7PHKj-aKNCa4-4Okg1hi97OLDXIo/get-latest-accepted-ds";
    public static final String GET_LATEST_DS_CANDIDATES = "RAFNTW3jhWKnNvhMSOfYvG53ZAurxrFv_-vnIJkZyfAuo/get-latest-ds-candidates";
    public static final String GET_DS_REACTIONS = "RA0FiH8gukovvEHPBMn72zUDdMQylQmUwtIGNLYBZXGfk/get-ds-reactions";
    public static final String GET_LATEST_ACCEPTED_RIO = "RAAXmnJdXHO86GqJs8VTdqapUWqCrHKRgRT2b4NfjAfgk/get-latest-accepted-rio";
    public static final String GET_LATEST_RIO_CANDIDATES = "RAehKOCOnZ3uDBmI0kkCNTh5k9Nl6YYNj7tyc20tVymxY/get-latest-rio-candidates";
    public static final String GET_REACTIONS = "RAe7k3L0oElPOrFoUMkUhqU9dGUqfBaUSw3cVplOUn3Fk/get-reactions";
    public static final String GET_TERM_DEFINITIONS = "RAZUsK7jU85oUYEVKvMPFlqbwn19oR55IQuFkXuiS_Tkg/get-term-definitions";
    // v10 (issue #302): standalone + preset-supplied views (unbound ?display), gated to
    // admins/maintainers of the owning space or the affected user themselves. Each
    // referenced view is resolved to its latest version server-side: the version tree's
    // most recent current head (a nanopub itself neither superseded nor validly retracted
    // via npx:invalidates), robust to backdated supersedes and retracted versions, so
    // ?view is already the latest and needs no separate per-view lookup. v10 wraps that
    // resolution in a run-once sub-SELECT so the cross-repo lookup federates once for the
    // whole view set instead of once per referenced view -- cut a 44-display page from
    // ~4.5s to ~1.7s (the per-view federation round-trips were the dominant cost).
    // RAlOsra- (supersedes the broken RAyXaBuR, which had a double-slash op IRI from a trailing
    // slash on the sub: prefix) gates the view-version-resolution npx:invalidates filters on the
    // version nanopub's own signing pubkey (issue #487); no-regression verified across resources
    // incl. a 45-display user page.
    // RA4TGV_z (supersedes RAd105Rj) replaces the bare-IRI isMaintainedBy? authority hop and the
    // dead RoleDeclaration maintainer arm with a single npa:hasGoverningSpaceRef gate keyed on the
    // materialized npa:hasRoleType (nanopub-query#130 / issue #510); fixes maintainer visibility and
    // cross-ref bleed. No-regression verified against RAd105Rj across resources.
    // RArKslem (supersedes RA4TGV_z) adds space-governed version resolution (gen:governedBy;
    // docs/views-and-presets-as-maintained-resources.md): a referenced version declaring a
    // governing space resolves to the newest member+-signed version of its (kind, space)
    // pair via a run-once governed sub-select, falling back to the pinned version.
    // RAWlJqJ5 (supersedes RArKslem): same semantics, endpoint rebased from the ViewDisplay
    // type repo onto repo/full, cutting the query from 5 SERVICE clauses to 2 (only the
    // repo/spaces state lookups remain federated). The per-hop federation was the main
    // amplifier of the rdf4j connection-pool deadlock behind the fleet-wide /api wedges
    // (nanopub-query, 2026-08-20 thread dump: 22 of 28 pool-blocked threads were this
    // query). Validated byte-identical across standalone/preset/governed/self-page cases.
    // RA3ekD-2 (supersedes RAWlJqJ5) adds the pending-account self-arm (issue #625 /
    // nanopub-query#195): the own-page branch of the authority gate additionally accepts
    // npa:PendingAccountState rows (mirrored from authoritative introductions of users not
    // trust-approved yet), so such a user's own page shows the view displays they signed
    // themselves. Display-only: the pending class is distinct from npa:AccountState, so the
    // admin/maintainer arm and the governed-version resolution keep requiring approved
    // accounts. Validated byte-identical to RAWlJqJ5 across approved resources.
    // RAwkiytr (supersedes RA3ekD-2) adds ?presetKind to the preset branch (issue #607): the
    // assigned preset's stable kind (dct:isVersionOf, falling back to the version IRI), so the
    // client keeps only the newest assignment per (preset kind, resource) -- the identity view
    // displays already have via view kind. Purely additive; every other column is unchanged
    // (validated identical across standalone, preset, governed, self-page and maintained-resource
    // cases).
    // RAvPNGog (supersedes RAwkiytr) is a performance rewrite with identical results (validated
    // across 40 resources): the authority gate becomes a single-row key string tested with
    // contains(), since joining its ?pubkey rows let rdf4j start from the signing keys and walk
    // every nanopub they signed; the latest-version resolution becomes a bound lookup per
    // referenced view instead of a run-once sub-select over every view in the repository; and
    // the governed winners are computed once as a single string instead of a per-row
    // SERVICE sub-select. ~0.3-0.8s instead of 3.5-12s, and 502s at the 60s cap under load.
    public static final String GET_VIEW_DISPLAYS = "RAvPNGogj0GqBiBYBxqvympNd_RHXoxMHKs19bBymAelo/get-view-displays";
    // Test head for dropping the server-side view-version resolution (its run-once resolution
    // sub-select was the query's dominant cost, linear in the repo-wide view count; see
    // nanopub-query doc/design-view-head-materialization.md): ref-scoped like the retired
    // GET_VIEW_DISPLAYS_REF (RActfK6C) it replaced -- same inputs (resource + root_np) and the
    // same columns plus ?viewKind and ?governedBySpace -- but ?view carries the referenced
    // version UNRESOLVED — except for gen:governedBy pins, where it is the space-governed
    // resolution. Supersedes-head resolution happens caller-side per view (View.get with
    // resolveLatest=true, memoized), which also covers the governed case, so the extra columns
    // need not be consulted.
    // RAkIkmSi (supersedes RAXdRFNL): endpoint rebased onto repo/full (5 SERVICE -> 2, see
    // GET_VIEW_DISPLAYS above); in particular the per-referenced-view pin lookups are now
    // local joins instead of one federated round-trip per view under a nested-loop join.
    // RAt7dfZO (supersedes RAkIkmSi) adds the same ?presetKind column as GET_VIEW_DISPLAYS
    // (issue #607); no other change.
    // RA1Pm-iK (supersedes RAt7dfZO) applies the same authority-gate and governed-winner
    // rewrites as GET_VIEW_DISPLAYS above; results identical across 35 space/root pairs.
    public static final String GET_VIEW_DISPLAYS_UNRESOLVED = "RA1Pm-iKXwZRA0Hl9eDXHts2JBhseuZQJDci9VvixqylA/get-view-displays-unresolved";

    // Spaces-repo queries (endpoint: nanopub-query .../repo/spaces)
    // v3: ref-aware get-spaces (adds ?ref + ?root so the client can key one space per
    // ref). Published as an independent nanopub (no npx:supersedes). Active query used by
    // SpaceRepository. Source at docs/queries/get-spaces-ref.trig. See
    // docs/space-ref-identity.md.
    // v4 (RAyXmrfs, supersedes RAD5KmWO) gates the npx:invalidates filter on a shared signing
    // pubkey between invalidator and the space-definition nanopub (issue #487).
    // RALHRpoL (supersedes RAyXmrfs) is that same query re-published with correct provenance:
    // v4 went out signed by the placeholder orcid:0000-0000-0000-0000 and without
    // nt:wasCreatedFrom*Template links. The SPARQL is byte-identical (296 rows either way).
    public static final String GET_SPACES_REF = "RALHRpoLzFDvEPk_9MWKk4IkQ_BC841f8cs0uXtQnVU4w/get-spaces";
    // Disambiguation claimants: one row per ref (root definition) claiming a space IRI, with that
    // ref's validated admins (admins_multi_iri). Pass the space IRI; replaces the per-ref
    // get-space-admins fan-out with a single fetch. Which ref is the representative (default) is
    // decided client-side. Source at docs/queries/list-space-claimants.trig.
    // RAYU2MLE (supersedes RApsQhJn) is that same query re-published with correct provenance
    // (the original was signed by the placeholder orcid:0000-0000-0000-0000 and carried no
    // nt:wasCreatedFrom*Template links); the SPARQL is byte-identical.
    public static final String LIST_SPACE_CLAIMANTS = "RAYU2MLEEkLhSfkRYbE9olhuBq19e6g3QY-jfJTmqh0OI/list-space-claimants";
    // Space-governed view-version resolution: given a definition kind (dct:isVersionOf
    // target), its governing space and the pinned version's nanopub (pin), returns at most
    // one row. With the kind validated as maintained by the space: the newest version
    // declaring gen:governedBy that space, signed by a current member+ of the space's
    // governing ref; empty result = the caller keeps its pinned version (the pin is the
    // floor). With the kind not maintained by the space: the single current head of the pin's own
    // same-key supersedes chain, as GET_LATEST_VERSION_OF_NP resolves it, so that a
    // gen:governedBy declared before the space maintains the kind no longer freezes the pin.
    // RAyB49tP supersedes RA833rrc, which superseded RAPSWgzH: RAPSWgzH took no pin and
    // returned nothing for a kind the space doesn't maintain, and both it and RA833rrc looked up the
    // versions once per member row, starting from every nanopub signed by a member key
    // (up to a minute for spaces whose members have signed many nanopubs). RAyB49tP
    // gathers the member keys into one row and starts the full-repo lookup from the kind,
    // so it runs once; results validated identical across all governed pairs, maintained-kind
    // median 0.5s -> 0.2s, worst case 20-60s -> about 1s. Source at
    // docs/queries/get-latest-governed-version.trig; see
    // docs/views-and-presets-as-maintained-resources.md.
    public static final String GET_LATEST_GOVERNED_VERSION = "RAyB49tPLdgMjwmG9alOjtK2wSvxLGPunfncFk57elB-Q/get-latest-governed-version";
    public static final String GET_SUB_SPACE_LINKS = "RAWgoQbP9_B9h3Bnwd1FGYX1gLYPyZFOxaeqIeA3TTPSU/get-sub-space-links";
    public static final String GET_MAINTAINED_RESOURCES = "RAOOq81R84exTUKUBQT3BbgCaSJyC2lqPDXIP2XaDTosM/get-maintained-resources";
    public static final String GET_SPACE_ADMINS = "RAaHOXMQ7Kq37T9syR9at0RqushclHenlPOFRwFDn0Cfs/get-space-admins";
    // Ref-scoped admins (Stage 2): takes the ref's root nanopub (root_np), matches admins
    // on npa:forSpaceRef, so multi-ref spaces don't merge admin sets across refs. Published
    // independently. Source at docs/queries/get-space-admins-ref.trig. See
    // docs/space-ref-identity.md.
    public static final String GET_SPACE_ADMINS_REF = "RAWM8qlKbV3DEH_NsPJ6hIyTrBwIp8sNeg9MGDgu8la1o/get-space-admins";
    public static final String GET_SPACE_ADMIN_PUBKEY_HASHES = "RAJvvNY6KXqveJivZKh-chTCntrsY_KJSGLVNRQdi0pUc/get-space-admin-pubkey-hashes";
    // Ref-scoped admin pubkey hashes (Stage 2): takes the ref's root nanopub (root_np),
    // matches admins on npa:forSpaceRef, so multi-ref spaces don't merge admin keys across
    // refs. Published independently. Source at docs/queries/get-space-admin-pubkey-hashes-ref.trig.
    public static final String GET_SPACE_ADMIN_PUBKEY_HASHES_REF = "RAO8KDdS4_Z0-R1qCSKqWcewg0WUSaiQDh_p1N1Bg-zic/get-space-admin-pubkey-hashes";
    // 2026-09-11: the six role-listing queries below (get-space-roles, get-space-roles-ref, list-space-observers-ref,
    // list-space-non-approved-ref, list-space-members-ref, list-space-roles-ref) were superseded to read the role name via
    // schema:name in BOTH schemes (http://schema.org/ and https://schema.org/): nanopub-java >= 1.93.0 blacklists the
    // http form, so new roles carry https://schema.org/name and showed up as a bare "role" in the About tab.
    public static final String GET_SPACE_ROLES = "RAr9zGmPYtJwRK2m0pOwGrhsfhS7i3bqog27mILED6wjc/get-space-roles";
    // Ref-scoped roles (Stage 2): takes the ref's root nanopub (root_np), matches
    // RoleAssignments on npa:forSpaceRef, so multi-ref spaces don't merge role sets across
    // refs. Published independently. Source at docs/queries/get-space-roles-ref.trig.
    public static final String GET_SPACE_ROLES_REF = "RATwohQqQwgra4nu0CYEmJCpc-Xo1xz2xb1IVc544D28U/get-space-roles";
    public static final String GET_SPACE_MEMBERS = "RAo0c4UNoD-uTP3xATU_-TB6vO-nMO4Ya-mvdaGjX5qVE/get-space-members";
    // Ref-scoped members (Stage 2): takes the ref's root nanopub (root_np), resolves the
    // ref + its space IRI, and returns ALL non-admin RoleInstantiations naming that IRI
    // (raw npa:spacesGraph, matching the looser pre-migration semantic), each with a
    // ?validated flag = whether it is also in the trust-state-validated current-state graph
    // (i.e. the agent's key has a trust-approved AccountState from an accepted intro). Shows
    // every self-declared member while flagging the un-introduced ones, rather than hiding
    // them. Published independently. Source at docs/queries/get-space-members-ref.trig.
    // RA2eGba0 (supersedes RAqp9TSM) gates the npx:invalidates filter on a shared signing pubkey
    // between invalidator and the member declaration (issue #487).
    // RAPYJ7HL (supersedes RA2eGba0) is that same query re-published with correct provenance
    // (RA2eGba0 was signed by the placeholder orcid:0000-0000-0000-0000 and carried no
    // nt:wasCreatedFrom*Template links); the SPARQL is byte-identical.
    public static final String GET_SPACE_MEMBERS_REF = "RAPYJ7HL7UiPQA1vHocyHYL99bKuaaKddfefrgW2zyA-Y/get-space-members";
    // Ref-scoped observers (Stage 2): takes the ref's root nanopub (root_np), lists observers
    // INCLUDING un-introduced self-declared ones (not in the validated state), each flagged
    // via a headerless ?unverified_noheader column (⚠️ when unvalidated). Drives the existing
    // Observers view's table (the view nanopub is left untouched). Published independently.
    // v3 (RAZ41V9K, supersedes RA58KSjh) (a) resolves owl:sameAs space aliases via the ref's
    // validated npa:sameAsSpace edges, so observer roles declared against an alias IRI of the
    // space are included, and (b) lists EVERY observer-tier association — no longer hiding users
    // who also hold a higher-tier (admin/maintainer/member) role, so an admin who is also a
    // participant appears here for that participant role. The built-in admin property and
    // genuine higher-tier role declarations are still excluded; non-approved higher-tier claims
    // go to LIST_SPACE_NON_APPROVED_REF. v4 (RAobkcQi, supersedes RAZ41V9K) gates the
    // npx:invalidates filter on a shared signing pubkey (npa:hasValidSignatureForPublicKeyHash)
    // between invalidator and target, so a foreign-key retraction can no longer hide an observer
    // (issue #487; mirrors the materializer's #112 same-publisher gate). v5 (RAUQdhb2, supersedes
    // RAobkcQi) fixes a regression introduced in v3: the space-alias resolution used a
    // `{ bind(?spaceIri as ?inSpace) } union { ... sameAsSpace ... }` pattern, but RDF4J does not
    // propagate the outer ?spaceIri into a BIND inside a UNION branch, leaving ?inSpace unbound so
    // the query returned ZERO observers for every space. Replaced with a non-union
    // `filter( ?inSpace = ?spaceIri || exists { ... sameAsSpace ... } )`. Source at
    // docs/queries/list-space-observers-ref-v5.trig.
    // Latest (RAoW4pMA, nanodash#498): a member is excluded from the observers list only when they
    // hold a VALIDATED higher tier, read directly off a gen:RoleInstantiation (npa:hasRoleType in
    // {AdminRole,MaintainerRole,MemberRole}) in the current space state — replacing the global
    // RoleDeclaration matching that mis-excluded observers whose predicate was declared at a higher
    // tier by another space (which had returned ZERO observers for spaces like vu/ucds). Enabled by
    // nanopub-query persisting tier on the instantiation (nanopub-query#125 + #127). RAZNHDFQ
    // (supersedes RAoW4pMA) drops the role-label coalesce to read schema:name only. Latest
    // (RAQylZL4, supersedes RAZNHDFQ) BUGFIX: the RAoW4pMA/#498 higher-tier exclusion dropped a
    // member from the observers list whenever they held ANY validated higher-tier role, so an
    // admin/maintainer/member who also holds a separate genuinely observer-tier role (e.g. an
    // admin who is also a planned attendant) was hidden from the observer list entirely — every
    // observer of a space whose observers are also its admins returned ZERO rows. The check is now
    // scoped to the SAME role property ((npa:regularProperty|npa:inverseProperty) ?roleProp on
    // ?vriH), so a higher tier held through a different property no longer suppresses the observer
    // association, while the #498 tier-collision fix (same property validated at a higher tier)
    // is preserved. RAt8PKQ2 (supersedes RARcL1s1, adding the hidden revokeAgent action-mapping column; RARcL1s1 superseded RAQylZL4) distinguishes pending accounts (issue #625 /
    // nanopub-query#195): an association validated only through a pending account — the
    // materialized RoleInstantiation carries npa:trustStatus npa:seen, produced by the
    // PendingAccountState self-arm for observer-tier self-signups of introduced-but-unapproved
    // users — shows ⏳ in the headerless flag column (empty = approved-validated, ⏳ =
    // pending-validated, ⚠️ = not validated at all). Latest (RANXPEIi, supersedes RAt8PKQ2) adds a
    // member_label column so each agent shows by name: the canonical foaf:name mirrored into the
    // current space state, falling back to the foaf:name asserted for that agent in the grant
    // nanopub's pubinfo. Most role holders have no key introduction of their own, so without the
    // fallback they rendered as bare ORCIDs; purely self-declared claims that carry no name
    // anywhere still show their IRI.
    public static final String LIST_SPACE_OBSERVERS_REF = "RAy7MVefpFPFBhxDBh5sFzw6S5H-gCH5yJ2dENH1ackKI/list-space-observers";

    // Ref-scoped non-approved role claims (root_np): agents holding a higher-tier role
    // instantiation (admin/maintainer/member) that is NOT in the validated state — a
    // self-assigned or otherwise ungranted claim awaiting approval by an equal-or-higher-tier
    // member. Observer-tier roles are excluded (self-assignable, so they need no approval and
    // are listed by LIST_SPACE_OBSERVERS_REF). Drives the "❓ Pending
    // Admins/Maintainers/Members" view. v3 (RA2BnCGv, supersedes RAZMAChi) resolves owl:sameAs
    // space aliases via the ref's validated npa:sameAsSpace edges, so a higher-tier claim made
    // against an alias IRI of the space is detected. v4 (RAwv7GRc, supersedes RA2BnCGv) gates the
    // npx:invalidates filter on a shared signing pubkey between invalidator and target, so a
    // foreign-key retraction can no longer suppress a pending claim (issue #487; mirrors the
    // materializer's #112 same-publisher gate). v5 (RAtSaYBH, supersedes RAwv7GRc) fixes the same
    // v3 regression as the observers query: the `{ bind(?spaceIri as ?inSpace) } union { ...
    // sameAsSpace ... }` alias pattern left ?inSpace unbound on RDF4J (BIND in a UNION branch does
    // not see the outer ?spaceIri), so the query returned ZERO rows for every space (no pending
    // claim could ever surface). Replaced with a non-union
    // `filter( ?inSpace = ?spaceIri || exists { ... sameAsSpace ... } )`. v6 (RAhSqdJ6, supersedes
    // RAtSaYBH via the RAPo1zp2 intermediate) adds an approve_np column with the granting
    // nanopub. Latest (RAVsaIwA, supersedes RAhSqdJ6; issue #603): the admin-only
    // roleAssignmentTemplate and agent_iri columns are replaced by a per-row
    // (approve_np, grant_template) pair — the grant nanopub and its own creation template
    // (nt:wasCreatedFromTemplate, free-form template as fallback) — so the view's approve
    // action can open the pending grant in derive mode for EVERY tier, not only for admin
    // grants. The tier column is deterministic (the highest tier among the member's pending
    // claims, rank-keyed min instead of a flapping sample), and the pair is computed from
    // the SAME grant via a rank-prefixed concatenated min preferring that highest tier, so
    // tier, derive target, and template always agree. A grant whose assertion defines a
    // Space itself (a competing root definition) is not offered for derivation; admin-tier
    // claims from such definitions pair with the built-in admin-assignment template instead
    // (the hasAdmin triple unifies), keeping the space-ref-conflict remedy.
    // Source at docs/queries/list-space-non-approved-ref-v7.trig. v8 (RAoX3Htu, supersedes
    // RAVsaIwA) adds a member_label column, sourced like the observers query above.
    public static final String LIST_SPACE_NON_APPROVED_REF = "RA8r_O22FL53frfrmudFKMB-g7S-H0HU78O7uDiOx4WZ4/list-space-non-approved";

    // Ref-scoped variants of three About-tab *view* display queries (distinct from the
    // GET_SPACE_*_REF client-authority queries above). Each takes the ref's root nanopub
    // (root_np), resolves the ref via npa:rootNanopub, and scopes by npa:forSpaceRef (members,
    // roles) or the ref-level npa:hasSubSpace edge (sub-spaces), so a ?root=-pinned space page
    // shows only that one ref's listings rather than merging all refs claiming the IRI. (The
    // maintained-resources listing did the same with the npa:hasMaintainedResource edge until
    // its view's own query took the space plus an optional root_np, so it is view-driven now.)
    // Column-compatible with the IRI-keyed view queries, so they drive the existing view
    // nanopubs unchanged (the observers pattern). Used by AboutSpacePanel with an IRI-keyed
    // fallback when the ref root is unknown. Published independently (no npx:supersedes).
    // Sources at docs/queries/list-*-ref.trig. See docs/space-ref-identity.md.
    // v3 (RApyKS9D): reads each membership's tier directly off the materialized gen:RoleInstantiation
    // (npa:hasRoleType) and its role (gen:hasRole) in the current space state, now that nanopub-query
    // persists tier on the instantiation (nanopub-query#125 + #127). Simplifies away the earlier
    // RoleAssignment-scoping workaround and the global RoleDeclaration matching that leaked observer-tier
    // members into the Approved listing. See nanodash#498. RAJ15No3 (supersedes RA7E54m5, adding the hidden revokeAgent action-mapping column; RA7E54m5 superseded RApyKS9D)
    // drops the role-label coalesce to read schema:name only. Latest (RA-90ZiE, supersedes RAJ15No3)
    // adds a member_label column, sourced like the observers query above, and orders rows by tier and
    // then by that display name.
    public static final String LIST_SPACE_MEMBERS_REF = "RAroCpts3CpuUpSsuPpccRbyKkwkOvVNQSjoY0ZYAVvBg/list-space-members";
    public static final String LIST_SPACE_ROLES_REF = "RAYOsITlBsY5vmlPmZuMnsJQvwIss9DfjdWW0VjgLkMjE/list-space-roles";
    public static final String LIST_SUB_SPACES_REF = "RA-j0DFqkNUHxF_WIds8wWJix6DkDFBmUBWmKXfG24XYQ/list-sub-spaces";

    // View-displays listing queries are no longer referenced here: the About-tab view-displays
    // tables are view-driven (gen:hasViewQuery on the space/user/maintained view nanopubs), and the
    // panels pass the resource + (for spaces/maintained) the ref's root nanopub as params. The
    // ref-scoped query requires root_np; the IRI-keyed variant backs the users' view. See
    // docs/queries/list-view-displays{,-ref}.trig.

    // The part view-displays listing is not referenced here either: AboutPartPanel takes it
    // from the part view nanopub's gen:hasViewQuery and passes resource + partid + partclass.

    // Ref-scoped preset-assignment listing (root_np): reads the server-materialised
    // npa:PresetAssignment rows scoped by npa:forSpaceRef from the validated current space-state
    // graph (nanopub-query #122 ref-stamps each admin-authored assignment per ref), with
    // latest-by-date-per-(preset,resource) + npa:isActivated gating (a deactivation is a newer
    // admin-authored row). Column-identical to the IRI-keyed list-preset-assignments, so it drives
    // the existing Preset assignments view unchanged. Used by AboutSpacePanel with an IRI-keyed
    // fallback when the ref root is unknown. Source at docs/queries/list-preset-assignments-ref.trig.
    // RArC6iR- (supersedes RA3zdn0g, itself the head this constant had been left behind by)
    // keys an assignment on the preset's stable kind rather than the pinned version, and adds the
    // version columns behind the view's "update to latest version" action (issue #607).
    // RArXnUQf (supersedes RArC6iR-) replaces the earlier blank-header notice with a proper
    // "version" column: version_label is the displayed verdict ("latest" / "⬆️ update available")
    // and version the dates behind it, since the renderer shows a literal column's _label
    // companion and puts the principal value in the tooltip. Column-compatible with the IRI-keyed
    // variant, which the view supplies for non-space pages.
    public static final String LIST_PRESET_ASSIGNMENTS_REF = "RArXnUQf2dQguqlteWMwKVWxIpnk5xzF4ppAokGkpuOn8/list-preset-assignments";

    private static final Logger logger = LoggerFactory.getLogger(QueryApiAccess.class);

    private static ConcurrentMap<String, Pair<Long, String>> latestVersionMap = new ConcurrentHashMap<>();

    private static final String queryIriPattern = "^(.*[^A-Za-z0-9-_])(RA[A-Za-z0-9-_]{43})[/#]([^/#]+)$";

    /**
     * Forces the retrieval of an API response for a given query name and parameters.
     * Retries until a valid response is received.
     *
     * @param queryRef The query reference
     * @return The API response.
     */
    public static ApiResponse forcedGet(QueryRef queryRef) {
        long deadline = System.currentTimeMillis() + 30_000;
        long sleepMs = 1000;
        while (System.currentTimeMillis() < deadline) {
            try {
                ApiResponse resp = QueryApiAccess.get(queryRef);
                if (resp != null) {
                    return resp;
                }
            } catch (Exception ex) {
                logger.error("Error while forcing API get for query {}", queryRef, ex);
            }
            try {
                Thread.sleep(Math.min(sleepMs, Math.max(0, deadline - System.currentTimeMillis())));
                sleepMs = Math.min(sleepMs * 2, 16_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new RuntimeException("Timed out forcing API get for query: " + queryRef);
    }

    /**
     * Retrieves an API response for a given query reference.
     *
     * @param queryRef The query reference
     * @return The API response.
     * @throws org.nanopub.extra.services.FailedApiCallException         If the API call fails.
     * @throws org.nanopub.extra.services.APINotReachableException       If the API is not reachable.
     * @throws org.nanopub.extra.services.NotEnoughAPIInstancesException If there are not enough API instances.
     */
    public static ApiResponse get(QueryRef queryRef) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        if (!queryRef.getQueryId().matches("^RA[A-Za-z0-9-_]{43}/.*$")) {
            throw new IllegalArgumentException("QueryRef name must be full query ID: " + queryRef.getQueryId());
        }
        return QueryAccess.get(queryRef);
    }

    /**
     * Retrieves the latest version ID of a given nanopublication.
     *
     * @param nanopubId The ID of the nanopublication.
     * @return The latest version ID.
     */
    public static String getLatestVersionId(String nanopubId) {
        long currentTime = System.currentTimeMillis();
        if (!latestVersionMap.containsKey(nanopubId) || currentTime - latestVersionMap.get(nanopubId).getLeft() > 1000 * 60) {
            // Re-fetch if existing value is older than 1 minute
            try {
                ApiResponse r = ApiCache.retrieveResponseSync(new QueryRef(GET_LATEST_VERSION_OF_NP, "np", nanopubId), false);
                if (r != null && r.getData().size() == 1) {
                    String l = r.getData().getFirst().get("latest");
                    latestVersionMap.put(nanopubId, Pair.of(currentTime, l));
                }
            } catch (Exception ex) {
                logger.error("Error while getting latest version of nanopub '{}'", nanopubId, ex);
            }
        }
        Pair<Long, String> cached = latestVersionMap.get(nanopubId);
        return cached != null ? cached.getRight() : nanopubId;
    }

    /**
     * Drops the memoized latest-version lookup for a nanopub, so that the next
     * {@link #getLatestVersionId(String)} goes back to the query API instead of answering
     * from a memo that can be up to a minute old. For the places where the user explicitly
     * asks for current data, such as {@link View#refreshLatestVersion(String)}.
     *
     * @param nanopubId The ID of the nanopublication.
     */
    public static void forgetLatestVersion(String nanopubId) {
        latestVersionMap.remove(nanopubId);
    }

    /**
     * Checks whether the given nanopublication has been loaded by the query services,
     * with a single cheap indexed lookup. A negative answer only means the instance that
     * happened to answer does not have the nanopub yet.
     *
     * @param nanopubId The ID of the nanopublication.
     * @return True if the answering query service instance has the nanopub.
     * @throws org.nanopub.extra.services.FailedApiCallException         If the API call fails.
     * @throws org.nanopub.extra.services.APINotReachableException       If the API is not reachable.
     * @throws org.nanopub.extra.services.NotEnoughAPIInstancesException If there are not enough API instances.
     */
    public static boolean isNanopubLoaded(String nanopubId) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        ApiResponse r = get(new QueryRef(CHECK_NANOPUB_LOADED, "np", nanopubId));
        return r != null && !r.getData().isEmpty();
    }

    /**
     * Checks whether the given IRI has already been used as the identifier of a resource,
     * i.e. whether some nanopublication already introduces it.
     * <p>
     * This is the question an identifier that carries no artifact code raises: nothing makes
     * it unique, so the same form filled with the same name twice yields the same IRI, and
     * the second nanopublication silently attaches itself to the first one's resource (#646).
     * The lookup runs against the meta repository, where {@code npx:introduces} is indexed.
     * <p>
     * A query service that cannot be reached answers false: a check that cannot be made is
     * not evidence of a collision, and publishing should not depend on the query services
     * being up.
     *
     * @param uri The IRI to check.
     * @return True if a nanopublication introducing the IRI was found.
     */
    public static boolean isUriIntroduced(String uri) {
        try {
            ApiResponse r = get(new QueryRef(GET_INTRODUCING_NANOPUB, "thing", uri));
            return r != null && !r.getData().isEmpty();
        } catch (Exception ex) {
            logger.error("Could not check whether IRI '{}' is already introduced", uri, ex);
            return false;
        }
    }

    /**
     * Extracts the query ID from a given query IRI.
     *
     * @param queryIri The query IRI.
     * @return The query ID, or null if the IRI is invalid.
     */
    public static String getQueryId(IRI queryIri) {
        if (queryIri == null) {
            return null;
        }
        if (!queryIri.stringValue().matches(queryIriPattern)) {
            return null;
        }
        return queryIri.stringValue().replaceFirst(queryIriPattern, "$2/$3");
    }

    /**
     * Extracts the query name from a given query IRI.
     *
     * @param queryIri The query IRI.
     * @return The query name, or null if the IRI is invalid.
     */
    public static String getQueryName(IRI queryIri) {
        if (queryIri == null) {
            return null;
        }
        if (!queryIri.stringValue().matches(queryIriPattern)) {
            return null;
        }
        return queryIri.stringValue().replaceFirst(queryIriPattern, "$3");
    }

}
