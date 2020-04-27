package org.petapico.nanobench;

import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public StatementItem(String id, IRI subj, IRI pred, IRI obj, final PublishForm form) {
		super(id);

		add(new ValueItem("subj", subj, false, form));
		add(new ValueItem("pred", pred, false, form));
		add(new ValueItem("obj", obj, false, form));
	}

}
