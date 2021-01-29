package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
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

public class RestrictedChoiceItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;
	private PublishFormContext context;
	private Select2Choice<String> choice;
	private final List<String> dropdownValues;

	public RestrictedChoiceItem(String id, String parentId, IRI iri, boolean optional, final PublishFormContext context) {
		super(id);
		this.context = context;
		final Template template = context.getTemplate();
		IModel<String> model = context.getFormComponentModels().get(iri);
		if (model == null) {
			String value = "";
			String postfix = iri.stringValue().replaceFirst("^.*[/#](.*)$", "$1");
			if (context.hasParam(postfix)) {
				value = context.getParam(postfix);
			}
			model = Model.of(value);
			context.getFormComponentModels().put(iri, model);
		}
		dropdownValues = new ArrayList<>();
		for (Value v : template.getPossibleValues(iri)) {
			dropdownValues.add(v.toString());
		}

		String prefixLabel = template.getPrefixLabel(iri);
		Label prefixLabelComp;
		if (prefixLabel == null) {
			prefixLabelComp = new Label("prefix", "");
			prefixLabelComp.setVisible(false);
		} else {
			if (prefixLabel.length() > 0 && parentId.equals("subj")) {
				// Capitalize first letter of label if at subject position:
				prefixLabel = prefixLabel.substring(0, 1).toUpperCase() + prefixLabel.substring(1);
			}
			prefixLabelComp = new Label("prefix", prefixLabel);
		}
		add(prefixLabelComp);

		ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getDisplayValue(String object) {
				if (object == null || object.isEmpty()) return "";
				IRI valueIri = vf.createIRI(object);
				if (template.getLabel(valueIri) != null) {
					return template.getLabel(valueIri);
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
				if (term == null) {
					response.addAll(dropdownValues);
					return;
				}
				term = term.toLowerCase();
				for (String s : dropdownValues) {
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) response.add(s);
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		choice = new Select2Choice<String>("choice", model, choiceProvider);
		if (!optional) choice.setRequired(true);
		choice.getSettings().setCloseOnSelect(true);
		choice.add(new ValueItem.KeepValueAfterRefreshBehavior());
		context.getFormComponents().add(choice);
		add(choice);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	@Override
	public void removeFromContext() {
		context.getFormComponents().remove(choice);
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v instanceof IRI) {
			if (!dropdownValues.contains(v.stringValue())) {
				return false;
			}
			if (choice.getModelObject().isEmpty()) {
				return true;
			}
			return v.stringValue().equals(choice.getModelObject());
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		choice.setModelObject(v.stringValue());
	}

}
