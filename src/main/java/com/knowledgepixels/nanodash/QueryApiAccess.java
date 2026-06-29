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
    public static final String GET_USER_STATS_FROM_PUBKEYS = "RAiCBvPL2hRGzI8g5L68O-C9yEXryC_vG35GdEm5jtH_s/get-user-stats-from-pubkeys";
    public static final String GET_USER_STATS_FROM_USERID = "RA3U23LL3xbNwsu92fAqsKb0kagOud4f9TlRQq3evNJck/get-user-stats-from-userid";
    public static final String GET_TOP_CREATORS_LAST30D = "RAcNvmEiUNUb2a7O4fwRvy2x2BCN640AC880fTzFworr8/get-top-creators-last30d";
    public static final String GET_LATEST_USERS = "RAr27GmRUKQmvPbfmB34N9l9lX-xYK7nQhvOMbQCk3byI/get-latest-users";
    public static final String GET_MOST_RECENT_NANOPUBS = "RAYNg6rfvXIVvJY2u8oS0EEjxnVvimLLVZG1rOar_nWIY/get-most-recent-nanopubs";
    public static final String GET_PUBLISHER_VERSION = "RAPGhXDRzeGu-Qk0AkjleEtxMxqAvJ-dZn7985gzAbyhs/get-publisher-version";
    public static final String GET_MOST_USED_TEMPLATES_LAST30D = "RAvL7pe2ppsfq4mVWTdJjssYGsjrmliNd_sZO2ytLvg1Y/get-most-used-templates-last30d";
    public static final String GET_LATEST_NANOPUBS_BY_TYPE = "RANn4Mu8r8bqJA9KJMGXTQAEGAEvtNKGFsuhRIC6BRIOo/get-latest-nanopubs-by-type";
    public static final String GET_LATEST_VERSION_OF_NP = "RAiRsB2YywxjsBMkVRTREJBooXhf2ZOHoUs5lxciEl37I/get-latest-version-of-np";
    public static final String GET_ALL_USER_INTROS = "RAjHh6P11QFUaoPiMRBavdAnTq4YMJW4PB85oVFSBfYjU/get-all-user-intros";
    public static final String GET_ALL_USER_PROFILE_PICS= "RAtcodMPmTrmBvdOqwYIrNNFDO74f8B_xo0qsOcKlCwTA/get-all-user-profile-pics";
    public static final String GET_ALL_USER_DEFAULT_LICENSE = "RA-_IwzReR2_HfTLz4YcNM6Mh3Vt16y0RUS12tpJTN9FI/get-all-user-default-license";
    public static final String GET_SUGGESTED_TEMPLATES_TO_GET_STARTED = "RA-tlMmQA7iT2wR2aS3PlONrepX7vdXbkzeWluea7AECg/get-suggested-templates-to-get-started";
    public static final String GET_MONTHLY_TYPE_OVERVIEW_BY_PUBKEYS = "RAhI-C2KsqS_IvnxwyBrbMFsoj65dhLWE_CBo_KtcVEVA/get-monthly-type-overview-by-pubkeys";
    public static final String GET_APPROVED_NANOPUBS = "RAn3agwsH2yk-8132RJApGYxdPSHHCXDAIYiCaSBBo6tg/get-approved-nanopubs";
    public static final String FIND_URI_REFERENCES = "RAz1ogtMxSTKSOYwHAfD5M3Y-vd1vd46OZta_vvbqh8kY/find-uri-references";
    public static final String GET_NANOPUBS_BY_TYPE = "RAE35dYJQlpnqim7VeKuu07E9I1LQUZpkdYQR4RvU3KMU/get-nanopubs-by-type";
    public static final String GET_INTRODUCING_NANOPUB = "RALZXWg5lZoJoQ0VHL5mpDgNxYpqU6FoDLWGp4rs8A6b8/get-introducing-nanopub";
    public static final String FULLTEXT_SEARCH = "RAxdh5xkc6K6SMLY23yKu__zTWJPXeRFc0qgNNxkbOkpY/fulltext-search";
    public static final String FIND_THINGS = "RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things";
    public static final String GET_INSTANCES = "RAjt1H9rCSr6A9VGzlhye00zPdH69JdGc3kd_2VjDmzVg/get-instances";
    public static final String GET_CLASSES_FOR_THING = "RAH06iUwnvj_pRARY15ayJAY5tuJau3rCvHhPPhe49fVI/get-classes-for-thing";
    public static final String FIND_REFERENCING_NANOPUBS = "RAJStXEm1wZcg34ZLPqe00VPSzIVCwC2rrxdj_JR8v5DY/find-referencing-nanopubs";
    public static final String GET_LABELS_FOR_THING = "RAtftxAXJubB4rlm9fOvvHNVIkXvWQLC6Ag_MiV7HL0ow/get-labels-for-thing";
    public static final String GET_TEMPLATES_WITH_URI = "RARtWHRzNY5hh31X2VB5eOCJAdp9Cjv4CakA0Idqz69MI/get-templates-with-uri";
    public static final String GET_NEWER_VERSIONS_OF_NP = "RAqmmNSxQaRNWRYH0o4Da3GSOwvoFLObhXfAGUCOqEtfw/get-newer-versions-of-np";
    public static final String GET_QUERIES = "RAQqjXQYlxYQeI4Y3UQy9OrD5Jx1E3PJ8KwKKQlWbiYSw/get-queries";
    public static final String GET_LATEST_THING_NANOPUB = "RAzXDzCHoZmJITgYYquLwDDkSyNf3eKKQz9NfQPYB1cyE/get-latest-thing-nanopub";
    public static final String GET_PROJECTS = "RAnpimW7SPwaum2fefdS6_jpzYxcTRGjE-pmgNTL_BBJU/get-projects";
    public static final String GET_OWNERS = "RApiw7Z0NeP3RaLiqX6Q7Ml5CfEWbt-PysUbMNljuiLJw/get-owners";
    public static final String GET_MEMBERS = "RASyFJyADTtG-l_Qe3a5PE_e2yUJR-PydXfkZjjrBuV7U/get-members";
    public static final String GET_PARTS = "RAJmZoM0xCGE8OL6EgmQBOd1M58ggNkwZ0IUqHOAPRfvE/get-parts";
    public static final String GET_ASSERTION_TEMPLATES = "RA6bgrU3Ezfg5VAiLru0BFYHaSj6vZU6jJTscxNl8Wqvc/get-assertion-templates";
    public static final String GET_PROVENANCE_TEMPLATES = "RA4bt3MQRnEPC2nSsdbCJc74wT-e1w68dSCpYVyvG0274/get-provenance-templates";
    public static final String GET_PUBINFO_TEMPLATES = "RAMcdiJpvvk8424AJIH1jsDUQVcPYOLRw0DNnZt_ND_LQ/get-pubinfo-templates";
    public static final String GET_FILTERED_NANOPUB_LIST = "RAeoXI4vBzLV_BM2lfI5DWkFSfm6y1z3fOk4E1IncXWUo/get-filtered-nanopub-list";
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
    public static final String GET_VIEW_DISPLAYS = "RA4TGV_zGtvVPjvffGF6OTbNV-ZmjwIdcZ3-JNYdEhpoE/get-view-displays";
    // Ref-scoped get-view-displays (the Content-tab renderer query): takes the space IRI (resource)
    // AND the ref's root nanopub (root_np) as two concrete params, gating the authorised signers on
    // that ref's admins/maintainers (npa:forSpaceRef) instead of the IRI merged across refs, so the
    // rendered Content views match the space ref shown. Both params concrete so the 4-SERVICE
    // federation propagates. Column-identical to get-view-displays. Source at
    // docs/queries/get-view-displays-ref.trig. See docs/space-ref-identity.md.
    // RAny-yPb (supersedes RA8iqtd) gates the view-version-resolution npx:invalidates filters on
    // the version nanopub's own signing pubkey (issue #487; the row-level filters were already
    // gated). No-regression verified against RA8iqtd across spaces.
    // RAtZbUry (supersedes RAIpgHc6): same hasGoverningSpaceRef gate as get-view-displays, with the
    // governing ref pinned to ?passedRef so authority cannot bleed across rival refs (issue #510).
    public static final String GET_VIEW_DISPLAYS_REF = "RAtZbUry42si1BZpBoRBkgbzGweNzfPmOludNIoJYT2VM/get-view-displays";

    // Spaces-repo queries (endpoint: nanopub-query .../repo/spaces)
    // v2: IRI-keyed get-spaces. Prior client head, retained for reference; deployments up
    // to this release stay pinned on it, and the roll-out fork-merge will supersede both
    // it and v3. No longer fetched by SpaceRepository (now uses GET_SPACES_REF).
    public static final String GET_SPACES = "RAxGboS_juHuMyJQghGV3elEgZmQTew5oyw_aC9O9FFQI/get-spaces";
    // v3: ref-aware get-spaces (adds ?ref + ?root so the client can key one space per
    // ref). Published as an independent nanopub (no npx:supersedes). Active query used by
    // SpaceRepository. Source at docs/queries/get-spaces-ref.trig. See
    // docs/space-ref-identity.md.
    // v4 (RAyXmrfs, supersedes RAD5KmWO) gates the npx:invalidates filter on a shared signing
    // pubkey between invalidator and the space-definition nanopub (issue #487).
    public static final String GET_SPACES_REF = "RAyXmrfs8HeSJWGxz2dFX7qhMIsvTMzWro0J6EyBvsNu8/get-spaces";
    // Disambiguation claimants: one row per ref (root definition) claiming a space IRI, with that
    // ref's validated admins (admins_multi_iri). Pass the space IRI; replaces the per-ref
    // get-space-admins fan-out with a single fetch. Which ref is the representative (default) is
    // decided client-side. Source at docs/queries/list-space-claimants.trig.
    public static final String LIST_SPACE_CLAIMANTS = "RApsQhJnK7MV5fHzFQe4GsnsUdf_HvPT186E02JE-4CTY/list-space-claimants";
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
    public static final String GET_SPACE_ROLES = "RAKJFw-xIQ2r_aSKT4-6Pm3JkeqlWC_wmypfpA1JWPJl8/get-space-roles";
    // Ref-scoped roles (Stage 2): takes the ref's root nanopub (root_np), matches
    // RoleAssignments on npa:forSpaceRef, so multi-ref spaces don't merge role sets across
    // refs. Published independently. Source at docs/queries/get-space-roles-ref.trig.
    public static final String GET_SPACE_ROLES_REF = "RAqUWUfmEmzxpkeuXek7oEiVSnwjuzRfV8kRe7pQSpe4c/get-space-roles";
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
    public static final String GET_SPACE_MEMBERS_REF = "RA2eGba0_0GLtyFWPH2PZe76G0d8azkHaojNCgacifTyI/get-space-members";
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
    // is preserved.
    public static final String LIST_SPACE_OBSERVERS_REF = "RAQylZL4shGjfhxcBiqoanuY2-cUJcVeWvpZDkfjP9_ko/list-space-observers";

    // Ref-scoped non-approved role claims (root_np): agents holding a higher-tier role
    // instantiation (admin/maintainer/member) that is NOT in the validated state — a
    // self-assigned or otherwise ungranted claim awaiting approval by an equal-or-higher-tier
    // member. Observer-tier roles are excluded (self-assignable, so they need no approval and
    // are listed by LIST_SPACE_OBSERVERS_REF). Only admin claims are detectable today (the live
    // repo materialises every declaration as ObserverRole). Drives the "❓ Pending
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
    // `filter( ?inSpace = ?spaceIri || exists { ... sameAsSpace ... } )`. Source at
    // docs/queries/list-space-non-approved-ref-v5.trig.
    public static final String LIST_SPACE_NON_APPROVED_REF = "RAtSaYBHpb2iG6dwlHRHVfUpygNAKS-3bUa1iV5YNqk3w/list-space-non-approved";

    // Ref-scoped variants of the four About-tab *view* display queries (distinct from the
    // GET_SPACE_*_REF client-authority queries above). Each takes the ref's root nanopub
    // (root_np), resolves the ref via npa:rootNanopub, and scopes by npa:forSpaceRef (members,
    // roles) or the ref-level npa:hasSubSpace / npa:hasMaintainedResource edge (sub-spaces,
    // maintained resources), so a ?root=-pinned space page shows only that one ref's listings
    // rather than merging all refs claiming the IRI. Column-compatible with the IRI-keyed view
    // queries, so they drive the existing view nanopubs unchanged (the observers pattern). Used
    // by AboutSpacePanel with an IRI-keyed fallback when the ref root is unknown. Published
    // independently (no npx:supersedes). Sources at docs/queries/list-*-ref.trig. See
    // docs/space-ref-identity.md.
    // v3 (RApyKS9D): reads each membership's tier directly off the materialized gen:RoleInstantiation
    // (npa:hasRoleType) and its role (gen:hasRole) in the current space state, now that nanopub-query
    // persists tier on the instantiation (nanopub-query#125 + #127). Simplifies away the earlier
    // RoleAssignment-scoping workaround and the global RoleDeclaration matching that leaked observer-tier
    // members into the Approved listing. See nanodash#498. Latest (RA7E54m5, supersedes RApyKS9D)
    // drops the role-label coalesce to read schema:name only.
    public static final String LIST_SPACE_MEMBERS_REF = "RA7E54m5Hb413Ud-0T9HEiH9wnxlJXBkcUT701NDaUGQk/list-space-members";
    public static final String LIST_SPACE_ROLES_REF = "RAYrSRARuWV2iTWVe6tKDgkaED8ztlr1q5Z5QBIDV4a-Q/list-space-roles";
    public static final String LIST_SUB_SPACES_REF = "RA-j0DFqkNUHxF_WIds8wWJix6DkDFBmUBWmKXfG24XYQ/list-sub-spaces";
    public static final String LIST_MAINTAINED_RESOURCES_REF = "RAPthUMRDXiJeD2BrOsZigTsbA0LktBc-HC4alDSfVNKM/list-maintained-resources";

    // View-displays listing queries are no longer referenced here: the About-tab view-displays
    // tables are view-driven (gen:hasViewQuery on the space/user/maintained view nanopubs), and the
    // panels pass the resource + (for spaces/maintained) the ref's root nanopub as params. The
    // ref-scoped query requires root_np; the IRI-keyed variant backs the users' view. See
    // docs/queries/list-view-displays{,-ref}.trig.

    // Part view-displays listing (resource + partid + partclass): the owning resource's displays,
    // each ?displayed_here-flagged for the specific part. RAMy6Nu supersedes RAPaHJiD (renames
    // ?shown_here → ?displayed_here). RAFkUf3A (derived from RAMy6Nu) adds a ?position_label column
    // (first 3 chars of the structural position) so the position cell shows e.g. "1.1" with the full
    // literal on hover.
    // RAs9S7c9 (supersedes RA2LG9c5) swaps the authority gate to the npa:hasGoverningSpaceRef gate
    // keyed on materialized npa:hasRoleType (issue #510), preserving the ?position_label column.
    public static final String LIST_PART_VIEW_DISPLAYS = "RAs9S7c9wdasz2c745f-mBg53JSQ1q20vLLHkmnXCmX2w/list-part-view-displays";

    // Ref-scoped preset-assignment listing (root_np): reads the server-materialised
    // npa:PresetAssignment rows scoped by npa:forSpaceRef from the validated current space-state
    // graph (nanopub-query #122 ref-stamps each admin-authored assignment per ref), with
    // latest-by-date-per-(preset,resource) + npa:isActivated gating (a deactivation is a newer
    // admin-authored row). Column-identical to the IRI-keyed list-preset-assignments, so it drives
    // the existing Preset assignments view unchanged. Used by AboutSpacePanel with an IRI-keyed
    // fallback when the ref root is unknown. Source at docs/queries/list-preset-assignments-ref.trig.
    public static final String LIST_PRESET_ASSIGNMENTS_REF = "RAeLNbudAq68NdqfIL3mtT2YeLnIHZ5T52Qwl_rJzMJJk/list-preset-assignments";

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
                if (resp != null) return resp;
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
                    String l = r.getData().get(0).get("latest");
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
     * Extracts the query ID from a given query IRI.
     *
     * @param queryIri The query IRI.
     * @return The query ID, or null if the IRI is invalid.
     */
    public static String getQueryId(IRI queryIri) {
        if (queryIri == null) return null;
        if (!queryIri.stringValue().matches(queryIriPattern)) return null;
        return queryIri.stringValue().replaceFirst(queryIriPattern, "$2/$3");
    }

    /**
     * Extracts the query name from a given query IRI.
     *
     * @param queryIri The query IRI.
     * @return The query name, or null if the IRI is invalid.
     */
    public static String getQueryName(IRI queryIri) {
        if (queryIri == null) return null;
        if (!queryIri.stringValue().matches(queryIriPattern)) return null;
        return queryIri.stringValue().replaceFirst(queryIriPattern, "$3");
    }

}
