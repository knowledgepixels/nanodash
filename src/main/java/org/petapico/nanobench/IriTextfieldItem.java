package org.petapico.nanobench;

import java.net.URISyntaxException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import net.trustyuri.TrustyUriUtils;

public class IriTextfieldItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;

	private String prefix;
	private PublishFormContext context;
	private TextField<String> textfield;
	private IRI iri;

	public IriTextfieldItem(String id, String parentId, final IRI iriP, boolean optional, final PublishFormContext context) {
		super(id);
		this.context = context;
		this.iri = iriP;
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
		prefix = template.getPrefix(iri);
		if (prefix == null) prefix = "";
		if (template.isLocalResource(iri)) {
			prefix = iri.stringValue().replaceFirst("^(.*[/#]).*$", "$1");
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
		String prefixTooltip = prefix;
		if (!prefix.isEmpty()) {
			prefixTooltip += "...";
			if (template.isLocalResource(iri)) {
				prefixTooltip = "local:...";
			}
		}
		add(new Label("prefixtooltiptext", prefixTooltip));
		textfield = new TextField<>("textfield", model);
		if (!optional) textfield.setRequired(true);
		if (template.isLocalResource(iri)) {
			textfield.add(new AttributeAppender("style", "width:250px;"));
		}
		textfield.add(new Validator(iri, template, prefix));
		context.getFormComponents().add(textfield);
		if (template.getLabel(iri) != null) {
			textfield.add(new AttributeModifier("placeholder", template.getLabel(iri)));
			textfield.setLabel(Model.of(template.getLabel(iri)));
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

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v instanceof IRI) {
			String vs = v.stringValue();
			if (vs.startsWith(prefix)) vs = vs.substring(prefix.length());
			if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:[#/]?", "");
			Validatable<String> validatable = new Validatable<>(vs);
			if (context.getTemplate().isLocalResource(iri)) {
				vs = vs.replaceFirst("^.*[/#](.*)$", "$1");
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
			textfield.setModelObject(vs.substring(prefix.length()));
		} else if (vs.startsWith("local:")) {
			textfield.setModelObject(vs.replaceFirst("^local:[#/]?", ""));
		} else {
			textfield.setModelObject(vs);
		}
	}


	protected static class Validator implements IValidator<String> {

		private static final long serialVersionUID = 1L;

		private IRI iri;
		private Template template;
		private String prefix;

		public Validator(IRI iri, Template template, String prefix) {
			this.iri = iri;
			this.template = template;
			this.prefix = prefix;
		}

		@Override
		public void validate(IValidatable<String> s) {
			String p = prefix;
			if (s.getValue().matches("(https?|file)://.+")) p = "";
			try {
				ParsedIRI piri = new ParsedIRI(p + s.getValue());
				if (!piri.isAbsolute()) {
					s.error(new ValidationError("IRI not well-formed"));
				}
				if (p.isEmpty() && !(s.getValue()).matches("(https?|file)://.+")) {
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

	}

}
