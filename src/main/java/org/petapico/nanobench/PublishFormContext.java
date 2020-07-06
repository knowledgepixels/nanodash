package org.petapico.nanobench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class PublishFormContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public static enum ContextType {
		ASSERTION, PROVENANCE, PUBINFO
	}

	private final ContextType contextType;
	private final Template template;
	private final Map<String,String> params = new HashMap<>();
	private List<FormComponent<String>> formComponents = new ArrayList<>();
	private Map<IRI,IModel<String>> formComponentModels = new HashMap<>();
	private Set<IRI> introducedIris = new HashSet<>();

	public PublishFormContext(ContextType contextType, String templateId) {
		this.contextType = contextType;
		this.template = Template.getTemplate(templateId);
	}

	public ContextType getType() {
		return contextType;
	}

	public Template getTemplate() {
		return template;
	}

	public String getTemplateId() {
		return template.getId();
	}

	public void setParam(String name, String value) {
		params.put(name, value);
	}

	public String getParam(String name) {
		return params.get(name);
	}

	public boolean hasParam(String name) {
		return params.containsKey(name);
	}

	public List<FormComponent<String>> getFormComponents() {
		return formComponents;
	}

	public Map<IRI,IModel<String>> getFormComponentModels() {
		return formComponentModels;
	}

	public Set<IRI> getIntroducedIris() {
		return introducedIris;
	}

	public IRI processIri(IRI iri) {
		Value v = processValue(iri);
		if (v instanceof IRI) return (IRI) v;
		return iri;
	}

	public Value processValue(Value value) {
		if (!(value instanceof IRI)) return value;
		IRI iri = (IRI) value;
		if (iri.equals(Template.CREATOR_PLACEHOLDER)) {
			iri = ProfilePage.getUserIri();
		}
		if (iri.stringValue().startsWith("https://w3id.org/np/o/ntemplate/local/")) {
			// TODO: deprecate this (use LocalResource instead)
			return vf.createIRI(iri.stringValue().replaceFirst("^https://w3id.org/np/o/ntemplate/local/", "http://purl.org/nanopub/temp/nanobench-new-nanopub/"));
		}
		if (template.isUriPlaceholder(iri) || template.isGuidedChoicePlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				String prefix = template.getPrefix(iri);
				if (prefix == null) prefix = "";
				if (template.isLocalResource(iri)) prefix = "http://purl.org/nanopub/temp/nanobench-new-nanopub/";
				if (tf.getObject().matches("(https?|file)://.+")) prefix = "";
				IRI processedIri = vf.createIRI(prefix + tf.getObject());
				if (template.isIntroducedResource(iri)) {
					introducedIris.add(processedIri);
				}
				return processedIri;
			} else {
				return null;
			}
		} else if (template.isLocalResource(iri)) {
			IRI processedIri = vf.createIRI(iri.stringValue().replaceFirst("^.*[/#]", "http://purl.org/nanopub/temp/nanobench-new-nanopub/"));
			if (template.isIntroducedResource(iri)) {
				introducedIris.add(processedIri);
			}
			return processedIri;
		} else if (template.isLiteralPlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				return vf.createLiteral(tf.getObject());
			} else {
				return null;
			}
		} else if (template.isRestrictedChoicePlaceholder(iri)) {
			IModel<String> tf = formComponentModels.get(iri);
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				String prefix = template.getPrefix(iri);
				if (prefix == null) prefix = "";
				if (tf.getObject().matches("https?://.*") || prefix.matches("https?://.*")) {
					return vf.createIRI(prefix + tf.getObject());
				}
				return vf.createLiteral(tf.getObject());
			} else {
				return null;
			}
		}
		return iri;
	}

}
