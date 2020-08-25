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

}
