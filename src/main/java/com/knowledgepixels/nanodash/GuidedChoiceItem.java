package com.knowledgepixels.nanodash;

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
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import com.knowledgepixels.nanodash.IriTextfieldItem.Validator;

public class GuidedChoiceItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;
	private PublishFormContext context;
	private Select2Choice<String> textfield;
	private ExternalLink tooltipLink;
	private Label tooltipDescription;
	private IRI iri;
	private IModel<String> model;

	private String prefix;

	// TODO: This map being static could mix up labels if the same URI is described at different places:
	// TODO: This should maybe go into a different class?
	private static Map<String,String> labelMap = new HashMap<>();

	public static String getLabel(String value) {
		return labelMap.get(value);
	}

	public static String setLabel(String value, String label) {
		return labelMap.put(value, label);
	}

	private String getChoiceLabel(String choiceId) {
		Template template = context.getTemplate();
		String label = null;
		if (choiceId.matches("(https?|file)://.+") && template.getLabel(vf.createIRI(choiceId)) != null) {
			label = template.getLabel(vf.createIRI(choiceId));
		} else if (labelMap.containsKey(choiceId)) {
			label = labelMap.get(choiceId);
			if (label.length() > 160) label = label.substring(0, 157) + "...";
		}
		return label;
	}

	public GuidedChoiceItem(String id, String parentId, final IRI iriP, boolean optional, final PublishFormContext context) {
		super(id);
		this.context = context;
		this.iri = iriP;
		final Template template = context.getTemplate();
		model = context.getFormComponentModels().get(iri);
		if (model == null) {
			model = Model.of("");
			context.getFormComponentModels().put(iri, model);
		}
		String postfix = Utils.getUriPostfix(iri);
		if (context.hasParam(postfix)) {
			model.setObject(context.getParam(postfix));
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

			@Override
			public String getDisplayValue(String choiceId) {
				if (choiceId == null || choiceId.isEmpty()) return "";
				String label = getChoiceLabel(choiceId);
				if (label == null || label.isBlank()) {
					return choiceId;
				}
				return label + " (" + choiceId + ")";
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
				Map<String,Boolean> alreadyAddedMap = new HashMap<>();
				term = term.toLowerCase();
				for (String s : possibleValues) {
					if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
						response.add(s);
						alreadyAddedMap.put(s, true);
					}
				}
				for (String v : context.getTemplate().getPossibleValuesFromApi(iri, term, labelMap)) {
					if (!alreadyAddedMap.containsKey(v)) response.add(v);
				}
			}

			@Override
			public Collection<String> toChoices(Collection<String> ids) {
				return ids;
			}

		};
		textfield = new Select2Choice<String>("textfield", model, choiceProvider);
		textfield.getSettings().getAjax(true).setDelay(500);
		textfield.getSettings().setCloseOnSelect(true);
		textfield.getSettings().setTags(true);
		String placeholder = template.getLabel(iri);
		if (placeholder == null) placeholder = "";
		textfield.getSettings().setPlaceholder(placeholder);
		Utils.setSelect2ChoiceMinimalEscapeMarkup(textfield);
		textfield.getSettings().setAllowClear(true);

		if (!optional) textfield.setRequired(true);
		textfield.add(new AttributeAppender("style", "width:750px;"));
		textfield.add(new Validator(iri, template, prefix));
		context.getFormComponents().add(textfield);

		tooltipDescription = new Label("description", new IModel<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				String obj = GuidedChoiceItem.this.getModel().getObject();
				if (obj == null || obj.isEmpty()) return "choose a value";
				String label = getChoiceLabel(GuidedChoiceItem.this.getModel().getObject());
				if (label == null || !label.contains(" - ")) return "";
				return label.substring(label.indexOf(" - ") + 3);
			}

		});
		tooltipDescription.setOutputMarkupId(true);
		add(tooltipDescription);

		tooltipLink = new ExternalLink("uri", model, model);
		tooltipLink.setOutputMarkupId(true);
		add(tooltipLink);

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
				target.add(tooltipLink);
				target.add(tooltipDescription);
			}

		});
		add(textfield);
	}

	public IModel<String> getModel() {
		return model;
	}

	@Override
	public void removeFromContext() {
		context.getFormComponents().remove(textfield);
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v instanceof IRI) {
			String vs = v.stringValue();
			if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
			if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
			Validatable<String> validatable = new Validatable<>(vs);
			if (context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
				vs = Utils.getUriPostfix(vs);
			}
			new Validator(iri, context.getTemplate(), prefix).validate(validatable);
			if (!validatable.isValid()) {
				return false;
			}
			if (textfield.getModelObject().isEmpty()) {
				return true;
			}
			return vs.equals(textfield.getModelObject());
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		String vs = v.stringValue();
		if (prefix != null && vs.startsWith(prefix)) {
			vs = vs.substring(prefix.length());
		} else if (vs.startsWith("local:")) {
			vs = vs.replaceFirst("^local:", "");
		}
		textfield.setModelObject(vs);
		if (!labelMap.containsKey(vs)) {
			context.getTemplate().getPossibleValuesFromApi(iri, vs, labelMap);
		}
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public String toString() {
		return "[Guided choiced item: " + iri + "]";
	}

}
