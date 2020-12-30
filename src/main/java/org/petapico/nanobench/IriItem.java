package org.petapico.nanobench;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.SimpleCreatorPattern;
import org.petapico.nanobench.PublishFormContext.ContextType;

public class IriItem extends Panel {
	
	private static final long serialVersionUID = 1L;

	public IriItem(String id, String parentId, IRI iri, boolean objectPosition, StatementItem.RepetitionGroup s) {
		super(id);
		final Template template = s.getContext().getTemplate();
		String labelString = null;
		if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
			iri = ProfilePage.getUserIri();
			if (objectPosition) {
				labelString = "me";
			} else {
				labelString = "I";
			}
		}
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			if (s.getContext().getType() == ContextType.ASSERTION) {
				labelString = "this assertion";
			} else {
				labelString = "the assertion above";
			}
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			labelString = "this nanopublication";
		}
		if (template.getLabel(iri) != null) {
			labelString = template.getLabel(iri);
		} else if (iri.equals(SimpleCreatorPattern.PROV_WASATTRIBUTEDTO)) {
			// temporary solution until we have full provenance graph support
			labelString = "is attributed to";
		} else if (labelString == null) {
			labelString = getShortNameFromURI(iri.stringValue());
		}
		if (labelString != null && labelString.length() > 0 && parentId.equals("subj")) {
			// Capitalize first letter of label if at subject position:
			labelString = labelString.substring(0, 1).toUpperCase() + labelString.substring(1);
		}
		labelString = labelString.replaceAll("%I%", "" + s.getRepeatIndex());
		Label labelComp = new Label("label", labelString);
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("class", " nanopub-assertion "));
			labelComp.add(new AttributeAppender("style", "padding: 4px; border-radius: 4px;"));
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("style", "background: #ffffff; border-width: 1px; border-color: #666; border-style: solid; padding: 4px; border-radius: 4px;"));
		}
		add(labelComp);
		String iriString = iri.stringValue();
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			iriString = "local:assertion";
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			iriString = "local:nanopub";
		} else if (template.isLocalResource(iri)) {
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
