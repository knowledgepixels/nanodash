package org.petapico.nanobench;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;

public class IriItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public IriItem(String id, String parentId, IRI iri, boolean objectPosition, PublishForm form) {
		super(id);
		String labelString = null;
		if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
			iri = ProfilePage.getUserIri();
			if (objectPosition) {
				labelString = "me";
			} else {
				labelString = "I";
			}
		}
		if (form.template.getLabel(iri) != null) {
			labelString = form.template.getLabel(iri);
		} else if (labelString == null) {
			labelString = getShortNameFromURI(iri.stringValue());
		}
		if (labelString != null && labelString.length() > 0 && parentId.equals("subj")) {
			// Capitalize first letter of label if at subject position:
			labelString = labelString.substring(0, 1).toUpperCase() + labelString.substring(1);
		}
		add(new Label("label", labelString));
		String iriString = iri.stringValue();
		if (form.template.isLocalResource(iri)) {
			iriString = iriString.replaceFirst("^.*[/#]", "local:");
		}
		add(new Label("iri", iriString));
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
