package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubWithNs;

public class TemplateContext implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();

	private final ContextType contextType;
	private final Template template;
	private final String componentId;
	private final Map<String,String> params = new HashMap<>();
	private List<Component> components = new ArrayList<>();
	private Map<IRI,IModel<String>> componentModels = new HashMap<>();
	private Set<IRI> introducedIris = new HashSet<>();
	private List<StatementItem> statementItems;
	private Set<IRI> iriSet = new HashSet<>();
	private Map<IRI,StatementItem> narrowScopeMap = new HashMap<>();
	private String targetNamespace = Template.DEFAULT_TARGET_NAMESPACE;
	private Nanopub existingNanopub;

	// For PublishForm when nanopub doesn't exist yet:
	public TemplateContext(ContextType contextType, String templateId, String componentId, String targetNamespace) {
		this(contextType, templateId, componentId, targetNamespace, null);
	}

	// For NanopubItem to show existing nanopub in read-only mode:
	public TemplateContext(ContextType contextType, String templateId, String componentId, Nanopub existingNanopub) {
		this(contextType, templateId, componentId, null, existingNanopub);
	}

	private TemplateContext(ContextType contextType, String templateId, String componentId, String targetNamespace, Nanopub existingNanopub) {
		this.contextType = contextType;
		// TODO: check whether template is of correct type:
		this.template = Template.getTemplate(templateId);
		this.componentId = componentId;
		if (targetNamespace != null) {
			this.targetNamespace = targetNamespace;
		}
		this.existingNanopub = existingNanopub;
		if (existingNanopub == null && NanodashSession.get().getUserIri() != null) {
			componentModels.put(Template.CREATOR_PLACEHOLDER, Model.of(NanodashSession.get().getUserIri().stringValue()));
		}
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
					si.addRepetitionGroup();
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
				si.addRepetitionGroup();
			}
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

	public List<Component> getComponents() {
		return components;
	}

	public Map<IRI,IModel<String>> getComponentModels() {
		return componentModels;
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
		// TODO: Move this code below to the respective placeholder classes:
		IModel<String> tf = componentModels.get(iri);
		Value processedValue = null;
		if (template.isRestrictedChoicePlaceholder(iri)) {
			if (tf != null && tf.getObject() != null && !tf.getObject().isEmpty()) {
				String prefix = template.getPrefix(iri);
				if (prefix == null) prefix = "";
				if (template.isLocalResource(iri)) prefix = targetNamespace;
				if (tf.getObject().matches("https?://.+")) prefix = "";
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
					if (tf.getObject().matches("https?://.+")) prefix = "";
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
		if (processedValue instanceof IRI && template.isIntroducedResource(iri)) {
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

	public boolean hasNarrowScope(IRI iri) {
		return narrowScopeMap.containsKey(iri);
	}

	public void fill(List<Statement> statements) throws UnificationException {
		for (StatementItem si : statementItems) {
			si.fill(statements);
		}
	}

	public Nanopub getExistingNanopub() {
		return existingNanopub;
	}

	public boolean isReadOnly() {
		return existingNanopub != null;
	}

}
