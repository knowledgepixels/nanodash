package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryAccess;

import com.opencsv.exceptions.CsvValidationException;


public class QueryApiAccess {

	private QueryApiAccess() {}  // no instances allowed

	private static Map<String,String> queryIds = new HashMap<>();

	static {
		// TODO Load this dynamically somehow at some point:
		load("RACzZG4HmDGSg4N0KnNrymC_CMOcahWKvIj7yWm4Z7C-4/get-latest-nanopubs-from-pubkeys");
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
		load("RAmbWGfXZiHA1S2RsuVnoEmg54oFYzGARBQXlvb_K5QqQ/get-approved-nanopubs");
		load("RAz1ogtMxSTKSOYwHAfD5M3Y-vd1vd46OZta_vvbqh8kY/find-uri-references");
		load("RAE35dYJQlpnqim7VeKuu07E9I1LQUZpkdYQR4RvU3KMU/get-nanopubs-by-type");
		load("RALZXWg5lZoJoQ0VHL5mpDgNxYpqU6FoDLWGp4rs8A6b8/get-introducing-nanopub");
		load("RAWruhiSmyzgZhVRs8QY8YQPAgHzTfl7anxII1de-yaCs/fulltext-search-on-labels");
		load("RAVEmFh3d6qonTFQ5S9SVqXZh0prrH1YLhSSs0dJvyvpM/find-things");
		load("RAzWRujvw65FF4MpUj9HPTdYL5G72udjy5-iRYCSXzLFs/get-instance-count");
		load("RA4GfMzFgb1ZjSKlUA_aKgmRvf_knCkN-3o5oGca4AGyw/get-latest-instance-nps");
		load("RAhWK15S--cGM4Hy7oiO5RA0zLpRAAGEnxGvgWbCGvikY/get-classes-for-thing");
	}

	private static void load(String queryId) {
		queryIds.put(queryId.substring(46), queryId);
	}

	public static ApiResponse get(String queryName) {
		return get(queryName, new HashMap<>());
	}

	public static ApiResponse get(String queryName, String paramKey, String paramValue) {
		Map<String,String> params = new HashMap<>();
		params.put(paramKey, paramValue);
		return get(queryName, params);
	}

	public static ApiResponse get(String queryName, Map<String,String> params) {
		String queryId;
		if (queryName.matches("^RA[A-Za-z0-9-_]{43}/.*$")) {
			queryId = queryName;
		} else if (queryIds.containsKey(queryName)) {
			queryId = queryIds.get(queryName);
		} else {
			throw new IllegalArgumentException("Query name not known: " + queryName);
		}
		try {
			return QueryAccess.get(queryId, params);
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		return null;
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

}
