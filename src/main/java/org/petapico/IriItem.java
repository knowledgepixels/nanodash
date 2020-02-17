package org.petapico;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class IriItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public IriItem(String id, IRI iri, boolean objectPosition, PublishForm form) {
		super(id);
		String labelString = null;
		if (iri.equals(PublishForm.CREATOR_PLACEHOLDER)) {
			iri = form.userIri;
			if (objectPosition) {
				labelString = "me";
			} else {
				labelString = "I";
			}
		}
		if (form.labelMap.containsKey(iri)) {
			labelString = form.labelMap.get(iri);
		} else if (labelString == null) {
			labelString = getShortNameFromURI(iri.stringValue());
		}
		add(new Label("label", labelString));
		add(new Label("iri", iri.stringValue()));
	}

	public static String getShortNameFromURI(String uri) {
		uri = uri.replaceFirst("[/#]$", "");
		uri = uri.replaceFirst("^.*[/#]([^/#]*)[/#]([0-9]+)$", "$1/$2");
		uri = uri.replaceFirst("^.*[/#]([^/#]*[^0-9][^/#]*)$", "$1");
		uri = uri.replaceFirst("((^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{8})[A-Za-z0-9\\-_]{35}$", "$1");
		uri = uri.replaceFirst("(^|[^A-Za-z0-9\\-_])RA[A-Za-z0-9\\-_]{43}[^A-Za-z0-9\\-_](.+)$", "$2");
		return uri;
	}

}
