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
		IModel<String> model = textFields.get(iri);
		if (model == null) {
			model = Model.of("");
			textFields.put(iri, model);
		}
		TextField<String> textfield = new TextField<>("textfield", model);
		add(textfield);
	}

}
