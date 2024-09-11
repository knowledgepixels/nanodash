package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryAccess;

import com.opencsv.exceptions.CsvValidationException;


public class ApiAccess {

	private ApiAccess() {}  // no instances allowed

	private static Map<String,String> queryIds = new HashMap<>();

	static {
		// TODO Load this dynamically somehow at some point:
		load("RAaLOqOwHVAfH8PK4AzHz5UF-P4vTnd-QnmH4w9hxTo3Y/get-latest-nanopubs-from-pubkeys");
		load("RAPT_uUoyBjPt0J3FI74XcakoZ7UW6MaiAvHH8mFw-kd4/get-accepted-nanopubs-by-author");
		load("RAmP2Ymp-tiIN0IBzHhdzcYoYAoCr1jJtgH03ekFX6rZA/get-user-stats");
		load("RAna6AB9majJbslfFCtrZaM3_QPKzeDnOUsbGOx2LUgfE/get-top-creators-last30d");
		load("RAmhy4KQe6I80bA2Da4JziYyKBoXuXIzqo57GDSVgLfDg/get-top-authors");
		load("RA7oUCHG8TEjVQpGTUN5sfu3_IQmza3aSBSCxfJdBc3Rs/get-most-recent-nanopubs");
		load("RA52cg2OzJucmpCb7KSKTgfPV5HKIjsFcmww_rxb7v5zU/get-latest-accepted");
		load("RAnwrEraMUbgSrKVasLdbHx9lcrqA1AIFBg0EpuVz8diE/get-publisher-version-at-bdj-journal");
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
