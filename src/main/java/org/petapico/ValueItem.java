package org.petapico;

import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri, final PublishPage page) {
		super(id);
		if (page.typeMap.containsKey(iri) && page.typeMap.get(iri).contains(PublishPage.URI_PLACEHOLDER_CLASS)) {
			add(new TextfieldItem("value", iri, page));
		} else if (page.typeMap.containsKey(iri) && page.typeMap.get(iri).contains(PublishPage.LITERAL_PLACEHOLDER_CLASS)) {
				add(new TextfieldItem("value", iri, page));
		} else {
			add(new IriItem("value", iri));
		}
	}

}
