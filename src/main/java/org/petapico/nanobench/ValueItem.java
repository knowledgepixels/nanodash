package org.petapico.nanobench;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public class ValueItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	private ContextComponent component;

	public ValueItem(String id, IRI iri, StatementItem.RepetitionGroup s) {
		super(id);
		final Template template = s.getContext().getTemplate();
		if (template.isRestrictedChoicePlaceholder(iri)) {
			component = new RestrictedChoiceItem("value", id, iri, s.isOptional(), s.getContext());
		} else if (template.isGuidedChoicePlaceholder(iri)) {
			component = new GuidedChoiceItem("value", id, iri, s.isOptional(), s.getContext());
		} else if (template.isUriPlaceholder(iri)) {
			component = new IriTextfieldItem("value", id, iri, s.isOptional(), s.getContext());
		} else if (template.isLiteralPlaceholder(iri)) {
			component = new LiteralTextfieldItem("value", iri, s.isOptional(), s.getContext());
		} else {
			component = new IriItem("value", id, iri, id.equals("obj"), s);
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

}
