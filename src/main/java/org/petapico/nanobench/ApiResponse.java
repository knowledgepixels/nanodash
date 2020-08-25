package org.petapico.nanobench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ApiResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private String[] header;
	private List<ApiResponseEntry> data = new ArrayList<>();

	public ApiResponse() {
	}

	public ApiResponse(Collection<ApiResponseEntry> entries) {
		data.addAll(entries);
	}

	public void setHeader(String[] header) {
		this.header = header;
	}

	public void add(ApiResponseEntry entry) {
		data.add(entry);
	}

	public void add(String[] line) {
		ApiResponseEntry entry = new ApiResponseEntry();
		for (int i = 0 ; i < line.length ; i++) {
			entry.add(header[i], line[i]);
		}
		data.add(entry);
	}

	public String[] getHeader() {
		return header;
	}

	public List<ApiResponseEntry> getData() {
		return data;
	}

	public int size() {
		return data.size();
	}

}
