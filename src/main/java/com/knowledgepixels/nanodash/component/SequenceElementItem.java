package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.UnificationException;

public class SequenceElementItem extends Panel implements ContextComponent {

	private static final long serialVersionUID = 1L;

	public static final String SEQUENCE_ELEMENT_PROPERTY_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#_";

	private final int number;

	public SequenceElementItem(String id, final IRI iri, int number, TemplateContext context) {
		super(id);
		this.number = number;
		context.getComponentModels().put(iri, Model.of(SEQUENCE_ELEMENT_PROPERTY_PREFIX + number));

		String labelString = "has element number ${number}";
		if (context.getTemplate().getLabel(iri) != null) {
			labelString = context.getTemplate().getLabel(iri);
		}
		String description = "This relation links a sequence/list to its element at position " + number + ".";
		if (labelString.contains(" - ")) description = labelString.replaceFirst("^.* - ", "");
		String label = labelString.replaceFirst(" - .*$", "");
		label = label.replaceAll("\\$\\{number\\}", number + "");

		add(new Label("description", description));
		add(Utils.getUriLink("uri", SEQUENCE_ELEMENT_PROPERTY_PREFIX + number));

		add(new ExternalLink("text", "", label));
	}

	@Override
	public void removeFromContext() {
	}

	@Override
	public boolean isUnifiableWith(Value v) {
		if (v == null) return true;
		if (v instanceof Literal) return false;
		return v.stringValue().equals(SEQUENCE_ELEMENT_PROPERTY_PREFIX + number);
	}

	@Override
	public void unifyWith(Value v) throws UnificationException {
		if (v == null) return;
		if (!isUnifiableWith(v)) throw new UnificationException(v.stringValue());
	}

	@Override
	public void fillFinished() {
	}

	@Override
	public void finalizeValues() {
	}

	public String toString() {
		return "[Sequence element]";
	}

}
