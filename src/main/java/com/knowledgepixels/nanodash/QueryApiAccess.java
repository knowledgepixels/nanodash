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
		load("RAK_BnN1afUqRNAUDjbZq-N7VN9CVMqOAob4otN288aZM/get-latest-nanopubs-from-pubkeys-userid");
		load("RAZbyFSenuKSRMLGlRfrbeu6vQ6g2IEECYZ2zSGcIBIhQ/get-accepted-nanopubs-by-author");
		load("RA3a61zP378T-l2aUmAcov2yDa7-qsl9dFCigZsPmGGyc/get-user-stats");
		load("RAcNvmEiUNUb2a7O4fwRvy2x2BCN640AC880fTzFworr8/get-top-creators-last30d");
		load("RAmhy4KQe6I80bA2Da4JziYyKBoXuXIzqo57GDSVgLfDg/get-top-authors");
		load("RA7oUCHG8TEjVQpGTUN5sfu3_IQmza3aSBSCxfJdBc3Rs/get-most-recent-nanopubs");
		load("RA52cg2OzJucmpCb7KSKTgfPV5HKIjsFcmww_rxb7v5zU/get-latest-accepted");
		load("RAPGhXDRzeGu-Qk0AkjleEtxMxqAvJ-dZn7985gzAbyhs/get-publisher-version");
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
