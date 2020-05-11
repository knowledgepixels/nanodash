package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

public class RestrictedChoiceItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public RestrictedChoiceItem(String id, IRI iri, boolean optional, final PublishForm form) {
		super(id);
		IModel<String> model = form.formComponentModels.get(iri);
		if (model == null) {
			model = Model.of("");
			form.formComponentModels.put(iri, model);
		}
		final List<String> dropdownValues = new ArrayList<>();
		for (Value v : form.template.getPossibleValues(iri)) {
			dropdownValues.add(v.toString());
		}
		ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(String object) {
				if (object == null || object.isEmpty()) return "";
				IRI valueIri = vf.createIRI(object);
				if (form.template.getLabel(valueIri) != null) {
					return form.template.getLabel(valueIri);
				} else {
					return IriItem.getShortNameFromURI(object);
				}
			}

			@Override
			public String getIdValue(String object) {
				return object;
			}

			// Getting strange errors with Tomcat if this method is not overridden:
			@Override
			public void detach() {
			}

			@Override
			public void query(String term, int page, Response<String> response) {
				for (String s : dropdownValues) {
					if (term == null || s.toLowerCase().contains(term.toLowerCase())) response.add(s);
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		Select2Choice<String> choice = new Select2Choice<String>("choice", model, choiceProvider);
		if (!optional) choice.setRequired(true);
		choice.getSettings().setCloseOnSelect(true);
		form.formComponents.add(choice);
		add(choice);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
