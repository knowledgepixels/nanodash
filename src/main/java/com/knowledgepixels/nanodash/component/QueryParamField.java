package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;

public class QueryParamField extends Panel {

	private static final long serialVersionUID = 1L;

	private TextField<String> textfield;

	public QueryParamField(String id, String paramName) {
		super(id);
		add(new Label("paramname", paramName));
		textfield = new TextField<>("textfield");
		add(textfield);
	}

	public String getValue() {
		return textfield.getModelObject();
	}

}
