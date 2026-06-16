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
    public static final String GET_VIEW_DISPLAYS = "RAy49uUd2fPLHJAZ_7QKDtIDVgqaQ589OgQhMwNamKy-4/get-view-displays";

    // Spaces-repo queries (endpoint: nanopub-query .../repo/spaces)
    // v2: IRI-keyed get-spaces. Prior client head, retained for reference; deployments up
    // to this release stay pinned on it, and the roll-out fork-merge will supersede both
    // it and v3. No longer fetched by SpaceRepository (now uses GET_SPACES_REF).
    public static final String GET_SPACES = "RAxGboS_juHuMyJQghGV3elEgZmQTew5oyw_aC9O9FFQI/get-spaces";
    // v3: ref-aware get-spaces (adds ?ref + ?root so the client can key one space per
    // ref). Published as an independent nanopub (no npx:supersedes). Active query used by
    // SpaceRepository. Source at docs/queries/get-spaces-ref.trig. See
    // docs/space-ref-identity.md.
    public static final String GET_SPACES_REF = "RAD5KmWO6uqjM04tK7tb2IREgbxA1GTGyRhaRjjaVIKPw/get-spaces";
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
    public static final String GET_SPACE_MEMBERS_REF = "RAqp9TSM4oAwvJ0UQrvZ-qzEuS4R8zpsuD_lw1lyW5MOw/get-space-members";
    // Ref-scoped observers (Stage 2): takes the ref's root nanopub (root_np), lists observers
    // INCLUDING un-introduced self-declared ones (not in the validated state), each flagged
    // via a headerless ?unverified_noheader column (⚠️ when unvalidated). Drives the existing
    // Observers view's table (the view nanopub is left untouched). Published independently.
    // v2 (RA58KSjh, supersedes RARc37t3) excludes higher-tier role claims (admin built-in
    // property, or Maintainer/Member-tier declarations) — those go to LIST_SPACE_NON_APPROVED_REF
    // instead of showing up here mislabelled as observers. Source at
    // docs/queries/list-space-observers-ref-v2.trig.
    public static final String LIST_SPACE_OBSERVERS_REF = "RA58KSjhMzsFjibtL02m11Xptk0A-CtAjG8wWhvA_ljmQ/list-space-observers";

    // Ref-scoped non-approved role claims (root_np): agents holding a higher-tier role
    // instantiation (admin/maintainer/member) that is NOT in the validated state — a
    // self-assigned or otherwise ungranted claim awaiting approval by an equal-or-higher-tier
    // member. Observer-tier roles are excluded (self-assignable, so they need no approval and
    // are listed by LIST_SPACE_OBSERVERS_REF). Only admin claims are detectable today (the live
    // repo materialises every declaration as ObserverRole). Drives the "❓ Pending
    // Admins/Maintainers/Members" view. Source at docs/queries/list-space-non-approved-ref.trig.
    public static final String LIST_SPACE_NON_APPROVED_REF = "RAZMAChiW6g1uJ02fYKuw_1tVk6XPUI1PpYiSraUDYpVY/list-space-non-approved";

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
