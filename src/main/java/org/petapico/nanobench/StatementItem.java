package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public StatementItem(String id, IRI subj, IRI pred, IRI obj, PublishFormContext context) {
		super(id);

		add(new ValueItem("subj", subj, false, context));
		add(new ValueItem("pred", pred, false, context));
		add(new ValueItem("obj", obj, false, context));
	}

	public StatementItem(String id, IRI subj, IRI pred, Value obj) {
		super(id);

		add(new Link("subj", subj.stringValue()));
		add(new Link("pred", pred.stringValue()));
		if (obj instanceof IRI) {
			add(new Link("obj", obj.stringValue()));
		} else {
			add(new Label("obj", "\"" + obj.stringValue() + "\""));
		}
	}

}
