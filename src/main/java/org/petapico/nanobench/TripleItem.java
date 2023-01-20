package org.petapico.nanobench;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;

public class TripleItem extends Panel {

	private static final long serialVersionUID = 1L;

	public TripleItem(String id, Statement st, Nanopub np, IRI templateClass) {
		super(id);

		WebMarkupContainer statement = new WebMarkupContainer("triple");
		statement.add(new NanobenchLink("subj", st.getSubject().stringValue(), np, templateClass, false));
		statement.add(new NanobenchLink("pred", st.getPredicate().stringValue(), np, templateClass, false));
		if (st.getObject() instanceof IRI) {
			statement.add(new NanobenchLink("obj", st.getObject().stringValue(), np, templateClass, true));
		} else {
			statement.add(new Label("obj", "\"" + st.getObject().stringValue() + "\""));
		}
		add(statement);
	}

}
