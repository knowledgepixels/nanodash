package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.Nanopub;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public StatementItem(String id, IRI subj, IRI pred, IRI obj, PublishFormContext context) {
		super(id);

		add(new ValueItem("subj", subj, false, context));
		add(new ValueItem("pred", pred, false, context));
		add(new ValueItem("obj", obj, false, context));
	}

	public StatementItem(String id, IRI subj, IRI pred, Value obj, Nanopub np) {
		super(id);

		add(new NanobenchLink("subj", subj.stringValue(), np));
		add(new NanobenchLink("pred", pred.stringValue(), np));
		if (obj instanceof IRI) {
			add(new NanobenchLink("obj", obj.stringValue(), np));
		} else {
			add(new Label("obj", "\"" + obj.stringValue() + "\""));
		}
	}

}
