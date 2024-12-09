package com.knowledgepixels.nanodash.connector;

import java.io.Serializable;

public class ConnectorOptionGroup implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String title;
	private final ConnectorOption[] options;

	public ConnectorOptionGroup(String title, ConnectorOption... options) {
		this.title = title;
		this.options = options;
	}

	public String getTitle() {
		return title;
	}

	public ConnectorOption[] getOptions() {
		return options;
	}

}
