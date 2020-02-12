package org.petapico;

import java.util.Map;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

public class TextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public TextfieldItem(String id, IRI iri, Map<IRI,IModel<String>> textFields) {
		super(id);
		TextField<String> textfield = new TextField<>("textfield", Model.of(""));
		textFields.put(iri, textfield.getModel());
		add(textfield);
	}

}
