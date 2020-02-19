package org.petapico;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public class RestrictedChoiceItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public RestrictedChoiceItem(String id, IRI iri, final PublishForm form) {
		super(id);
		IModel<String> model = form.formComponentModels.get(iri);
		if (model == null) {
			model = Model.of("");
			form.formComponentModels.put(iri, model);
		}
		List<String> dropdownValues = new ArrayList<>();
		for (Value v : form.template.getPossibleValues(iri)) {
			dropdownValues.add(v.toString());
		}
		DropDownChoice<String> dropdown = new DropDownChoice<String>("dropdown", model, dropdownValues);
		add(dropdown);
	}

}
