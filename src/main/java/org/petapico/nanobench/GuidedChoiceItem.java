package org.petapico.nanobench;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import net.trustyuri.TrustyUriUtils;

public class GuidedChoiceItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;
	private PublishFormContext context;
	private Select2Choice<String> textfield;

	private String prefix;

	public GuidedChoiceItem(String id, String parentId, final IRI iri, boolean optional, final PublishFormContext context) {
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
		final List<String> possibleValues = new ArrayList<>();
		for (Value v : template.getPossibleValues(iri)) {
			possibleValues.add(v.toString());
		}

		prefix = template.getPrefix(iri);
		if (prefix == null) prefix = "";
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
		String prefixTooltip = prefix;
		if (!prefix.isEmpty()) {
			prefixTooltip += "...";
		}
		add(new Label("prefixtooltiptext", prefixTooltip));

		ChoiceProvider<String> choiceProvider = new ChoiceProvider<String>() {

			private static final long serialVersionUID = 1L;

			private Map<String,String> labelMap = new HashMap<>();

			@Override
			public String getDisplayValue(String id) {
				if (id == null || id.isEmpty()) return "";
				String label = null;
				if (id.matches("(https?|file)://.+") && template.getLabel(vf.createIRI(id)) != null) {
					label = template.getLabel(vf.createIRI(id));
				} else if (labelMap.containsKey(id)) {
					label = labelMap.get(id);
				}
				if (label != null) {
					return label + " (" + id + ")";
				} else {
					return id;
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
					response.addAll(possibleValues);
					return;
				}
				term = term.toLowerCase();
				for (String s : possibleValues) {
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) response.add(s);
				}
				for (String v : context.getTemplate().getPossibleValuesFromApi(iri, term, labelMap)) {
					response.add(v);
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		textfield = new Select2Choice<String>("textfield", model, choiceProvider);
		textfield.getSettings().setCloseOnSelect(true);
		textfield.getSettings().setTags(true);
		textfield.getSettings().setPlaceholder("");
		textfield.getSettings().setAllowClear(true);

		if (!optional) textfield.setRequired(true);
		textfield.add(new AttributeAppender("style", "width:750px;"));
		textfield.add(new IValidator<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<String> s) {
				String p = prefix;
				if (s.getValue().matches("(https?|file)://.+")) p = "";
				try {
					ParsedIRI piri = new ParsedIRI(p + s.getValue());
					if (!piri.isAbsolute()) {
						s.error(new ValidationError("IRI not well-formed"));
					}
					if (p.isEmpty() && !(s.getValue().matches("(https?|file)://.+"))) {
						s.error(new ValidationError("Only http(s):// and file:// IRIs are allowed here"));
					}
				} catch (URISyntaxException ex) {
					s.error(new ValidationError("IRI not well-formed"));
				}
				String regex = template.getRegex(iri);
				if (regex != null) {
					if (!s.getValue().matches(regex)) {
						s.error(new ValidationError("Value '" + s.getValue() + "' doesn't match the pattern '" + regex + "'"));
					}
				}
				if (template.isTrustyUriPlaceholder(iri)) {
					if (!TrustyUriUtils.isPotentialTrustyUri(p + s.getValue())) {
						s.error(new ValidationError("Not a trusty URI"));
					}
				}
			}

		});
		context.getFormComponents().add(textfield);
		if (template.getLabel(iri) != null) {
			textfield.getSettings().setPlaceholder(template.getLabel(iri));
		}
		textfield.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FormComponent<String> fc : context.getFormComponents()) {
					if (fc == textfield) continue;
					if (fc.getModel() == textfield.getModel()) {
						fc.modelChanged();
						target.add(fc);
					}
				}
			}

		});
		add(textfield);
	}

	@Override
	public void removeFromContext() {
		context.getFormComponents().remove(textfield);
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

}
