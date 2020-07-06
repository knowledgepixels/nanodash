package org.petapico.nanobench;

import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class OptionalStatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public OptionalStatementItem(String id, IRI subj, IRI pred, IRI obj, PublishFormContext context) {
		super(id);

		add(new ValueItem("subj", subj, true, context));
		add(new ValueItem("pred", pred, true, context));
		add(new ValueItem("obj", obj, true, context));
	}

}
