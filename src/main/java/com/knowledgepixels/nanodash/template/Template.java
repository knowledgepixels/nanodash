package com.knowledgepixels.nanodash.template;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import com.knowledgepixels.nanodash.LookupApis;
import com.knowledgepixels.nanodash.Utils;

public class Template implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI ASSERTION_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate");
	public static final IRI PROVENANCE_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ProvenanceTemplate");
	public static final IRI PUBINFO_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/PubinfoTemplate");
	public static final IRI UNLISTED_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UnlistedTemplate");
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI LOCAL_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LocalResource");
	public static final IRI INTRODUCED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/IntroducedResource");
	public static final IRI EMBEDDED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/EmbeddedResource");
	public static final IRI VALUE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ValuePlaceholder");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI AUTO_ESCAPE_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AutoEscapeUriPlaceholder");
	public static final IRI EXTERNAL_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ExternalUriPlaceholder");
	public static final IRI TRUSTY_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/TrustyUriPlaceholder");
	public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");
	public static final IRI LONG_LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LongLiteralPlaceholder");
	public static final IRI RESTRICTED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RestrictedChoicePlaceholder");
	public static final IRI GUIDED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/GuidedChoicePlaceholder");
	public static final IRI AGENT_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AgentPlaceholder");
	public static final IRI CREATOR_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/CREATOR");
	public static final IRI ASSERTION_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/ASSERTION");
	public static final IRI NANOPUB_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/NANOPUB");
	public static final IRI WAS_CREATED_FROM_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");
	public static final IRI WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromProvenanceTemplate");
	public static final IRI WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromPubinfoTemplate");
	public static final IRI STATEMENT_ORDER_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/statementOrder");
	public static final IRI POSSIBLE_VALUE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValue");
	public static final IRI POSSIBLE_VALUES_FROM_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValuesFrom");
	public static final IRI POSSIBLE_VALUES_FROM_API_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValuesFromApi");
	public static final IRI HAS_PREFIX_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefix");
	public static final IRI HAS_REGEX_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasRegex");
	public static final IRI HAS_PREFIX_LABEL_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefixLabel");
	public static final IRI OPTIONAL_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/OptionalStatement");
	public static final IRI GROUPED_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/GroupedStatement");
	public static final IRI REPEATABLE_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RepeatableStatement");
	public static final IRI HAS_DEFAULT_PROVENANCE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDefaultProvenance");
	public static final IRI HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasRequiredPubinfoElement");
	public static final IRI HAS_TAG = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTag");
	public static final IRI HAS_LABEL_FROM_API = vf.createIRI("https://w3id.org/np/o/ntemplate/hasLabelFromApi");
	public static final IRI HAS_DEFAULT_VALUE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDefaultValue");
	public static final IRI HAS_TARGET_NAMESPACE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTargetNamespace");
	public static final IRI HAS_NANOPUB_LABEL_PATTERN = vf.createIRI("https://w3id.org/np/o/ntemplate/hasNanopubLabelPattern");
	public static final IRI HAS_TARGET_NANOPUB_TYPE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTargetNanopubType");
	public static final IRI SEQUENCE_ELEMENT_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/SequenceElementPlaceholder");

	public static final String DEFAULT_TARGET_NAMESPACE = "https://w3id.org/np/";

	private Nanopub nanopub;
	private String label;
	private String description;

	// TODO: Make all these maps more generic and the code simpler:
	private IRI templateIri;
	private Map<IRI,List<IRI>> typeMap = new HashMap<>();
	private Map<IRI,List<Value>> possibleValueMap = new HashMap<>();
	private Map<IRI,List<IRI>> possibleValuesToLoadMap = new HashMap<>();
	private Map<IRI,List<String>> apiMap = new HashMap<>();
	private Map<IRI,String> labelMap = new HashMap<>();
	private Map<IRI,String> prefixMap = new HashMap<>();
	private Map<IRI,String> prefixLabelMap = new HashMap<>();
	private Map<IRI,String> regexMap = new HashMap<>();
	private Map<IRI,List<IRI>> statementMap = new HashMap<>();
	private Map<IRI,IRI> statementSubjects = new HashMap<>();
	private Map<IRI,IRI> statementPredicates = new HashMap<>();
	private Map<IRI,Value> statementObjects = new HashMap<>();
	private Map<IRI,Integer> statementOrder = new HashMap<>();
	private IRI defaultProvenance;
	private List<IRI> requiredPubinfoElements = new ArrayList<>();
	private String tag = null;
	private Map<IRI,Value> defaultValues = new HashMap<>();
	private String targetNamespace = null;
	private String nanopubLabelPattern;
	private List<IRI> targetNanopubTypes = new ArrayList<>();

	Template(String templateId) throws RDF4JException, MalformedNanopubException, IOException, MalformedTemplateException {
		nanopub = Utils.getNanopub(templateId);
		processTemplate(nanopub);
	}

	public boolean isUnlisted() {
		return typeMap.get(nanopub.getAssertionUri()).contains(UNLISTED_TEMPLATE_CLASS);
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

	public String getDescription() {
		return description;
	}

	public String getLabel(IRI iri) {
		iri = transform(iri);
		return labelMap.get(iri);
	}

	public IRI getFirstOccurence(IRI iri) {
		for (IRI i : getStatementIris()) {
			if (statementMap.containsKey(i)) {
				// grouped statement
				for (IRI g : getStatementIris(i)) {
					if (iri.equals(statementSubjects.get(g))) return g;
					if (iri.equals(statementPredicates.get(g))) return g;
					if (iri.equals(statementObjects.get(g))) return g;
				}
			} else {
				// non-grouped statement
				if (iri.equals(statementSubjects.get(i))) return i;
				if (iri.equals(statementPredicates.get(i))) return i;
				if (iri.equals(statementObjects.get(i))) return i;
			}
		}
		return null;
	}

	public String getPrefix(IRI iri) {
		iri = transform(iri);
		return prefixMap.get(iri);
	}

	public String getPrefixLabel(IRI iri) {
		iri = transform(iri);
		return prefixLabelMap.get(iri);
	}

	public String getRegex(IRI iri) {
		iri = transform(iri);
		return regexMap.get(iri);
	}

	public Value getDefault(IRI iri) {
		iri = transform(iri);
		return defaultValues.get(iri);
	}

	public List<IRI> getStatementIris() {
		return statementMap.get(templateIri);
	}

	public List<IRI> getStatementIris(IRI groupIri) {
		return statementMap.get(groupIri);
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
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(LOCAL_RESOURCE_CLASS);
	}

	public boolean isIntroducedResource(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(INTRODUCED_RESOURCE_CLASS);
	}

	public boolean isEmbeddedResource(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(EMBEDDED_RESOURCE_CLASS);
	}

	public boolean isValuePlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(VALUE_PLACEHOLDER_CLASS);
	}

	public boolean isUriPlaceholder(IRI iri) {
		iri = transform(iri);
		if (!typeMap.containsKey(iri)) return false;
		for (IRI t : typeMap.get(iri)) {
			if (t.equals(URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(EXTERNAL_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(TRUSTY_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(AUTO_ESCAPE_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(RESTRICTED_CHOICE_PLACEHOLDER_CLASS)) return true;
			if (t.equals(GUIDED_CHOICE_PLACEHOLDER_CLASS)) return true;
			if (t.equals(AGENT_PLACEHOLDER_CLASS)) return true;
		}
		return false;
	}

	public boolean isExternalUriPlaceholder(IRI iri) {
		iri = transform(iri);
		if (!typeMap.containsKey(iri)) return false;
		for (IRI t : typeMap.get(iri)) {
			if (t.equals(EXTERNAL_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(TRUSTY_URI_PLACEHOLDER_CLASS)) return true;
		}
		return false;
	}

	public boolean isTrustyUriPlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(TRUSTY_URI_PLACEHOLDER_CLASS);
	}

	public boolean isAutoEscapePlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(AUTO_ESCAPE_URI_PLACEHOLDER_CLASS);
	}

	public boolean isLiteralPlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && (typeMap.get(iri).contains(LITERAL_PLACEHOLDER_CLASS) || typeMap.get(iri).contains(LONG_LITERAL_PLACEHOLDER_CLASS));
	}

	public boolean isLongLiteralPlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(LONG_LITERAL_PLACEHOLDER_CLASS);
	}

	public boolean isRestrictedChoicePlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(RESTRICTED_CHOICE_PLACEHOLDER_CLASS);
	}

	public boolean isGuidedChoicePlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(GUIDED_CHOICE_PLACEHOLDER_CLASS);
	}

	public boolean isAgentPlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(AGENT_PLACEHOLDER_CLASS);
	}

	public boolean isSequenceElementPlaceholder(IRI iri) {
		iri = transform(iri);
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(SEQUENCE_ELEMENT_PLACEHOLDER);
	}

	public boolean isPlaceholder(IRI iri) {
		iri = transform(iri);
		if (!typeMap.containsKey(iri)) return false;
		for (IRI t : typeMap.get(iri)) {
			if (t.equals(VALUE_PLACEHOLDER_CLASS)) return true;
			if (t.equals(URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(EXTERNAL_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(TRUSTY_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(AUTO_ESCAPE_URI_PLACEHOLDER_CLASS)) return true;
			if (t.equals(RESTRICTED_CHOICE_PLACEHOLDER_CLASS)) return true;
			if (t.equals(GUIDED_CHOICE_PLACEHOLDER_CLASS)) return true;
			if (t.equals(AGENT_PLACEHOLDER_CLASS)) return true;
			if (t.equals(LITERAL_PLACEHOLDER_CLASS)) return true;
			if (t.equals(LONG_LITERAL_PLACEHOLDER_CLASS)) return true;
			if (t.equals(SEQUENCE_ELEMENT_PLACEHOLDER)) return true;
		}
		return false;
	}

	public boolean isOptionalStatement(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(OPTIONAL_STATEMENT_CLASS);
	}

	public boolean isGroupedStatement(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(GROUPED_STATEMENT_CLASS);
	}

	public boolean isRepeatableStatement(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(REPEATABLE_STATEMENT_CLASS);
	}

	public List<Value> getPossibleValues(IRI iri) {
		iri = transform(iri);
		List<Value> l = possibleValueMap.get(iri);
		if (l == null) {
			l = new ArrayList<>();
			possibleValueMap.put(iri, l);
		}
		List<IRI> nanopubList = possibleValuesToLoadMap.get(iri);
		if (nanopubList != null) {
			for (IRI npIri : new ArrayList<>(nanopubList)) {
				try {
					Nanopub valuesNanopub = Utils.getNanopub(npIri.stringValue());
					for (Statement st : valuesNanopub.getAssertion()) {
						if (st.getPredicate().equals(RDFS.LABEL)) {
							l.add((IRI) st.getSubject());
						}
					}
					nanopubList.remove(npIri);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return l;
	}

	public IRI getDefaultProvenance() {
		return defaultProvenance;
	}

	public String getTargetNamespace() {
		return targetNamespace;
	}

	public String getNanopubLabelPattern() {
		return nanopubLabelPattern;
	}

	public List<IRI> getTargetNanopubTypes() {
		return targetNanopubTypes;
	}

	public List<IRI> getRequiredPubinfoElements() {
		return requiredPubinfoElements;
	}

	public List<String> getPossibleValuesFromApi(IRI iri, String searchterm, Map<String,String> labelMap) {
		iri = transform(iri);
		List<String> values = new ArrayList<>();
		List<String> apiList = apiMap.get(iri);
		if (apiList != null) {
			for (String apiString : apiList) {
				LookupApis.getPossibleValues(apiString, searchterm, labelMap, values);
			}
		}
		return values;
	}

	public String getTag() {
		return tag;
	}

	private void processTemplate(Nanopub templateNp) throws MalformedTemplateException {
		Set<IRI> npTypes = NanopubUtils.getTypes(templateNp);
		if (npTypes.contains(ASSERTION_TEMPLATE_CLASS) || npTypes.contains(PROVENANCE_TEMPLATE_CLASS) || npTypes.contains(PUBINFO_TEMPLATE_CLASS)) {
			processNpTemplate(templateNp);
		} else {
			// Experimental SHACL-based template:
			processShaclTemplate(templateNp);
		}
	}

	private void processNpTemplate(Nanopub templateNp) throws MalformedTemplateException {
		templateIri = templateNp.getAssertionUri();
		for (Statement st : templateNp.getAssertion()) {
			final IRI subj = (IRI) st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			final String objS = obj.stringValue();

			if (subj.equals(templateIri)) {
				if (pred.equals(RDFS.LABEL)) {
					label = objS;
				} else if (pred.equals(DCTERMS.DESCRIPTION)) {
					description = Utils.sanitizeHtml(objS);
				} else if (obj instanceof IRI objIri) {
					if (pred.equals(HAS_DEFAULT_PROVENANCE_PREDICATE)) {
						defaultProvenance = objIri;
					} else if (pred.equals(HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE)) {
						requiredPubinfoElements.add(objIri);
					} else if (pred.equals(HAS_TARGET_NAMESPACE)) {
						targetNamespace = objS;
					} else if (pred.equals(HAS_TARGET_NANOPUB_TYPE)) {
						targetNanopubTypes.add(objIri);
					}
				} else if (obj instanceof Literal) {
					if (pred.equals(HAS_TAG)) {
						// TODO This should be replaced at some point with a more sophisticated mechanism based on classes.
						// We are assuming that there is at most one tag.
						this.tag = objS;
					} else if (pred.equals(HAS_NANOPUB_LABEL_PATTERN)) {
						nanopubLabelPattern = objS;
					}	
				}
			}
			if (pred.equals(RDF.TYPE) && obj instanceof IRI objIri) {
				addType(subj, objIri);
			} else if (pred.equals(HAS_STATEMENT_PREDICATE) && obj instanceof IRI objIri) {
				List<IRI> l = statementMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					statementMap.put(subj, l);
				}
				l.add((IRI) objIri);
			} else if (pred.equals(POSSIBLE_VALUE_PREDICATE)) {
				List<Value> l = possibleValueMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					possibleValueMap.put(subj, l);
				}
				l.add(obj);
			} else if (pred.equals(POSSIBLE_VALUES_FROM_PREDICATE)) {
				List<IRI> l = possibleValuesToLoadMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					possibleValuesToLoadMap.put(subj, l);
				}
				if (obj instanceof IRI objIri) {
					l.add(objIri);
					Nanopub valuesNanopub = Utils.getNanopub(objS);
					for (Statement s : valuesNanopub.getAssertion()) {
						if (s.getPredicate().equals(RDFS.LABEL)) {
							labelMap.put((IRI) s.getSubject(), s.getObject().stringValue());
						}
					}
				}
			} else if (pred.equals(POSSIBLE_VALUES_FROM_API_PREDICATE)) {
				List<String> l = apiMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					apiMap.put(subj, l);
				}
				if (obj instanceof Literal) {
					l.add(objS);
				}
			} else if (pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				labelMap.put(subj, objS);
			} else if (pred.equals(HAS_PREFIX_PREDICATE) && obj instanceof Literal) {
				prefixMap.put(subj, objS);
			} else if (pred.equals(HAS_PREFIX_LABEL_PREDICATE) && obj instanceof Literal) {
				prefixLabelMap.put(subj, objS);
			} else if (pred.equals(HAS_REGEX_PREDICATE) && obj instanceof Literal) {
				regexMap.put(subj, objS);
			} else if (pred.equals(RDF.SUBJECT) && obj instanceof IRI objIri) {
				statementSubjects.put(subj, objIri);
			} else if (pred.equals(RDF.PREDICATE) && obj instanceof IRI objIri) {
				statementPredicates.put(subj, objIri);
			} else if (pred.equals(RDF.OBJECT)) {
				statementObjects.put(subj, obj);
			} else if (pred.equals(HAS_DEFAULT_VALUE)) {
				defaultValues.put(subj, obj);
			} else if (pred.equals(STATEMENT_ORDER_PREDICATE)) {
				if (obj instanceof Literal && objS.matches("[0-9]+")) {
					statementOrder.put(subj, Integer.valueOf(objS));
				}
			}
		}
//		List<IRI> assertionTypes = typeMap.get(templateIri);
//		if (assertionTypes == null || (!assertionTypes.contains(ASSERTION_TEMPLATE_CLASS) &&
//				!assertionTypes.contains(PROVENANCE_TEMPLATE_CLASS) && !assertionTypes.contains(PUBINFO_TEMPLATE_CLASS))) {
//			throw new MalformedTemplateException("Unknown template type");
//		}
		for (List<IRI> l : statementMap.values()) {
			l.sort(statementComparator);
		}
	}

	private void processShaclTemplate(Nanopub templateNp) throws MalformedTemplateException {
		templateIri = null;
		for (Statement st : templateNp.getAssertion()) {
			if (st.getPredicate().equals(SHACL.TARGET_CLASS)) {
				templateIri = (IRI) st.getSubject();
				break;
			}
		}
		if (templateIri == null) {
			throw new MalformedTemplateException("Base node shape not found");
		}

		IRI baseSubj = vf.createIRI(templateIri.stringValue() + "+subj");
		addType(baseSubj, INTRODUCED_RESOURCE_CLASS);

		List<IRI> statementList = new ArrayList<>();
		Map<IRI,Integer> minCounts = new HashMap<>();
		Map<IRI,Integer> maxCounts = new HashMap<>();

		for (Statement st : templateNp.getAssertion()) {
			final IRI subj = (IRI) st.getSubject();
			final IRI pred = st.getPredicate();
			final Value obj = st.getObject();
			final String objS = obj.stringValue();

			if (subj.equals(templateIri)) {
				if (pred.equals(RDFS.LABEL)) {
					label = objS;
				} else if (pred.equals(DCTERMS.DESCRIPTION)) {
					description = Utils.sanitizeHtml(objS);
				} else if (obj instanceof IRI objIri) {
					if (pred.equals(HAS_DEFAULT_PROVENANCE_PREDICATE)) {
						defaultProvenance = objIri;
					} else if (pred.equals(HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE)) {
						requiredPubinfoElements.add(objIri);
					} else if (pred.equals(HAS_TARGET_NAMESPACE)) {
						targetNamespace = objS;
					} else if (pred.equals(HAS_TARGET_NANOPUB_TYPE)) {
						targetNanopubTypes.add(objIri);
					}
				} else if (obj instanceof Literal) {
					if (pred.equals(HAS_TAG)) {
						// TODO This should be replaced at some point with a more sophisticated mechanism based on classes.
						// We are assuming that there is at most one tag.
						this.tag = objS;
					} else if (pred.equals(HAS_NANOPUB_LABEL_PATTERN)) {
						nanopubLabelPattern = objS;
					}	
				}
			}
			if (pred.equals(RDF.TYPE) && obj instanceof IRI objIri) {
				addType(subj, objIri);
			} else if (pred.equals(SHACL.PROPERTY) && obj instanceof IRI objIri) {
				statementList.add(objIri);
				List<IRI> l = statementMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					statementMap.put(subj, l);
				}
				l.add((IRI) objIri);
				IRI stSubjIri = vf.createIRI(subj.stringValue() + "+subj");
				statementSubjects.put(objIri, stSubjIri);
				addType(stSubjIri, LOCAL_RESOURCE_CLASS);
				addType(stSubjIri, URI_PLACEHOLDER_CLASS);
			} else if (pred.equals(SHACL.PATH) && obj instanceof IRI objIri) {
				statementPredicates.put(subj, objIri);
				IRI stObjIri = vf.createIRI(subj.stringValue() + "+obj");
				statementObjects.put(subj, stObjIri);
				addType(stObjIri, VALUE_PLACEHOLDER_CLASS);
			} else if (pred.equals(POSSIBLE_VALUE_PREDICATE)) {
				List<Value> l = possibleValueMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					possibleValueMap.put(subj, l);
				}
				l.add(obj);
			} else if (pred.equals(POSSIBLE_VALUES_FROM_PREDICATE)) {
				List<IRI> l = possibleValuesToLoadMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					possibleValuesToLoadMap.put(subj, l);
				}
				if (obj instanceof IRI objIri) {
					l.add(objIri);
					Nanopub valuesNanopub = Utils.getNanopub(objS);
					for (Statement s : valuesNanopub.getAssertion()) {
						if (s.getPredicate().equals(RDFS.LABEL)) {
							labelMap.put((IRI) s.getSubject(), s.getObject().stringValue());
						}
					}
				}
			} else if (pred.equals(POSSIBLE_VALUES_FROM_API_PREDICATE)) {
				List<String> l = apiMap.get(subj);
				if (l == null) {
					l = new ArrayList<>();
					apiMap.put(subj, l);
				}
				if (obj instanceof Literal) {
					l.add(objS);
				}
			} else if (pred.equals(RDFS.LABEL) && obj instanceof Literal) {
				labelMap.put(subj, objS);
			} else if (pred.equals(HAS_PREFIX_PREDICATE) && obj instanceof Literal) {
				prefixMap.put(subj, objS);
			} else if (pred.equals(HAS_PREFIX_LABEL_PREDICATE) && obj instanceof Literal) {
				prefixLabelMap.put(subj, objS);
			} else if (pred.equals(HAS_REGEX_PREDICATE) && obj instanceof Literal) {
				regexMap.put(subj, objS);
//			} else if (pred.equals(RDF.SUBJECT) && obj instanceof IRI objIri) {
//				statementSubjects.put(subj, objIri);
//			} else if (pred.equals(RDF.PREDICATE) && obj instanceof IRI objIri) {
//				statementPredicates.put(subj, objIri);
//			} else if (pred.equals(RDF.OBJECT)) {
//				statementObjects.put(subj, obj);
			} else if (pred.equals(HAS_DEFAULT_VALUE)) {
				defaultValues.put(subj, obj);
			} else if (pred.equals(STATEMENT_ORDER_PREDICATE)) {
				if (obj instanceof Literal && objS.matches("[0-9]+")) {
					statementOrder.put(subj, Integer.valueOf(objS));
				}
			} else if (pred.equals(SHACL.MIN_COUNT)) {
				try {
					minCounts.put(subj, Integer.parseInt(obj.stringValue()));
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			} else if (pred.equals(SHACL.MAX_COUNT)) {
				try {
					maxCounts.put(subj, Integer.parseInt(obj.stringValue()));
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
		}
		for (List<IRI> l : statementMap.values()) {
			l.sort(statementComparator);
		}
		for (IRI iri : statementList) {
			if (!minCounts.containsKey(iri) || minCounts.get(iri) <= 0) {
				addType(iri, OPTIONAL_STATEMENT_CLASS);
			}
			if (!maxCounts.containsKey(iri) || maxCounts.get(iri) > 1) {
				addType(iri, REPEATABLE_STATEMENT_CLASS);
			}
		}

		if (label == null) {
			label = NanopubUtils.getLabel(templateNp);
		}
	}

	private void addType(IRI thing, IRI type) {
		List<IRI> l = typeMap.get(thing);
		if (l == null) {
			l = new ArrayList<>();
			typeMap.put(thing, l);
		}
		l.add(type);
	}

	private IRI transform(IRI iri) {
		if (iri.stringValue().matches(".*__[0-9]+")) {
			// TODO: Check that this double-underscore pattern isn't used otherwise:
			return vf.createIRI(iri.stringValue().replaceFirst("__[0-9]+$", ""));
		}
		return iri;
	}


	private StatementComparator statementComparator = new StatementComparator();

	private class StatementComparator implements Comparator<IRI>, Serializable {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(IRI arg0, IRI arg1) {
			Integer i0 = statementOrder.get(arg0);
			Integer i1 = statementOrder.get(arg1);
			if (i0 == null && i1 == null) return arg0.stringValue().compareTo(arg1.stringValue());
			if (i0 == null) return 1;
			if (i1 == null) return -1;
			return i0-i1;
		}

	}

}
