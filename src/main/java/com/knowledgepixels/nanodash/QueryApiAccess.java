package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
		load("RAT5odJScFpREW7nbgGcnZBw4b4r3nPdqva7h24NJbbiI/get-most-used-templates-last30d");  // not yet used...
	}

	private static void load(String queryId) {
		queryIds.put(queryId.substring(46), queryId);
	}

	public static ApiResponse get(String queryName, Map<String,String> params) {
		if (!queryIds.containsKey(queryName)) {
			throw new IllegalArgumentException("Query name not known: " + queryName);
		}
		try {
			return QueryAccess.get(queryIds.get(queryName), params);
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

}
