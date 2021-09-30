package org.petapico.nanobench;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubWithNs;

public class PublishFormContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	public static IRI NP_TEMP_IRI = vf.createIRI("http://purl.org/nanopub/temp/nanobench-new-nanopub/");
	public static IRI ASSERTION_TEMP_IRI = vf.createIRI("http://purl.org/nanopub/temp/nanobench-new-nanopub/assertion");

	private final ContextType contextType;
	private final Template template;
	private final String componentId;
	private final Map<String,String> params = new HashMap<>();
	private List<FormComponent<String>> formComponents = new ArrayList<>();
	private Map<IRI,IModel<String>> formComponentModels = new HashMap<>();
	private Set<IRI> introducedIris = new HashSet<>();
	private boolean isLocal;
	private List<StatementItem> statementItems;
	private Bag<IRI> iriBag = new HashBag<>();

	public PublishFormContext(ContextType contextType, String templateId, String componentId) {
		this.contextType = contextType;
		this.isLocal = templateId != null && templateId.startsWith("file://");
		// TODO: check whether template is of correct type:
		this.template = Template.getTemplate(templateId);
		this.componentId = componentId;
	}

	public void initStatements() {
		if (statementItems != null) return;
		statementItems = new ArrayList<>();
		for (IRI st : template.getStatementIris()) {
			StatementItem si = new StatementItem(componentId, st, this);
			statementItems.add(si);
			iriBag.addAll(si.getIriSet());
		}
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
		if (v == null) return null;
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
			return vf.createIRI(iri.stringValue().replaceFirst("^https://w3id.org/np/o/ntemplate/local/", NP_TEMP_IRI.stringValue()));
		}
		// TODO: Move this code below to the respective placeholder classes:
		IModel<String> tf = formComponentModels.get(iri);
		if (template.isRestrictedChoicePlaceholder(iri)) {
			if (tf == null || tf.getObject() == null || tf.getObject().isEmpty()) return null;
			String prefix = template.getPrefix(iri);
			if (prefix == null) prefix = "";
			if (template.isLocalResource(iri)) prefix = NP_TEMP_IRI.stringValue();
			if (tf.getObject().matches("(https?|file)://.+")) prefix = "";
			String v = prefix + tf.getObject();
			if (v.matches("[^/# ]+")) v = NP_TEMP_IRI.stringValue() + v;
			if (v.matches("https?://.*")) {
				return vf.createIRI(v);
			}
			return vf.createLiteral(tf.getObject());
		} else if (template.isUriPlaceholder(iri)) {
			if (tf == null || tf.getObject() == null || tf.getObject().isEmpty()) return null;
			String prefix = template.getPrefix(iri);
			if (prefix == null) prefix = "";
			if (template.isLocalResource(iri)) prefix = NP_TEMP_IRI.stringValue();
			if (tf.getObject().matches("(https?|file)://.+")) prefix = "";
			String v = prefix + tf.getObject();
			if (v.matches("[^/# ]+")) v = NP_TEMP_IRI.stringValue() + v;
			IRI processedIri = vf.createIRI(v);
			if (template.isIntroducedResource(iri)) {
				introducedIris.add(processedIri);
			}
			return processedIri;
		} else if (template.isLocalResource(iri)) {
			IRI processedIri = vf.createIRI(iri.stringValue().replaceFirst("^.*[/#]", NP_TEMP_IRI.stringValue()));
			if (template.isIntroducedResource(iri)) {
				introducedIris.add(processedIri);
			}
			return processedIri;
		} else if (template.isLiteralPlaceholder(iri)) {
			if (tf == null || tf.getObject() == null || tf.getObject().isEmpty()) return null;
			return vf.createLiteral(tf.getObject());
		} else if (template.isValuePlaceholder(iri)) {
			if (tf == null || tf.getObject() == null || tf.getObject().isEmpty()) return null;
			if (tf.getObject().startsWith("\"") && tf.getObject().endsWith("\"")) {
				return vf.createLiteral(tf.getObject().substring(1, tf.getObject().length()-1).replaceAll("\\\\(\\\\|\\\")", "$1"));
			} else {
				String v = tf.getObject();
				if (v.matches("[^/# ]+")) v = NP_TEMP_IRI.stringValue() + v;
				return vf.createIRI(v);
			}
		}
		return iri;
	}

	public List<StatementItem> getStatementItems() {
		return statementItems;
	}

	public void propagateStatements(NanopubCreator npCreator) throws MalformedNanopubException {
		if (template.getNanopub() instanceof NanopubWithNs) {
			NanopubWithNs np = (NanopubWithNs) template.getNanopub();
			for (String p : np.getNsPrefixes()) {
				npCreator.addNamespace(p, np.getNamespace(p));
			}
		}
		for (StatementItem si : statementItems) {
			si.addTriplesTo(npCreator);
		}
	}

	public boolean isLocal() {
		return isLocal;
	}

	public boolean hasNarrowScope(IRI iri) {
		return iriBag.getCount(iri) == 1;
	}

	public void fill(List<Statement> statements) throws UnificationException {
		for (StatementItem si : statementItems) {
			si.fill(statements);
		}
	}

}
