package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class QueryParamField extends Panel {

	private static final long serialVersionUID = 1L;

	private final TextField<String> textfield;
	private final String paramId;

	public QueryParamField(String id, String paramId) {
		super(id);
		this.paramId = paramId;
		add(new Label("paramname", paramId));
		textfield = new TextField<>("textfield", Model.of(""));
		add(textfield);
	}

	public String getValue() {
		return textfield.getModelObject();
	}

	public String getParamId() {
		return paramId;
	}

	public String getParamName() {
		return paramId.replaceFirst("^_+", "").replaceFirst("_iri$", "");
	}

}
