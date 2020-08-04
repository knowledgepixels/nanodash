package org.petapico.nanobench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubWithNs;

public class PublishFormContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public static enum ContextType {
		ASSERTION, PROVENANCE, PUBINFO
	}

	public static IRI NP_TEMP_IRI = vf.createIRI("http://purl.org/nanopub/temp/nanobench-new-nanopub/");
	public static IRI ASSERTION_TEMP_IRI = vf.createIRI("http://purl.org/nanopub/temp/nanobench-new-nanopub/assertion");

	private final ContextType contextType;
	private final Template template;
	private final Map<String,String> params = new HashMap<>();
	private List<FormComponent<String>> formComponents = new ArrayList<>();
	private Map<IRI,IModel<String>> formComponentModels = new HashMap<>();
	private Set<IRI> introducedIris = new HashSet<>();
	private boolean isLocal;

	public PublishFormContext(ContextType contextType, String templateId) {
		this.contextType = contextType;
		this.isLocal = templateId != null && templateId.startsWith("file://");
		// TODO: check whether template is of correct type:
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
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			iri = ASSERTION_TEMP_IRI;
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			iri = NP_TEMP_IRI;
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

	public List<Panel> makeStatementItems(String componentId) {
		List<Panel> statementItems = new ArrayList<>();
		for (IRI st : template.getStatementIris()) {
			IRI subj = template.getSubject(st);
			IRI pred = template.getPredicate(st);
			IRI obj = (IRI) template.getObject(st);
			if (template.isOptionalStatement(st)) {
				statementItems.add(new OptionalStatementItem(componentId, subj, pred, obj, this));
			} else {
				statementItems.add(new StatementItem(componentId, subj, pred, obj, this));
			}
		}
		return statementItems;
	}

	public void propagateStatements(NanopubCreator npCreator) throws MalformedNanopubException {
		if (template.getNanopub() instanceof NanopubWithNs) {
			NanopubWithNs np = (NanopubWithNs) template.getNanopub();
			for (String p : np.getNsPrefixes()) {
				npCreator.addNamespace(p, np.getNamespace(p));
			}
		}
		for (IRI st : template.getStatementIris()) {
			IRI subj = processIri(template.getSubject(st));
			IRI pred = processIri(template.getPredicate(st));
			Value obj = processValue(template.getObject(st));
			if (subj == null || pred == null || obj == null) {
				if (template.isOptionalStatement(st)) {
					continue;
				} else {
					throw new MalformedNanopubException("Field of non-optional statement not set.");
				}
			} else {
				if (contextType == ContextType.ASSERTION) {
					npCreator.addAssertionStatement(subj, pred, obj);
				} else if (contextType == ContextType.PROVENANCE) {
					npCreator.addProvenanceStatement(subj, pred, obj);
				} else if (contextType == ContextType.PUBINFO) {
					npCreator.addPubinfoStatement(subj, pred, obj);
				}
			}
		}
	}

	public boolean isLocal() {
		return isLocal;
	}

}
