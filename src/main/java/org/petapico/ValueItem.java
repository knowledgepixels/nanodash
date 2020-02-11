package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri) {
		super(id);
		String s = "null";
		if (iri != null) s = iri.stringValue();
		add(new Label("thing", s));
	}

}
