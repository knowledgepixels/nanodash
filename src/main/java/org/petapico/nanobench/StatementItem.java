package org.petapico.nanobench;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.Nanopub;

public class StatementItem extends Panel {

	private static final long serialVersionUID = 1L;

	public StatementItem(String id, IRI subj, IRI pred, IRI obj, PublishFormContext context, boolean optional) {
		super(id);

		WebMarkupContainer statement = new WebMarkupContainer("statement");
		if (optional) {
			statement.add(new AttributeModifier("class", "nanopub-optional"));
		}
		statement.add(new ValueItem("subj", subj, true, context));
		statement.add(new ValueItem("pred", pred, true, context));
		statement.add(new ValueItem("obj", obj, true, context));
		if (optional) {
			statement.add(new Label("label", "(optional)"));
		} else {
			statement.add(new Label("label", ""));
		}
		add(statement);
	}

	public StatementItem(String id, IRI subj, IRI pred, Value obj, Nanopub np) {
		super(id);

		WebMarkupContainer statement = new WebMarkupContainer("statement");
		statement.add(new NanobenchLink("subj", subj.stringValue(), np));
		statement.add(new NanobenchLink("pred", pred.stringValue(), np));
		if (obj instanceof IRI) {
			statement.add(new NanobenchLink("obj", obj.stringValue(), np));
		} else {
			statement.add(new Label("obj", "\"" + obj.stringValue() + "\""));
		}
		statement.add(new Label("label", ""));
		add(statement);
	}

}
