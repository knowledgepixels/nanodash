package org.petapico;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
		IChoiceRenderer<String> choiceRenderer = new IChoiceRenderer<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Object getDisplayValue(String object) {
				IRI valueIri = vf.createIRI(object);
				if (form.template.getLabel(valueIri) != null) {
					return form.template.getLabel(valueIri);
				} else {
					return IriItem.getShortNameFromURI(object);
				}
			}

			@Override
			public String getIdValue(String object, int index) {
				return object;
			}

			@Override
			public String getObject(String id, IModel<? extends List<? extends String>> choices) {
				return id;
			}

		};
		DropDownChoice<String> dropdown = new DropDownChoice<String>("dropdown", model, dropdownValues, choiceRenderer);
		dropdown.setRequired(true);
		form.formComponents.add(dropdown);
		add(dropdown);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
