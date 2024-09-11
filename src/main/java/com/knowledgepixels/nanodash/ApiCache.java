package com.knowledgepixels.nanodash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nanopub.extra.services.ApiResponseEntry;

public class ApiCache {

	private ApiCache() {}  // no instances allowed

	private transient static Map<String,List<NanopubElement>> cachedNanopubLists = new HashMap<>();
	private transient static Map<String,Map<String,String>> cachedMaps = new HashMap<>();
	private transient static Map<String,Long> lastRefresh = new HashMap<>();
	private transient static Map<String,Long> refreshStart = new HashMap<>();

	private static boolean isRunning(String cacheId) {
		if (!refreshStart.containsKey(cacheId)) return false;
		return System.currentTimeMillis() - refreshStart.get(cacheId) < 60 * 1000;
	}

	public static boolean isRunning(String queryName, Map<String,String> params) {
		return isRunning(getCacheId(queryName, params));
	}

	public static boolean isRunning(String queryName, String paramName, String paramValue) {
		Map<String,String> params = new HashMap<>();
		params.put(paramName, paramValue);
		return isRunning(getCacheId(queryName, params));
	}

	private static void updateNanopubList(String queryName, Map<String,String> params) {
		List<NanopubElement> nanopubs = new ArrayList<>();
		Map<String,String> nanopubParams = new HashMap<>();
		List<ApiResponseEntry> nanopubResults = new ArrayList<>();
		for (String k : params.keySet()) nanopubParams.put(k, params.get(k));
		nanopubResults = QueryApiAccess.get(queryName, nanopubParams).getData();
		while (nanopubResults != null && !nanopubResults.isEmpty()) {
			ApiResponseEntry resultEntry = nanopubResults.remove(0);
			String npUri = resultEntry.get("np");
			nanopubs.add(new NanopubElement(npUri));
		}
		String cacheId = getCacheId(queryName, params);
		cachedNanopubLists.put(cacheId, nanopubs);
		lastRefresh.put(cacheId, System.currentTimeMillis());
	}

	public static List<NanopubElement> retrieveNanopubList(String queryName, String paramName, String paramValue) {
		Map<String,String> params = new HashMap<>();
		params.put(paramName, paramValue);
		return retrieveNanopubList(queryName, params);
	}

	public static synchronized List<NanopubElement> retrieveNanopubList(String queryName, Map<String,String> params) {
		long timeNow = System.currentTimeMillis();
		String cacheId = getCacheId(queryName, params);
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
						Thread.sleep(100 + new Random().nextLong(400));
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					try {
						ApiCache.updateNanopubList(queryName, params);
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

	private static void updateMap(String queryName, Map<String,String> params) {
		Map<String,String> map = new HashMap<>();
		Map<String,String> nanopubParams = new HashMap<>();
		List<ApiResponseEntry> result = new ArrayList<>();
		for (String k : params.keySet()) nanopubParams.put(k, params.get(k));
		result = QueryApiAccess.get(queryName, nanopubParams).getData();
		while (result != null && !result.isEmpty()) {
			ApiResponseEntry resultEntry = result.remove(0);
			map.put(resultEntry.get("key"), resultEntry.get("value"));
		}
		String cacheId = getCacheId(queryName, params);
		cachedMaps.put(cacheId, map);
		lastRefresh.put(cacheId, System.currentTimeMillis());
	}

	public static synchronized Map<String,String> retrieveMap(String queryName, Map<String,String> params) {
		long timeNow = System.currentTimeMillis();
		String cacheId = getCacheId(queryName, params);
		boolean isCached = false;
		boolean needsRefresh = true;
		if (cachedMaps.containsKey(cacheId)) {
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
						Thread.sleep(100 + new Random().nextLong(400));
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					try {
						ApiCache.updateMap(queryName, params);
					} finally {
						refreshStart.remove(cacheId);
					}
				}

			}.start();
		}
		if (isCached) {
			return cachedMaps.get(cacheId);
		} else {
			return null;
		}
	}

	private static String paramsToString(Map<String,String> params) {
		List<String> keys = new ArrayList<>(params.keySet());
		Collections.sort(keys);
		String s = "";
		for (String k : keys) s += " " + k + "=" + params.get(k);
		return s;
	}

	public static String getCacheId(String queryName, Map<String,String> params) {
		return queryName + " " + paramsToString(params);
	}

}
