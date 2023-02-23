package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ApiResponseEntry implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<String,String> data = new HashMap<>();

	public ApiResponseEntry() {
	}

	public void add(String key, String value) {
		data.put(key, value);
	}

	public String get(String key) {
		return data.get(key);
	}

	public boolean getAsBoolean(String key) {
		String v = data.get(key);
		return v.equals("1") || v.equals("true");
	}


	public static class DataComparator implements Comparator<ApiResponseEntry> {

		public int compare(ApiResponseEntry e1, ApiResponseEntry e2) {
			String d1 = e1.get("date");
			String d2 = e2.get("date");
			if (d1 == null && d2 == null) return 0;
			if (d1 == null) return 1;
			if (d2 == null) return -1;
			return d2.compareTo(d1);
		}
		
	}

}
