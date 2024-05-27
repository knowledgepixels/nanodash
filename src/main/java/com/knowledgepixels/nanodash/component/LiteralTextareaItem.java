package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.TemplateContext;

public class LiteralTextareaItem extends LiteralTextfieldItem {
	
	private static final long serialVersionUID = 1L;
	private TextArea<String> textarea;

	public LiteralTextareaItem(String id, final IRI iri, boolean optional, TemplateContext context) {
		super(id, iri, optional, context);
	}
	protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
		textarea = new TextArea<>("textarea", model);
		return textarea;
	}

	protected AbstractTextComponent<String> getTextComponent() {
		return textarea;
	}

	public String toString() {
		return "[Long literal textfield item]";
	}

}
