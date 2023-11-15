package com.knowledgepixels.nanodash;

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
	private TemplateContext context;
	private AbstractTextComponent<String> textfield;
	private final String regex;

	public LiteralTextfieldItem(String id, final IRI iri, boolean optional, TemplateContext context) {
		super(id);
		this.context = context;
		final Template template = context.getTemplate();
		regex = template.getRegex(iri);
		IModel<String> model = context.getComponentModels().get(iri);
		if (model == null) {
			model = Model.of("");
			context.getComponentModels().put(iri, model);
		}
		String postfix = Utils.getUriPostfix(iri);
		if (context.hasParam(postfix)) {
			model.setObject(context.getParam(postfix));
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
		context.getComponentModels().put(iri, tc.getModel());
		context.getComponents().add(tc);
		tc.add(new ValueItem.KeepValueAfterRefreshBehavior());
		tc.add(new InvalidityHighlighting());
		add(tc);

		try {
			unifyWith(template.getDefault(iri));
		} catch (UnificationException ex) {
			ex.printStackTrace();
		}
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
		context.getComponents().remove(getTextComponent());
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v == null) return true;
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
		if (v == null) return;
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		getTextComponent().setModelObject(v.stringValue());
	}

	public String toString() {
		return "[Literal textfield item]";
	}

}
