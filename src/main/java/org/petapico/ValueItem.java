package org.petapico;

import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri, boolean objectPosition, PublishForm form) {
		super(id);
		if (form.typeMap.containsKey(iri) && form.typeMap.get(iri).contains(PublishForm.URI_PLACEHOLDER_CLASS)) {
			add(new IriTextfieldItem("value", iri, form));
		} else if (form.typeMap.containsKey(iri) && form.typeMap.get(iri).contains(PublishForm.LITERAL_PLACEHOLDER_CLASS)) {
				add(new LiteralTextfieldItem("value", iri, form));
		} else {
			add(new IriItem("value", iri, objectPosition, form));
		}
	}

}
