package org.petapico;

import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri, Map<IRI,List<IRI>> typeMap, Map<IRI,IModel<String>> textFields) {
		super(id);
		if (typeMap.containsKey(iri) && typeMap.get(iri).contains(PublishPage.URI_PLACEHOLDER_CLASS)) {
			add(new TextfieldItem("value", iri, textFields));
		} else if (typeMap.containsKey(iri) && typeMap.get(iri).contains(PublishPage.LITERAL_PLACEHOLDER_CLASS)) {
				add(new TextfieldItem("value", iri, textFields));
		} else {
			add(new IriItem("value", iri));
		}
	}

}
