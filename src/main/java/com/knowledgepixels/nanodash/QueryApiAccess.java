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

    private static ConcurrentMap<String, String> queryIds = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(QueryApiAccess.class);

    private static ConcurrentMap<String, Pair<Long, String>> latestVersionMap = new ConcurrentHashMap<>();

    private static final String queryIriPattern = "^(.*[^A-Za-z0-9-_])(RA[A-Za-z0-9-_]{43})[/#]([^/#]+)$";

    static {
        // TODO Load this dynamically somehow at some point:
        load("RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys");
        load("RAuy4N1h4vZ1wgBUMvTiWw2y_Y0_5oFYRTwdq-xj2qqNM/get-latest-nanopubs-from-userid");
        load("RAiCBvPL2hRGzI8g5L68O-C9yEXryC_vG35GdEm5jtH_s/get-user-stats-from-pubkeys");  // Deactivated for now...
        load("RA3U23LL3xbNwsu92fAqsKb0kagOud4f9TlRQq3evNJck/get-user-stats-from-userid");  // Deactivated for now...
        load("RAcNvmEiUNUb2a7O4fwRvy2x2BCN640AC880fTzFworr8/get-top-creators-last30d");
        load("RAr27GmRUKQmvPbfmB34N9l9lX-xYK7nQhvOMbQCk3byI/get-latest-users");
        load("RAYNg6rfvXIVvJY2u8oS0EEjxnVvimLLVZG1rOar_nWIY/get-most-recent-nanopubs");
        load("RAPGhXDRzeGu-Qk0AkjleEtxMxqAvJ-dZn7985gzAbyhs/get-publisher-version");
        load("RAvL7pe2ppsfq4mVWTdJjssYGsjrmliNd_sZO2ytLvg1Y/get-most-used-templates-last30d");
        load("RANn4Mu8r8bqJA9KJMGXTQAEGAEvtNKGFsuhRIC6BRIOo/get-latest-nanopubs-by-type");
        load("RAiRsB2YywxjsBMkVRTREJBooXhf2ZOHoUs5lxciEl37I/get-latest-version-of-np");
        load("RA0aZxyh_I0rCJyBepXOWC2tGdI5YYHORFCC-qBR8xHZA/get-all-user-intros");
        load("RA-tlMmQA7iT2wR2aS3PlONrepX7vdXbkzeWluea7AECg/get-suggested-templates-to-get-started");
        load("RAhI-C2KsqS_IvnxwyBrbMFsoj65dhLWE_CBo_KtcVEVA/get-monthly-type-overview-by-pubkeys");
        load("RAn3agwsH2yk-8132RJApGYxdPSHHCXDAIYiCaSBBo6tg/get-approved-nanopubs");
        load("RAz1ogtMxSTKSOYwHAfD5M3Y-vd1vd46OZta_vvbqh8kY/find-uri-references");
        load("RAE35dYJQlpnqim7VeKuu07E9I1LQUZpkdYQR4RvU3KMU/get-nanopubs-by-type");
        load("RALZXWg5lZoJoQ0VHL5mpDgNxYpqU6FoDLWGp4rs8A6b8/get-introducing-nanopub");
        load("RAWruhiSmyzgZhVRs8QY8YQPAgHzTfl7anxII1de-yaCs/fulltext-search-on-labels");
        load("RAyMrQ89RECTi9gZK5q7gjL1wKTiP8StkLy0NIkkCiyew/find-things");
        load("RAjt1H9rCSr6A9VGzlhye00zPdH69JdGc3kd_2VjDmzVg/get-instances");
        load("RAH06iUwnvj_pRARY15ayJAY5tuJau3rCvHhPPhe49fVI/get-classes-for-thing");
        load("RAJStXEm1wZcg34ZLPqe00VPSzIVCwC2rrxdj_JR8v5DY/find-referencing-nanopubs");  // not yet used...
        load("RAtftxAXJubB4rlm9fOvvHNVIkXvWQLC6Ag_MiV7HL0ow/get-labels-for-thing");  // not yet used...
        load("RARtWHRzNY5hh31X2VB5eOCJAdp9Cjv4CakA0Idqz69MI/get-templates-with-uri");
        load("RAIn9NTsWE0qrpKiK3nOmZRXVzwv0qnfbm7dR_CUnp4aA/get-newer-versions-of-np");
        load("RAQqjXQYlxYQeI4Y3UQy9OrD5Jx1E3PJ8KwKKQlWbiYSw/get-queries");
        load("RAzXDzCHoZmJITgYYquLwDDkSyNf3eKKQz9NfQPYB1cyE/get-latest-thing-nanopub");
        load("RAnpimW7SPwaum2fefdS6_jpzYxcTRGjE-pmgNTL_BBJU/get-projects");
        load("RApiw7Z0NeP3RaLiqX6Q7Ml5CfEWbt-PysUbMNljuiLJw/get-owners");
        load("RASyFJyADTtG-l_Qe3a5PE_e2yUJR-PydXfkZjjrBuV7U/get-members");
        load("RAlPzIqHJsPt33WuPNBIyPDr8KC7htHO9UYvSuYOfTjwo/get-spaces");
        load("RAJmZoM0xCGE8OL6EgmQBOd1M58ggNkwZ0IUqHOAPRfvE/get-parts");
        load("RA6bgrU3Ezfg5VAiLru0BFYHaSj6vZU6jJTscxNl8Wqvc/get-assertion-templates");
        load("RA4bt3MQRnEPC2nSsdbCJc74wT-e1w68dSCpYVyvG0274/get-provenance-templates");
        load("RAMcdiJpvvk8424AJIH1jsDUQVcPYOLRw0DNnZt_ND_LQ/get-pubinfo-templates");
        load("RAOpebLWcp0G6D9U8ZhfYGOkz-kObkytDsM6rCQlIi_j0/get-admins");
        load("RA_Xf7mfC2PAohGYktJEXjkbzxPhkfX4DE1bQKFqkxrRg/get-space-members");
        load("RAbq1a1FwRFAZPDde3Sy4GqNUQ2TmaKOWLydJPOyCKc0w/get-filtered-nanopub-list");
        load("RA_F6C2DtlL4EXSF2vzU1KLVUiRLjPAM-r1Wy32JIeSYI/get-pinned-templates");
        load("RAH5jJUo8ukFkPP1xghveSNPze3pmSK69zri5PPM6HEbg/get-pinned-queries");
        load("RAF-LNZPrT11euM9NQGOLRrJg1pD02oXM5Cxb9XP7tBkU/get-space-member-roles");
        load("RAuwOBxwXal5NTW_rl9WtUA38IJbbT8q_Vp6vq4F6Pu9k/get-views-for-space");
        load("RAkoDiXZG_CYt978-dZ_vffK-UTbN6e1bmtFy6qdmFzC4/get-latest-accepted-bdj");
        load("RAgnLJH8kcI_e488VdoyQ0g3-wcumj4mSiusxPmeAYsSI/get-latest-biodiv-candidates");
        load("RATpsBysLf8yXeMpY7PHKj-aKNCa4-4Okg1hi97OLDXIo/get-latest-accepted-ds");
        load("RAFNTW3jhWKnNvhMSOfYvG53ZAurxrFv_-vnIJkZyfAuo/get-latest-ds-candidates");
        load("RA0FiH8gukovvEHPBMn72zUDdMQylQmUwtIGNLYBZXGfk/get-ds-reactions");
        load("RAAXmnJdXHO86GqJs8VTdqapUWqCrHKRgRT2b4NfjAfgk/get-latest-accepted-rio");
        load("RAehKOCOnZ3uDBmI0kkCNTh5k9Nl6YYNj7tyc20tVymxY/get-latest-rio-candidates");
        load("RAe7k3L0oElPOrFoUMkUhqU9dGUqfBaUSw3cVplOUn3Fk/get-reactions");
        load("RAjaERe_ihZh_TP1CIFKa6gbd1rzqFtx8nFZFgtyd13Bo/get-term-definitions");
    }

    /**
     * Loads a query ID into the queryIds map.
     *
     * @param queryId The query ID to load.
     */
    static void load(String queryId) {
        queryIds.put(queryId.substring(46), queryId);
    }

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
                Thread.sleep(100);
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
        if (queryRef.getName().matches("^RA[A-Za-z0-9-_]{43}/.*$")) {
            // All good
        } else if (queryIds.containsKey(queryRef.getName())) {
            queryRef = new QueryRef(queryIds.get(queryRef.getName()), queryRef.getParams());
        } else {
            throw new IllegalArgumentException("Query name not known: " + queryRef.getName());
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
                ApiResponse r = get(new QueryRef("get-latest-version-of-np", "np", nanopubId));
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
     * Retrieves the query ID for a given query name.
     *
     * @param queryName The name of the query.
     * @return The query ID, or null if the query name is unknown.
     */
    public static String getQueryId(String queryName) {
        return queryIds.get(queryName);
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
