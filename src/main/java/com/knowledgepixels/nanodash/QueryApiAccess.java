package com.knowledgepixels.nanodash;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;

public class QueryApiAccess {

	private QueryApiAccess() {}  // no instances allowed

	private static Map<String,String> queryIds = new HashMap<>();

	static {
		// TODO Load this dynamically somehow at some point:
		load("RAe-oA5eSmkCXCALZ99-0k4imnlI74KPqURfhHOmnzo6A/get-latest-nanopubs-from-pubkeys");
		load("RAuy4N1h4vZ1wgBUMvTiWw2y_Y0_5oFYRTwdq-xj2qqNM/get-latest-nanopubs-from-userid");
		load("RAZbyFSenuKSRMLGlRfrbeu6vQ6g2IEECYZ2zSGcIBIhQ/get-accepted-nanopubs-by-author");
		load("RAiCBvPL2hRGzI8g5L68O-C9yEXryC_vG35GdEm5jtH_s/get-user-stats-from-pubkeys");  // Deactivated for now...
		load("RA3U23LL3xbNwsu92fAqsKb0kagOud4f9TlRQq3evNJck/get-user-stats-from-userid");  // Deactivated for now...
		load("RAcNvmEiUNUb2a7O4fwRvy2x2BCN640AC880fTzFworr8/get-top-creators-last30d");
		load("RAmhy4KQe6I80bA2Da4JziYyKBoXuXIzqo57GDSVgLfDg/get-top-authors");
		load("RA7oUCHG8TEjVQpGTUN5sfu3_IQmza3aSBSCxfJdBc3Rs/get-most-recent-nanopubs");
		load("RA52cg2OzJucmpCb7KSKTgfPV5HKIjsFcmww_rxb7v5zU/get-latest-accepted");
		load("RAPGhXDRzeGu-Qk0AkjleEtxMxqAvJ-dZn7985gzAbyhs/get-publisher-version");
		load("RAvL7pe2ppsfq4mVWTdJjssYGsjrmliNd_sZO2ytLvg1Y/get-most-used-templates-last30d");
		load("RANn4Mu8r8bqJA9KJMGXTQAEGAEvtNKGFsuhRIC6BRIOo/get-latest-nanopubs-by-type");
		load("RAiRsB2YywxjsBMkVRTREJBooXhf2ZOHoUs5lxciEl37I/get-latest-version-of-np");
		load("RA5i5-o05hg5KlZocCvKlQAV_CcJn3ToW3AGK7TbrCkVM/get-all-user-intros");
		load("RA-tlMmQA7iT2wR2aS3PlONrepX7vdXbkzeWluea7AECg/get-suggested-templates-to-get-started");
		load("RAtIndAeo8zYnEvTjFRaHD2AlenvkJHlw6P52JWR_l5dQ/get-type-overview-last-12-months");
		load("RAn3agwsH2yk-8132RJApGYxdPSHHCXDAIYiCaSBBo6tg/get-approved-nanopubs");
		load("RAz1ogtMxSTKSOYwHAfD5M3Y-vd1vd46OZta_vvbqh8kY/find-uri-references");
		load("RAE35dYJQlpnqim7VeKuu07E9I1LQUZpkdYQR4RvU3KMU/get-nanopubs-by-type");
		load("RALZXWg5lZoJoQ0VHL5mpDgNxYpqU6FoDLWGp4rs8A6b8/get-introducing-nanopub");
		load("RAWruhiSmyzgZhVRs8QY8YQPAgHzTfl7anxII1de-yaCs/fulltext-search-on-labels");
		load("RAVEmFh3d6qonTFQ5S9SVqXZh0prrH1YLhSSs0dJvyvpM/find-things");
		load("RAjt1H9rCSr6A9VGzlhye00zPdH69JdGc3kd_2VjDmzVg/get-instances");
		load("RAH06iUwnvj_pRARY15ayJAY5tuJau3rCvHhPPhe49fVI/get-classes-for-thing");
		load("RAJStXEm1wZcg34ZLPqe00VPSzIVCwC2rrxdj_JR8v5DY/find-referencing-nanopubs");  // not yet used...
		load("RAtftxAXJubB4rlm9fOvvHNVIkXvWQLC6Ag_MiV7HL0ow/get-labels-for-thing");  // not yet used...
		load("RARtWHRzNY5hh31X2VB5eOCJAdp9Cjv4CakA0Idqz69MI/get-templates-with-uri");
		load("RAfiHseHSs9ED7zbXD-OotwblOsT-AfgZ5_2RUeQkBdFc/get-introducing-np");
		load("RAWH0fe1RCpoOgaJE1B2qfTzzdTiBUUK7iIk6l7Zll9mg/get-newer-versions-of-np");
		load("RAQqjXQYlxYQeI4Y3UQy9OrD5Jx1E3PJ8KwKKQlWbiYSw/get-queries");
		load("RAzXDzCHoZmJITgYYquLwDDkSyNf3eKKQz9NfQPYB1cyE/get-latest-thing-nanopub");
		load("RAnkM-_WqYU_dch4YqbL90lNJlXJOFV17R14Ntt1WGaNM/get-projects");
		load("RAJmZoM0xCGE8OL6EgmQBOd1M58ggNkwZ0IUqHOAPRfvE/get-parts");
	}

	private static void load(String queryId) {
		queryIds.put(queryId.substring(46), queryId);
	}

	public static ApiResponse forcedGet(String queryName) {
		return forcedGet(queryName, new HashMap<>());
	}

	public static ApiResponse forcedGet(String queryName, Map<String,String> params) {
		while (true) {
			ApiResponse resp = null;
			try {
				resp = QueryApiAccess.get(queryName, params);
			} catch (FailedApiCallException ex) {
				ex.printStackTrace();
			}
			if (resp != null) return resp;
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static ApiResponse get(String queryName) throws FailedApiCallException {
		return get(queryName, new HashMap<>());
	}

	public static ApiResponse get(String queryName, String paramKey, String paramValue) throws FailedApiCallException {
		Map<String,String> params = new HashMap<>();
		params.put(paramKey, paramValue);
		return get(queryName, params);
	}

	public static ApiResponse get(String queryName, Map<String,String> params) throws FailedApiCallException {
		String queryId;
		if (queryName.matches("^RA[A-Za-z0-9-_]{43}/.*$")) {
			queryId = queryName;
		} else if (queryIds.containsKey(queryName)) {
			queryId = queryIds.get(queryName);
		} else {
			throw new IllegalArgumentException("Query name not known: " + queryName);
		}
		return QueryAccess.get(queryId, params);
	}

	private static Map<String,Pair<Long,String>> latestVersionMap = new HashMap<>();

	public static String getLatestVersionId(String nanopubId) {
		long currentTime = System.currentTimeMillis();
		if (!latestVersionMap.containsKey(nanopubId) || currentTime - latestVersionMap.get(nanopubId).getLeft() > 1000*60) {
			// Re-fetch if existing value is older than 1 minute
			Map<String,String> params = new HashMap<>();
			params.put("np", nanopubId);
			try {
				ApiResponse r = get("get-latest-version-of-np", params);
				if (r.getData().size() != 1) return nanopubId;
				String l = r.getData().get(0).get("latest");
				latestVersionMap.put(nanopubId, Pair.of(currentTime, l));
			} catch (Exception ex) {
				ex.printStackTrace();
				return nanopubId;
			}
		}
		return latestVersionMap.get(nanopubId).getRight();
	}

	private static final String queryIriPattern = "^(.*[^A-Za-z0-9-_])(RA[A-Za-z0-9-_]{43})[/#]([^/#]+)$";

	public static String getQueryId(IRI queryIri) {
		if (queryIri == null) return null;
		if (!queryIri.stringValue().matches(queryIriPattern)) return null;
		return queryIri.stringValue().replaceFirst(queryIriPattern, "$2/$3");
	}

	public static String getQueryId(String queryName) {
		return queryIds.get(queryName);
	}

	public static String getQueryName(IRI queryIri) {
		if (queryIri == null) return null;
		if (!queryIri.stringValue().matches(queryIriPattern)) return null;
		return queryIri.stringValue().replaceFirst(queryIriPattern, "$3");
	}

}
