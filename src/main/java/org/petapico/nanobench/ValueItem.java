package org.petapico.nanobench;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class ValueItem extends Panel {

	private static final long serialVersionUID = 1L;

	private StatementItem.RepetitionGroup s;

	public ValueItem(String id, IRI iri, StatementItem.RepetitionGroup s) {
		super(id);
		this.s = s;
		final Template template = s.getContext().getTemplate();
		if (template.isUriPlaceholder(iri)) {
			add(new IriTextfieldItem("value", id, transform(iri), s.isOptional(), s.getContext()));
		} else if (template.isLiteralPlaceholder(iri)) {
			add(new LiteralTextfieldItem("value", transform(iri), s.isOptional(), s.getContext()));
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			add(new RestrictedChoiceItem("value", id, transform(iri), s.isOptional(), s.getContext()));
		} else if (template.isGuidedChoicePlaceholder(iri)) {
			add(new GuidedChoiceItem("value", id, transform(iri), s.isOptional(), s.getContext()));
		} else {
			add(new IriItem("value", id, iri, id.equals("obj"), s.getContext()));
		}
	}

	private IRI transform(IRI iri) {
		if (s.getRepeatIndex() > 0 && s.getContext().hasNarrowScope(iri)) {
			// TODO: Check that this double-underscore pattern isn't used otherwise:
			return vf.createIRI(iri.stringValue() + "__" + s.getRepeatIndex());
		}
		return iri;
	}

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public static class KeepValueAfterRefreshBehavior extends OnChangeAjaxBehavior {

		private static final long serialVersionUID = 1L;

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			// No actual action needed here; Ajax request alone ensures values are kept after refreshing.
		}

	}

}
