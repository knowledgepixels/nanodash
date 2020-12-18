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
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;

import net.trustyuri.TrustyUriUtils;

public class IriTextfieldItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;

	private String prefix;
	private PublishFormContext context;
	private TextField<String> textfield;

	public IriTextfieldItem(String id, String parentId, final IRI iri, boolean optional, final PublishFormContext context) {
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

		});
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

}
