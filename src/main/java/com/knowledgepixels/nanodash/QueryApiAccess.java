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
    public static final String GET_NEWER_VERSIONS_OF_NP = "RAIn9NTsWE0qrpKiK3nOmZRXVzwv0qnfbm7dR_CUnp4aA/get-newer-versions-of-np";
    public static final String GET_QUERIES = "RAQqjXQYlxYQeI4Y3UQy9OrD5Jx1E3PJ8KwKKQlWbiYSw/get-queries";
    public static final String GET_LATEST_THING_NANOPUB = "RAzXDzCHoZmJITgYYquLwDDkSyNf3eKKQz9NfQPYB1cyE/get-latest-thing-nanopub";
    public static final String GET_PROJECTS = "RAnpimW7SPwaum2fefdS6_jpzYxcTRGjE-pmgNTL_BBJU/get-projects";
    public static final String GET_OWNERS = "RApiw7Z0NeP3RaLiqX6Q7Ml5CfEWbt-PysUbMNljuiLJw/get-owners";
    public static final String GET_MEMBERS = "RASyFJyADTtG-l_Qe3a5PE_e2yUJR-PydXfkZjjrBuV7U/get-members";
    public static final String GET_SPACES = "RAf0Apox1sbJRC0ZBrqS9wtYccLFJ_5VLq-u4rJy5WbnA/get-spaces";
    public static final String GET_PARTS = "RAJmZoM0xCGE8OL6EgmQBOd1M58ggNkwZ0IUqHOAPRfvE/get-parts";
    public static final String GET_ASSERTION_TEMPLATES = "RA6bgrU3Ezfg5VAiLru0BFYHaSj6vZU6jJTscxNl8Wqvc/get-assertion-templates";
    public static final String GET_PROVENANCE_TEMPLATES = "RA4bt3MQRnEPC2nSsdbCJc74wT-e1w68dSCpYVyvG0274/get-provenance-templates";
    public static final String GET_PUBINFO_TEMPLATES = "RAMcdiJpvvk8424AJIH1jsDUQVcPYOLRw0DNnZt_ND_LQ/get-pubinfo-templates";
    public static final String GET_ADMINS = "RAOpebLWcp0G6D9U8ZhfYGOkz-kObkytDsM6rCQlIi_j0/get-admins";
    public static final String GET_SPACE_MEMBERS = "RA_Xf7mfC2PAohGYktJEXjkbzxPhkfX4DE1bQKFqkxrRg/get-space-members";
    public static final String GET_FILTERED_NANOPUB_LIST = "RAeoXI4vBzLV_BM2lfI5DWkFSfm6y1z3fOk4E1IncXWUo/get-filtered-nanopub-list";
    public static final String GET_PINNED_TEMPLATES = "RA_F6C2DtlL4EXSF2vzU1KLVUiRLjPAM-r1Wy32JIeSYI/get-pinned-templates";
    public static final String GET_PINNED_QUERIES = "RAH5jJUo8ukFkPP1xghveSNPze3pmSK69zri5PPM6HEbg/get-pinned-queries";
    public static final String GET_SPACE_MEMBER_ROLES = "RAF-LNZPrT11euM9NQGOLRrJg1pD02oXM5Cxb9XP7tBkU/get-space-member-roles";
    public static final String GET_LATEST_ACCEPTED_BDJ = "RAkoDiXZG_CYt978-dZ_vffK-UTbN6e1bmtFy6qdmFzC4/get-latest-accepted-bdj";
    public static final String GET_LATEST_BIODIV_CANDIDATES = "RAgnLJH8kcI_e488VdoyQ0g3-wcumj4mSiusxPmeAYsSI/get-latest-biodiv-candidates";
    public static final String GET_LATEST_ACCEPTED_DS = "RATpsBysLf8yXeMpY7PHKj-aKNCa4-4Okg1hi97OLDXIo/get-latest-accepted-ds";
    public static final String GET_LATEST_DS_CANDIDATES = "RAFNTW3jhWKnNvhMSOfYvG53ZAurxrFv_-vnIJkZyfAuo/get-latest-ds-candidates";
    public static final String GET_DS_REACTIONS = "RA0FiH8gukovvEHPBMn72zUDdMQylQmUwtIGNLYBZXGfk/get-ds-reactions";
    public static final String GET_LATEST_ACCEPTED_RIO = "RAAXmnJdXHO86GqJs8VTdqapUWqCrHKRgRT2b4NfjAfgk/get-latest-accepted-rio";
    public static final String GET_LATEST_RIO_CANDIDATES = "RAehKOCOnZ3uDBmI0kkCNTh5k9Nl6YYNj7tyc20tVymxY/get-latest-rio-candidates";
    public static final String GET_REACTIONS = "RAe7k3L0oElPOrFoUMkUhqU9dGUqfBaUSw3cVplOUn3Fk/get-reactions";
    public static final String GET_TERM_DEFINITIONS = "RAZUsK7jU85oUYEVKvMPFlqbwn19oR55IQuFkXuiS_Tkg/get-term-definitions";
    public static final String GET_MAINTAINED_RESOURCES = "RA8RsuKt6HzCXGmw9wMeN4rwSm456bohOuJ0h-jEgXKmM/get-maintained-resources";
    public static final String GET_VIEW_DISPLAYS = "RAapc3jbJ3GkDy0ncKx3pok_zEKqwrT6-Z5TkCP1k96II/get-view-displays";

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
        while (true) {
            ApiResponse resp = null;
            try {
                resp = QueryApiAccess.get(queryRef);
            } catch (Exception ex) {
                // TODO We should be more specific about which exceptions we catch here
                //      and generally improve this, as this could hang forever.
                logger.error("Error while forcing API get for query {}", queryRef, ex);
            }
            if (resp != null) return resp;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                logger.error("Interrupted while forcing API get for query {}", queryRef, ex);
            }
        }
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
                if (r.getData().size() != 1) return nanopubId;
                String l = r.getData().get(0).get("latest");
                latestVersionMap.put(nanopubId, Pair.of(currentTime, l));
            } catch (Exception ex) {
                logger.error("Error while getting latest version of nanopub '{}'", nanopubId, ex);
                return nanopubId;
            }
        }
        return latestVersionMap.get(nanopubId).getRight();
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
