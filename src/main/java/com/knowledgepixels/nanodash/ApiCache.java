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

	private static void updateNanopubList(String queryName, String param) {
		List<NanopubElement> nanopubs = new ArrayList<>();
		String queryId;
		String paramName;
		if (queryName.equals("get-latest-nanopubs-from-pubkeys")) {
			queryId = "RAaLOqOwHVAfH8PK4AzHz5UF-P4vTnd-QnmH4w9hxTo3Y/get-latest-nanopubs-from-pubkeys";
			paramName = "pubkeyhashes";
		} else if (queryName.equals("get-accepted-nanopubs-by-author")) {
			queryId = "RAJvhWQvYiSvCSFXSkasBSotzE5Aj9g-jWTox_Cy6eRwU/get-accepted-nanopubs-by-author";
			paramName = "author";
		} else {
			return;
		}
		try {
			Map<String,String> nanopubParams = new HashMap<>();
			List<ApiResponseEntry> nanopubResults = new ArrayList<>();
			nanopubParams.put(paramName, param);
			nanopubResults = QueryAccess.get(queryId, nanopubParams).getData();
			while (!nanopubResults.isEmpty() && nanopubs.size() < 20) {
				ApiResponseEntry resultEntry = nanopubResults.remove(0);
				String npUri = resultEntry.get("np");
				nanopubs.add(new NanopubElement(npUri));
			}
		} catch (CsvValidationException | IOException ex) {
			ex.printStackTrace();
		}
		String cacheId = queryName + " " + param;
		cachedNanopubLists.put(cacheId, nanopubs);
		lastRefresh.put(cacheId, System.currentTimeMillis());
	}

	public static synchronized List<NanopubElement> retrieveNanopubList(String queryName, String param) {
		long timeNow = System.currentTimeMillis();
		String cacheId = queryName + " " + param;
		boolean isCached = false;
		boolean needsRefresh = true;
		if (cachedNanopubLists.containsKey(cacheId)) {
			long cacheAge = timeNow - lastRefresh.get(cacheId);
			isCached = cacheAge < 24 * 60 * 60 * 1000;
			needsRefresh = cacheAge > 60 * 1000;
		}
		if (needsRefresh && !isRunning(cacheId)) {
			refreshStart.put(cacheId, timeNow);
			new Thread() {

				@Override
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					try {
						ApiCache.updateNanopubList(queryName, param);
					} finally {
						refreshStart.remove(cacheId);
					}
				}

			}.start();
		}
		if (isCached) {
			return cachedNanopubLists.get(cacheId);
		} else {
			return null;
		}
	}

}
