package org.petapico;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;

public class LiteralTextfieldItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public LiteralTextfieldItem(String id, IRI iri, final PublishForm form) {
		super(id);
		TextField<String> textfield = new TextField<>("textfield", Model.of(""));
		if (form.labelMap.containsKey(iri)) {
			textfield.add(new AttributeModifier("placeholder", form.labelMap.get(iri)));
		}
		form.textFieldModels.put(iri, textfield.getModel());
		form.textFields.add(textfield);
		add(textfield);
	}

}
