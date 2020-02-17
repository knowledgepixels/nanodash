package org.petapico;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public class RestrictedChoiceItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public RestrictedChoiceItem(String id, IRI iri, final PublishForm form) {
		super(id);
		DropDownChoice<Value> dropdown = new DropDownChoice<Value>("dropdown", new Model<Value>(), form.template.getPossibleValues(iri));
		add(dropdown);
	}

}
