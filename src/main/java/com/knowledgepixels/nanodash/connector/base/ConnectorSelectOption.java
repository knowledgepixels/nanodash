package com.knowledgepixels.nanodash.connector.base;

import java.io.Serializable;

public class ConnectorSelectOption implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String typeId;
	private final String name;
	private final String explanation;

	public ConnectorSelectOption(String typeId, String name, String explanation) {
		this.typeId = typeId;
		this.name = name;
		this.explanation = explanation;
	}

	public ConnectorSelectOption(String typeId, String name) {
		this(typeId, name, null);
	}

	public String getTypeId() {
		return typeId;
	}

	public String getName() {
		return name;
	}

	public String getExplanation() {
		return explanation;
	}


	public static class Group implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String title;
		private final ConnectorSelectOption[] options;

		public Group(String title, ConnectorSelectOption... options) {
			this.title = title;
			this.options = options;
		}

		public String getTitle() {
			return title;
		}

		public ConnectorSelectOption[] getOptions() {
			return options;
		}

	}

}
