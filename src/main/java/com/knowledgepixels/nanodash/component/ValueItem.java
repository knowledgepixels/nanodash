package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.component.StatementItem.RepetitionGroup;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.UnificationException;

public class ValueItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	private ContextComponent component;
	private Value value;

	public ValueItem(String id, Value value, IRI statementPartId, RepetitionGroup rg) {
		super(id);
		this.value = value;
		final Template template = rg.getContext().getTemplate();
		if (value instanceof IRI) {
			IRI iri = (IRI) value;
			if (template.isSequenceElementPlaceholder(iri)) {
				component = new SequenceElementItem("value", iri, rg.getRepeatIndex()+1, rg.getContext());
			} else if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
				// This is a special placeholder that is always read-only
				component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
			} else if (rg.getContext().isReadOnly()) {
				if (template.isPlaceholder(iri)) {
					component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
				} else {
					component = new IriItem("value", id, iri, id.equals("obj"), statementPartId, rg);
				}
			} else if (template.isRestrictedChoicePlaceholder(iri)) {
				component = new RestrictedChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
			} else if (template.isAgentPlaceholder(iri)) {
				component = new AgentChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
			} else if (template.isGuidedChoicePlaceholder(iri)) {
				component = new GuidedChoiceItem("value", id, iri, rg.isOptional(), rg.getContext());
			} else if (template.isIntroducedResource(iri) && rg.getContext().getFillMode() == FillMode.SUPERSEDE) {
				component = new ReadonlyItem("value", id, iri, id.equals("obj"), statementPartId, rg);
			} else if (template.isUriPlaceholder(iri)) {
				component = new IriTextfieldItem("value", id, iri, rg.isOptional(), rg.getContext());
			} else if (template.isLongLiteralPlaceholder(iri)) {
				component = new LiteralTextareaItem("value", iri, rg.isOptional(), rg.getContext());
			} else if (template.isLiteralPlaceholder(iri)) {
				component = new LiteralTextfieldItem("value", iri, rg.isOptional(), rg.getContext());
			} else if (template.isPlaceholder(iri)) {
				component = new ValueTextfieldItem("value", id, iri, rg.isOptional(), rg.getContext());
			} else {
				component = new IriItem("value", id, iri, id.equals("obj"), statementPartId, rg);
			}
		} else {
			component = new LiteralItem("value", id, (Literal) value, rg);
		}
		add((Component) component);
	}

	public static class KeepValueAfterRefreshBehavior extends OnChangeAjaxBehavior {

		private static final long serialVersionUID = 1L;

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			// No actual action needed here; Ajax request alone ensures values are kept after refreshing.
		}

	}

	@Override
	public void removeFromContext() {
		component.removeFromContext();
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		return component.isUnifiableWith(v);
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		component.unifyWith(v);
	}

	@Override
	public void fillFinished() {
		component.fillFinished();
	}

	@Override
	public void finalizeValues() {
		component.finalizeValues();
	}

	public Value getValue() {
		return value;
	}

	public ContextComponent getComponent() {
		return component;
	}

	public String toString() {
		return component.toString();
	}

}
