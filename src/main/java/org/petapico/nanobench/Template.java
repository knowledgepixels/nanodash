package org.petapico.nanobench;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.IntegerLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;

public class Template implements Serializable {

	private static final long serialVersionUID = 1L;

	private static List<Template> templates;
	private static Map<String,Template> templateMap = new HashMap<>();

	private static void initTemplates() {
		templates = new ArrayList<>();
		Map<String,String> params = new HashMap<>();
		params.put("pred", RDF.TYPE.toString());
		params.put("obj", ASSERTION_TEMPLATE_CLASS.toString());
		params.put("graphpred", Nanopub.HAS_ASSERTION_URI.toString());
		List<Map<String,String>> templateEntries;
		try {
			templateEntries = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params);
			for (Map<String,String> entry : templateEntries) {
				if (entry.get("superseded").equals("1") || entry.get("superseded").equals("true")) continue;
				if (entry.get("retracted").equals("1") || entry.get("retracted").equals("true")) continue;
				Template t = new Template(entry.get("np"));
				templates.add(t);
				templateMap.put(t.getId(), t);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static List<Template> getTemplates() {
		if (templates == null) initTemplates();
		return templates;
	}

	public static Template getTemplate(String id) {
		if (templates == null) initTemplates();
		return templateMap.get(id);
	}


	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI ASSERTION_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate");
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI LOCAL_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LocalResource");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");
	public static final IRI RESTRICTED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RestrictedChoicePlaceholder");
	public static final IRI CREATOR_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/CREATOR");
	public static final IRI WAS_CREATED_FROM_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");
	public static final IRI STATEMENT_ORDER_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/statementOrder");
	public static final IRI POSSIBLE_VALUE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValue");
	public static final IRI HAS_PREFIX_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefix");
	public static final IRI HAS_PREFIX_LABEL_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefixLabel");


	private Nanopub nanopub;
	private String label;
	private Map<IRI,List<IRI>> typeMap = new HashMap<>();
	private Map<IRI,List<Value>> possibleValueMap = new HashMap<>();
	private Map<IRI,String> labelMap = new HashMap<>();
	private Map<IRI,String> prefixMap = new HashMap<>();
	private Map<IRI,String> prefixLabelMap = new HashMap<>();
	private List<IRI> statementIri;
	private Map<IRI,IRI> statementSubjects = new HashMap<>();
	private Map<IRI,IRI> statementPredicates = new HashMap<>();
	private Map<IRI,Value> statementObjects = new HashMap<>();
	private Map<IRI,Integer> statementOrder = new HashMap<>();

	private Template(String templateId) {
		nanopub = GetNanopub.get(templateId);
		processTemplate(nanopub);
	}

	public Nanopub getNanopub() {
		return nanopub;
	}

	public String getId() {
		return nanopub.getUri().toString();
	}

	public String getLabel() {
		return label;
	}

	public String getLabel(IRI iri) {
		return labelMap.get(iri);
	}

	public String getPrefix(IRI iri) {
		return prefixMap.get(iri);
	}

	public String getPrefixLabel(IRI iri) {
		return prefixLabelMap.get(iri);
	}

	public List<IRI> getStatementIris() {
		return statementIri;
	}

	public IRI getSubject(IRI statementIri) {
		return statementSubjects.get(statementIri);
	}

	public IRI getPredicate(IRI statementIri) {
		return statementPredicates.get(statementIri);
	}

	public Value getObject(IRI statementIri) {
		return statementObjects.get(statementIri);
	}

	public boolean isLocalResource(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(LOCAL_RESOURCE_CLASS);
	}

	public boolean isUriPlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(URI_PLACEHOLDER_CLASS);
	}

	public boolean isLiteralPlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(LITERAL_PLACEHOLDER_CLASS);
	}

	public boolean isRestrictedChoicePlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(RESTRICTED_CHOICE_PLACEHOLDER_CLASS);
	}

	public List<Value> getPossibleValues(IRI iri) {
		return possibleValueMap.get(iri);
	}

	private void processTemplate(Nanopub templateNp) {
		Map<IRI,Boolean> statementIriMap = new HashMap<>();
		for (Statement st : templateNp.getAssertion()) {
			if (st.getSubject().equals(templateNp.getAssertionUri())) {
				if (st.getPredicate().equals(RDFS.LABEL)) {
					label = st.getObject().stringValue();
				} else if (st.getPredicate().equals(HAS_STATEMENT_PREDICATE) && st.getObject() instanceof IRI) {
					statementIriMap.put((IRI) st.getObject(), true);
				}
			}
			if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
				List<IRI> l = typeMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					typeMap.put((IRI) st.getSubject(), l);
				}
				l.add((IRI) st.getObject());
			} else if (st.getPredicate().equals(POSSIBLE_VALUE_PREDICATE)) {
				List<Value> l = possibleValueMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					possibleValueMap.put((IRI) st.getSubject(), l);
				}
				l.add(st.getObject());
			} else if (st.getPredicate().equals(RDFS.LABEL) && st.getObject() instanceof Literal) {
				labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(HAS_PREFIX_PREDICATE) && st.getObject() instanceof Literal) {
				prefixMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(HAS_PREFIX_LABEL_PREDICATE) && st.getObject() instanceof Literal) {
				prefixLabelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			}
		}
		for (Statement st : templateNp.getAssertion()) {
			if (statementIriMap.containsKey(st.getSubject())) {
				if (st.getPredicate().equals(RDF.SUBJECT) && st.getObject() instanceof IRI) {
					statementSubjects.put((IRI) st.getSubject(), (IRI) st.getObject());
				} else if (st.getPredicate().equals(RDF.PREDICATE) && st.getObject() instanceof IRI) {
					statementPredicates.put((IRI) st.getSubject(), (IRI) st.getObject());
				} else if (st.getPredicate().equals(RDF.OBJECT)) {
					statementObjects.put((IRI) st.getSubject(), st.getObject());
				} else if (st.getPredicate().equals(STATEMENT_ORDER_PREDICATE)) {
					if (st.getObject() instanceof IntegerLiteral) {
						statementOrder.put((IRI) st.getSubject(), ((IntegerLiteral) st.getObject()).intValue());
					}
				}
			}
		}
		statementIri = new ArrayList<>(statementIriMap.keySet());
		statementIri.sort(new Comparator<IRI>() {
			@Override
			public int compare(IRI arg0, IRI arg1) {
				Integer i0 = statementOrder.get(arg0);
				Integer i1 = statementOrder.get(arg1);
				if (i0 == null && i1 == null) return arg0.stringValue().compareTo(arg1.stringValue());
				if (i0 == null) return 1;
				if (i1 == null) return -1;
				return i0-i1;
			}
		});
	}

}
