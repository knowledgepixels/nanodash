package org.petapico.nanobench;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

public class LiteralTextfieldItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;
	private PublishFormContext context;
	private AbstractTextComponent<String> textfield;
	private final String regex;

	public LiteralTextfieldItem(String id, final IRI iri, boolean optional, PublishFormContext context) {
		super(id);
		this.context = context;
		final Template template = context.getTemplate();
		regex = template.getRegex(iri);
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
		AbstractTextComponent<String> tc = initTextComponent(model);
		if (!optional) tc.setRequired(true);
		if (context.getTemplate().getLabel(iri) != null) {
			tc.add(new AttributeModifier("placeholder", context.getTemplate().getLabel(iri)));
		}
		tc.add(new IValidator<String>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void validate(IValidatable<String> s) {
				if (regex != null) {
					if (!s.getValue().matches(regex)) {
						s.error(new ValidationError("Value '" + s.getValue() + "' doesn't match the pattern '" + regex + "'"));
					}
				}
			}

		});
		context.getFormComponentModels().put(iri, tc.getModel());
		context.getFormComponents().add(tc);
		tc.add(new ValueItem.KeepValueAfterRefreshBehavior());
		tc.add(new InvalidityHighlighting());
		add(tc);
	}

	protected AbstractTextComponent<String> initTextComponent(IModel<String> model) {
		textfield = new TextField<>("textfield", model);
		return textfield;
	}

	protected AbstractTextComponent<String> getTextComponent() {
		return textfield;
	}

	@Override
	public void removeFromContext() {
		context.getFormComponents().remove(getTextComponent());
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v instanceof Literal) {
			if (regex != null && !v.stringValue().matches(regex)) {
				return false;
			}
			if (getTextComponent().getModelObject().isEmpty()) {
				return true;
			}
			return v.stringValue().equals(getTextComponent().getModelObject());
		}
		return false;
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		getTextComponent().setModelObject(v.stringValue());
	}

	public String toString() {
		return "[Literal textfield item]";
	}

}
