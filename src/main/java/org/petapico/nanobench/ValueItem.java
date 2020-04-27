package org.petapico.nanobench;

import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class ValueItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public ValueItem(String id, IRI iri, boolean optional, PublishForm form) {
		super(id);
		if (form.template.isUriPlaceholder(iri)) {
			add(new IriTextfieldItem("value", iri, optional, form));
		} else if (form.template.isLiteralPlaceholder(iri)) {
			add(new LiteralTextfieldItem("value", iri, optional, form));
		} else if (form.template.isRestrictedChoicePlaceholder(iri)) {
			add(new RestrictedChoiceItem("value", iri, optional, form));
		} else {
			add(new IriItem("value", iri, id.equals("obj"), form));
		}
	}

}
