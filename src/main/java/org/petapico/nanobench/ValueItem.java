package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	private List<ContextComponent> components = new ArrayList<>();

	public ValueItem(String id, IRI iri, StatementItem.RepetitionGroup s) {
		super(id);
		final Template template = s.getContext().getTemplate();
		if (template.isUriPlaceholder(iri)) {
			IriTextfieldItem item = new IriTextfieldItem("value", id, iri, s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isLiteralPlaceholder(iri)) {
			LiteralTextfieldItem item = new LiteralTextfieldItem("value", iri, s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			RestrictedChoiceItem item = new RestrictedChoiceItem("value", id, iri, s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isGuidedChoicePlaceholder(iri)) {
			GuidedChoiceItem item = new GuidedChoiceItem("value", id, iri, s.isOptional(), s.getContext());
			add(item);
		} else {
			add(new IriItem("value", id, iri, id.equals("obj"), s));
		}
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
		for (ContextComponent c : components) {
			c.removeFromContext();
		}
	}

}
