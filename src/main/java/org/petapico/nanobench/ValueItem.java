package org.petapico.nanobench;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class ValueItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	private StatementItem.RepetitionGroup s;
	private List<ContextComponent> components = new ArrayList<>();

	public ValueItem(String id, IRI iri, StatementItem.RepetitionGroup s) {
		super(id);
		this.s = s;
		final Template template = s.getContext().getTemplate();
		if (template.isUriPlaceholder(iri)) {
			IriTextfieldItem item = new IriTextfieldItem("value", id, transform(iri), s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isLiteralPlaceholder(iri)) {
			LiteralTextfieldItem item = new LiteralTextfieldItem("value", transform(iri), s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			RestrictedChoiceItem item = new RestrictedChoiceItem("value", id, transform(iri), s.isOptional(), s.getContext());
			components.add(item);
			add(item);
		} else if (template.isGuidedChoicePlaceholder(iri)) {
			GuidedChoiceItem item = new GuidedChoiceItem("value", id, transform(iri), s.isOptional(), s.getContext());
			add(item);
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

	@Override
	public void removeFromContext() {
		for (ContextComponent c : components) {
			c.removeFromContext();
		}
	}

}
