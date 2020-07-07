package org.petapico.nanobench;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {

	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri, boolean optional, PublishFormContext context) {
		super(id);
		final Template template = context.getTemplate();
		if (template.isUriPlaceholder(iri)) {
			add(new IriTextfieldItem("value", id, iri, optional, context));
		} else if (template.isLiteralPlaceholder(iri)) {
			add(new LiteralTextfieldItem("value", iri, optional, context));
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			add(new RestrictedChoiceItem("value", id, iri, optional, context));
		} else if (template.isGuidedChoicePlaceholder(iri)) {
			add(new GuidedChoiceItem("value", id, iri, optional, context));
		} else {
			add(new IriItem("value", id, iri, id.equals("obj"), context));
		}
	}

	public static class KeepValueAfterRefreshBehavior extends OnChangeAjaxBehavior {

		private static final long serialVersionUID = 1L;

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			// No actual action needed here; Ajax request alone ensures values are kept after refreshing.
		}

	}

}
