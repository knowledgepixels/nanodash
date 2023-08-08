package com.knowledgepixels.nanodash;

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

	private final ContextType contextType;
	private final Template template;
	private final String componentId;
	private final Map<String,String> params = new HashMap<>();
	private List<FormComponent<String>> formComponents = new ArrayList<>();
	private Map<IRI,IModel<String>> formComponentModels = new HashMap<>();
	private Set<IRI> introducedIris = new HashSet<>();
	private boolean isLocal;
	private List<StatementItem> statementItems;
	private Set<IRI> iriSet = new HashSet<>();
	private Map<IRI,StatementItem> narrowScopeMap = new HashMap<>();
	private String targetNamespace;

	public PublishFormContext(ContextType contextType, String templateId, String componentId, String targetNamespace) {
		this.contextType = contextType;
		this.isLocal = templateId != null && templateId.startsWith("file://");
		// TODO: check whether template is of correct type:
		this.template = Template.getTemplate(templateId);
		this.componentId = componentId;
		this.targetNamespace = targetNamespace;
	}

	public void initStatements() {
		if (statementItems != null) return;
		statementItems = new ArrayList<>();
		for (IRI st : template.getStatementIris()) {
			StatementItem si = new StatementItem(componentId, st, this);
			statementItems.add(si);
			for (IRI i : si.getIriSet()) {
				if (iriSet.contains(i)) {
					narrowScopeMap.remove(i);
				} else {
					iriSet.add(i);
					narrowScopeMap.put(i, si);
				}
			}
		}
	}

	public void finalizeStatements() {
		Map<StatementItem,Integer> finalRepetitionCount = new HashMap<>();
		for (IRI ni : narrowScopeMap.keySet()) {
			// TODO: Move all occurrences of this to utility function:
			String postfix = Utils.getUriPostfix(ni);
			StatementItem si = narrowScopeMap.get(ni);
			int i = si.getRepetitionCount();
			while (true) {
				String p = postfix + "__" + i;
				if (hasParam(p)) {
					si.repeat();
				} else {
					break;
				}
				i++;
			}
			i = 1;
			int corr = 0;
			if (si.isEmpty()) corr = 1;
			while (true) {
				String p = postfix + "__." + i;
				if (hasParam(p)) {
					int absPos = si.getRepetitionCount() + i - 1 - corr;
					String param = postfix + "__" + absPos;
					if (i - corr == 0) param = postfix;
					setParam(param, getParam(p));
					finalRepetitionCount.put(si, i - corr);
				} else {
					break;
				}
				i++;
			}
		}
		for (StatementItem si : finalRepetitionCount.keySet()) {
			for (int i = 0 ; i < finalRepetitionCount.get(si) ; i++) {
				si.repeat();
			}
			si.refreshStatements();
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
			iri = NanodashSession.get().getUserIri();
		}
		if (iri.equals(Template.ASSERTION_PLACEHOLDER)) {
			iri = vf.createIRI(targetNamespace + "assertion");
		} else if (iri.equals(Template.NANOPUB_PLACEHOLDER)) {
			iri = vf.createIRI(targetNamespace);
		}
		if (iri.stringValue().startsWith("https://w3id.org/np/o/ntemplate/local/")) {
			// TODO: deprecate this (use LocalResource instead)
			return vf.createIRI(iri.stringValue().replaceFirst("^https://w3id.org/np/o/ntemplate/local/", targetNamespace));
		}
		// TODO: Move this code below to the respective placeholder classes:
		IModel<String> tf = formComponentModels.get(iri);
		Value processedValue = null;
		if (template.isRestrictedChoicePlaceholder(iri)) {
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				String prefix = template.getPrefix(iri);
				if (prefix == null) prefix = "";
				if (template.isLocalResource(iri)) prefix = targetNamespace;
				if (tf.getObject().matches("(https?|file)://.+")) prefix = "";
				String v = prefix + tf.getObject();
				if (v.matches("[^:# ]+")) v = targetNamespace + v;
				if (v.matches("https?://.*")) {
					processedValue = vf.createIRI(v);
				} else {
					processedValue = vf.createLiteral(tf.getObject());
				}
			}
		} else if (template.isUriPlaceholder(iri)) {
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				String prefix = template.getPrefix(iri);
				if (prefix == null) prefix = "";
				if (template.isLocalResource(iri)) prefix = targetNamespace;
				String v;
				if (template.isAutoEscapePlaceholder(iri)) {
					v = prefix + Utils.urlEncode(tf.getObject());
				} else {
					if (tf.getObject().matches("(https?|file)://.+")) prefix = "";
					v = prefix + tf.getObject();
				}
				if (v.matches("[^:# ]+")) v = targetNamespace + v;
				processedValue = vf.createIRI(v);
			}
		} else if (template.isLocalResource(iri)) {
			String prefix = Utils.getUriPrefix(iri);
			processedValue = vf.createIRI(iri.stringValue().replace(prefix, targetNamespace));
		} else if (template.isLiteralPlaceholder(iri)) {
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				processedValue = vf.createLiteral(tf.getObject());
			}
		} else if (template.isValuePlaceholder(iri)) {
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				if (tf.getObject().startsWith("\"") && tf.getObject().endsWith("\"")) {
					processedValue = vf.createLiteral(tf.getObject().substring(1, tf.getObject().length()-1).replaceAll("\\\\(\\\\|\\\")", "$1"));
				} else {
					String v = tf.getObject();
					if (v.matches("[^:# ]+")) v = targetNamespace + v;
					processedValue = vf.createIRI(v);
				}
			}
		} else {
			processedValue = iri;
		}
		if (processedValue instanceof IRI && template.isIntroducedResource((IRI) processedValue)) {
			introducedIris.add((IRI) processedValue);
		}
		return processedValue;
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
		return narrowScopeMap.containsKey(iri);
	}

	public void fill(List<Statement> statements) throws UnificationException {
		for (StatementItem si : statementItems) {
			si.fill(statements);
		}
	}

}
