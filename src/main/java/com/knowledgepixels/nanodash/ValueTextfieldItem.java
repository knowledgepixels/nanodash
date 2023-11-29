package com.knowledgepixels.nanodash;

import java.net.URISyntaxException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

public class ValueTextfieldItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	private TemplateContext context;
	private TextField<String> textfield;
	private IRI iri;

	public ValueTextfieldItem(String id, String parentId, final IRI iriP, boolean optional, final TemplateContext context) {
		super(id);
		this.context = context;
		this.iri = iriP;
		final Template template = context.getTemplate();
		IModel<String> model = context.getComponentModels().get(iri);
		if (model == null) {
			model = Model.of("");
			context.getComponentModels().put(iri, model);
		}
		String postfix = Utils.getUriPostfix(iri);
		if (context.hasParam(postfix)) {
			model.setObject(context.getParam(postfix));
		}
		textfield = new TextField<>("textfield", model);
		if (!optional) textfield.setRequired(true);
		textfield.add(new Validator(iri, template));
		context.getComponents().add(textfield);
		if (template.getLabel(iri) != null) {
			textfield.add(new AttributeModifier("placeholder", template.getLabel(iri)));
			textfield.setLabel(Model.of(template.getLabel(iri)));
		}
		textfield.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (Component c : context.getComponents()) {
					if (c == textfield) continue;
					if (c.getDefaultModel() == textfield.getModel()) {
						c.modelChanged();
						target.add(c);
					}
				}
			}

		});
		add(textfield);

		try {
			unifyWith(template.getDefault(iri));
		} catch (UnificationException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void removeFromContext() {
		context.getComponents().remove(textfield);
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v == null) return true;
		String vs = v.stringValue();
		if (v instanceof Literal) vs = "\"" + vs.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"") + "\"";
		if (vs.startsWith("local:")) vs = vs.replaceFirst("^local:", "");
		Validatable<String> validatable = new Validatable<>(vs);
		if (v instanceof IRI && context.getTemplate().isLocalResource(iri) && !Utils.isUriPostfix(vs)) {
			vs = Utils.getUriPostfix(vs);
		}
		new Validator(iri, context.getTemplate()).validate(validatable);
		if (!validatable.isValid()) {
			return false;
		}
		if (textfield.getModelObject().isEmpty()) {
			return true;
		}
		return vs.equals(textfield.getModelObject());
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (v == null) return;
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		String vs = v.stringValue();
		if (vs.startsWith("local:")) {
			textfield.setModelObject(vs.replaceFirst("^local:", ""));
		} else if (v instanceof IRI) {
			textfield.setModelObject(vs);
		} else {
			textfield.setModelObject("\"" + vs.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"") + "\"");
		}
	}


	protected static class Validator extends InvalidityHighlighting implements IValidator<String> {

		private static final long serialVersionUID = 1L;

//		private IRI iri;
//		private Template template;

		public Validator(IRI iri, Template template) {
//			this.iri = iri;
//			this.template = template;
		}

		@Override
		public void validate(IValidatable<String> s) {
			if (s.getValue().startsWith("\"")) {
				if (!s.getValue().matches("\"([^\\\\\\\"]|\\\\\\\\|\\\\\")*\"")) {
					s.error(new ValidationError("Invalid literal value"));
				}
				return;
			}
			String p = "";
			if (s.getValue().matches("[^:# ]+")) p = "local:";
			try {
				ParsedIRI piri = new ParsedIRI(p + s.getValue());
				if (!piri.isAbsolute()) {
					s.error(new ValidationError("IRI not well-formed"));
				}
				if (p.isEmpty() && !s.getValue().startsWith("local:") && !(s.getValue()).matches("https?://.+")) {
					s.error(new ValidationError("Only http(s):// IRIs are allowed here"));
				}
			} catch (URISyntaxException ex) {
				s.error(new ValidationError("IRI not well-formed"));
			}
		}

	}

	@Override
	public void fillFinished() {
	}

	public String toString() {
		return "[value textfield item: " + iri + "]";
	}

}
