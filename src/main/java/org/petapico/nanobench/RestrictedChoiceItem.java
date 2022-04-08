package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
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
	private IRI iri;
	private Select2Choice<String> choice;
	private Template template;
	private final Map<String,Boolean> fixedPossibleValues = new HashMap<>();
	private final List<IRI> possibleRefValues = new ArrayList<>();

	public RestrictedChoiceItem(String id, String parentId, IRI iri, boolean optional, final PublishFormContext context) {
		super(id);
		this.context = context;
		this.iri = iri;
		template = context.getTemplate();
		IModel<String> model = context.getFormComponentModels().get(iri);
		if (model == null) {
			model = Model.of("");
			context.getFormComponentModels().put(iri, model);
		}
		String postfix = iri.stringValue().replaceFirst("^.*[/#](.*)$", "$1");
		if (context.hasParam(postfix)) {
			model.setObject(context.getParam(postfix));
		}
		for (Value v : template.getPossibleValues(iri)) {
			if (v instanceof IRI && template.isPlaceholder((IRI) v)) {
				possibleRefValues.add((IRI) v);
			} else {
				fixedPossibleValues.put(v.toString(), true);
			}
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
				if (!object.matches("(https?|file)://.+")) return object;
				IRI valueIri = vf.createIRI(object);
				if (fixedPossibleValues.containsKey(object) && template.getLabel(valueIri) != null) {
					return template.getLabel(valueIri);
				}
				if (object.startsWith(template.getId())) return object.substring(0, template.getId().length());
				return object;
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
				List<String> possibleValues = getPossibleValues();
				
				if (term == null) {
					response.addAll(possibleValues);
					return;
				}
				term = term.toLowerCase();
				for (String s : possibleValues) {
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
		if (template.isLocalResource(iri)) {
			choice.add(new AttributeAppender("style", "width:250px;"));
		}
		choice.getSettings().setCloseOnSelect(true);
		String placeholder = template.getLabel(iri);
		if (placeholder == null) placeholder = "";
		choice.getSettings().setPlaceholder(placeholder);
		choice.getSettings().setAllowClear(true);
		choice.add(new ValueItem.KeepValueAfterRefreshBehavior());
		choice.add(new Validator());
		context.getFormComponents().add(choice);
		choice.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FormComponent<String> fc : context.getFormComponents()) {
					if (fc == choice) continue;
					if (fc.getModel() == choice.getModel()) {
						fc.modelChanged();
						target.add(fc);
					}
				}
			}

		});
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
			String vs = v.stringValue();
			if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
			if (possibleRefValues.size() == 0 && !fixedPossibleValues.containsKey(vs)) {
				return false;
			}
			if (choice.getModelObject().isEmpty()) {
				return true;
			}
			return vs.equals(choice.getModelObject());
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		String vs = v.stringValue();
		if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
		if (!isUnifiableWith(v)) throw new UnificationException(vs);
		choice.setModelObject(vs);
	}

	public String toString() {
		return "[Restricted choice item: " + iri + "]";
	}

	public List<String> getPossibleValues() {
		Set<String> possibleValues = new HashSet<>();
		possibleValues.addAll(fixedPossibleValues.keySet());
		for (IRI r : possibleRefValues) {
			for (int i = 0 ; true ; i++) {
				String suffix = "__" + i;
				if (i == 0) suffix = "";
				IRI refIri = vf.createIRI(r.stringValue() + suffix);
				IModel<String> m = context.getFormComponentModels().get(refIri);
				if (m == null) break;
				if (m.getObject() != null && !m.getObject().startsWith("\"")) {
					possibleValues.add(m.getObject());
				}
			}
		}
		List<String> possibleValuesList = new ArrayList<>();
		for (String s : possibleValues) {
			if (template.isLocalResource(iri)) {
				if (s.matches("(https?|file)://.+")) continue;
			}
			possibleValuesList.add(s);
		}
		Collections.sort(possibleValuesList);
		return possibleValuesList;
	}


	protected class Validator extends InvalidityHighlighting implements IValidator<String> {

		private static final long serialVersionUID = 1L;

		public Validator() {
		}

		@Override
		public void validate(IValidatable<String> s) {
			if (!getPossibleValues().contains(s.getValue())) {
				s.error(new ValidationError("Invalid choice"));
			}
		}

	}

}
