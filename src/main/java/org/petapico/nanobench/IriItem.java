package org.petapico.nanobench;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.nanopub.SimpleCreatorPattern;
import org.petapico.nanobench.StatementItem.RepetitionGroup;

public class IriItem extends Panel implements ContextComponent {
	
	private static final long serialVersionUID = 1L;

	private IRI iri;
	private PublishFormContext context;

	public IriItem(String id, String parentId, IRI iriP, boolean objectPosition, RepetitionGroup rg) {
		super(id);
		this.iri = iriP;
		this.context = rg.getContext();
		final Template template = context.getTemplate();
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
			if (context.getType() == ContextType.ASSERTION) {
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
		labelString = labelString.replaceAll("%I%", "" + rg.getRepeatIndex());
		Label labelComp = new Label("label", labelString);
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("class", " nanopub-assertion "));
			labelComp.add(new AttributeAppender("style", "padding: 4px; border-radius: 4px;"));
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			labelComp.add(new AttributeAppender("style", "background: #ffffff; background-image: url(\"npback-left.png\"); border-width: 1px; border-color: #666; border-style: solid; padding: 4px 4px 4px 20px; border-radius: 4px;"));
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

	@Override
	public void removeFromContext() {
		// Nothing to be done here.
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (!(v instanceof IRI)) return false;
		String iriS = iri.stringValue().replaceFirst("^" + context.getTemplateId() + "[#/]?", "local:");
		return iriS.equals(v.stringValue());
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
		// Nothing left to be done here.
	}

	public String toString() {
		return "[IRI item: " + iri + "]";
	}

}
