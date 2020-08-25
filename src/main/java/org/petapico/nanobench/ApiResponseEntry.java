package org.petapico.nanobench;

import java.io.Serializable;
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

}
