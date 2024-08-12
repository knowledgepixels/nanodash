package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryAccess;

import com.opencsv.exceptions.CsvValidationException;

public class ApiCache {

	private ApiCache() {}  // no instances allowed


	private transient static Map<String,List<NanopubElement>> cachedNanopubLists = new HashMap<>();
	private transient static Map<String,Long> lastRefresh = new HashMap<>();
	private transient static Map<String,Long> refreshStart = new HashMap<>();

	public static boolean isRunning(String pubkeyHashes) {
		if (!refreshStart.containsKey(pubkeyHashes)) return false;
		return System.currentTimeMillis() - refreshStart.get(pubkeyHashes) < 60 * 1000;
	}

	private static void updateNanopubList(String pubkeyHashes) {
		List<NanopubElement> nanopubs = getNanopubList(pubkeyHashes);
		cachedNanopubLists.put(pubkeyHashes, nanopubs);
		lastRefresh.put(pubkeyHashes, System.currentTimeMillis());
	}

	private static List<NanopubElement> getNanopubList(String pubkeyHashes) {
		List<NanopubElement> nanopubs = new ArrayList<>();
		try {
			Map<String,String> nanopubParams = new HashMap<>();
			List<ApiResponseEntry> nanopubResults = new ArrayList<>();
			nanopubParams.put("pubkeyhashes", pubkeyHashes);
			nanopubResults = QueryAccess.get("RAaLOqOwHVAfH8PK4AzHz5UF-P4vTnd-QnmH4w9hxTo3Y/get-latest-nanopubs-from-pubkeys", nanopubParams).getData();
			while (!nanopubResults.isEmpty() && nanopubs.size() < 20) {
				ApiResponseEntry resultEntry = nanopubResults.remove(0);
				String npUri = resultEntry.get("np");
				nanopubs.add(new NanopubElement(npUri));
			}
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		return nanopubs;
	}

	public static synchronized List<NanopubElement> retrieveNanopubList(String pubkeyHashes) {
		long timeNow = System.currentTimeMillis();
		boolean isCached = false;
		boolean needsRefresh = true;
		if (cachedNanopubLists.containsKey(pubkeyHashes)) {
			long cacheAge = timeNow - lastRefresh.get(pubkeyHashes);
			isCached = cacheAge < 24 * 60 * 60 * 1000;
			needsRefresh = cacheAge > 60 * 1000;
		}
		if (needsRefresh && !isRunning(pubkeyHashes)) {
			refreshStart.put(pubkeyHashes, timeNow);
			new Thread() {

				@Override
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					try {
						ApiCache.updateNanopubList(pubkeyHashes);
					} finally {
						refreshStart.remove(pubkeyHashes);
					}
				}

			}.start();
		}
		if (isCached) {
			return cachedNanopubLists.get(pubkeyHashes);
		} else {
			return null;
		}
	}

}
