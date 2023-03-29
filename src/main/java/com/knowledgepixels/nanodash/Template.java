package com.knowledgepixels.nanodash;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.wicket.util.file.File;
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
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.services.ApiAccess;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import com.opencsv.exceptions.CsvValidationException;

import net.trustyuri.TrustyUriUtils;

public class Template implements Serializable {

	private static final long serialVersionUID = 1L;

	private static List<Template> assertionTemplates, provenanceTemplates, pubInfoTemplates;
	private static Map<String,Template> templateMap;

	static void refreshTemplates() {
		assertionTemplates = new ArrayList<>();
		provenanceTemplates = new ArrayList<>();
		pubInfoTemplates = new ArrayList<>();
		templateMap = new HashMap<>();
		refreshTemplates(assertionTemplates, ASSERTION_TEMPLATE_CLASS);
		refreshTemplates(provenanceTemplates, PROVENANCE_TEMPLATE_CLASS);
		refreshTemplates(pubInfoTemplates, PUBINFO_TEMPLATE_CLASS);
	}

	private static void refreshTemplates(List<Template> templates, IRI type) {
		Map<String,String> params = new HashMap<>();
		params.put("pred", RDF.TYPE.toString());
		params.put("obj", type.toString());
		params.put("graphpred", Nanopub.HAS_ASSERTION_URI.toString());
		ApiResponse templateEntries;
		try {
			templateEntries = ApiAccess.getAll("find_signed_nanopubs_with_pattern", params);
			for (ApiResponseEntry entry : templateEntries.getData()) {
				if (entry.get("superseded").equals("1") || entry.get("superseded").equals("true")) continue;
				if (entry.get("retracted").equals("1") || entry.get("retracted").equals("true")) continue;
				try {
					Template t = new Template(entry.get("np"));
					if (!t.isUnlisted()) templates.add(t);
					templateMap.put(t.getId(), t);
				} catch (Exception ex) {
					System.err.println("Exception: " + ex.getMessage());
				}
			}
		} catch (IOException|CsvValidationException ex) {
			// TODO Better handle this (re-try to get templates)
			ex.printStackTrace();
		}
	}

	public static List<Template> getAssertionTemplates() {
		if (assertionTemplates == null) refreshTemplates();
		return assertionTemplates;
	}

	public static List<Template> getProvenanceTemplates() {
		if (provenanceTemplates == null) refreshTemplates();
		return provenanceTemplates;
	}

	public static List<Template> getPubInfoTemplates() {
		if (pubInfoTemplates == null) refreshTemplates();
		return pubInfoTemplates;
	}

	public static Template getTemplate(String id) {
		if (assertionTemplates == null) refreshTemplates();
		Template template = templateMap.get(id);
		if (template != null) return template;
		if (id.startsWith("file://") || TrustyUriUtils.isPotentialTrustyUri(id)) {
			try {
				return new Template(id);
			} catch (Exception ex) {
				System.err.println("Exception: " + ex.getMessage());
				return null;
			}
		}
		return null;
	}

	public static Template getTemplate(Nanopub np) {
		IRI templateId = getTemplateId(np);
		if (templateId == null) return null;
		return getTemplate(templateId.stringValue());
	}


	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI ASSERTION_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate");
	public static final IRI PROVENANCE_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ProvenanceTemplate");
	public static final IRI PUBINFO_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/PubinfoTemplate");
	public static final IRI UNLISTED_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UnlistedTemplate");
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI LOCAL_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LocalResource");
	public static final IRI INTRODUCED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/IntroducedResource");
	public static final IRI VALUE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ValuePlaceholder");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI AUTO_ESCAPE_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AutoEscapeUriPlaceholder");
	public static final IRI EXTERNAL_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ExternalUriPlaceholder");
	public static final IRI TRUSTY_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/TrustyUriPlaceholder");
	public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");
	public static final IRI LONG_LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LongLiteralPlaceholder");
	public static final IRI RESTRICTED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RestrictedChoicePlaceholder");
	public static final IRI GUIDED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/GuidedChoicePlaceholder");
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


	private Nanopub nanopub;
	private String label;
	private String description;

	// TODO: Make all these maps more generic and the code simpler:
	private IRI assertionIri;
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

	private Template(String templateId) throws RDF4JException, MalformedNanopubException, IOException, MalformedTemplateException {
		if (templateId.startsWith("file://")) {
			nanopub = new NanopubImpl(new File(templateId.substring(7)));
		} else {
			nanopub = Utils.getNanopub(templateId);
		}
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

	public List<IRI> getStatementIris() {
		return statementMap.get(assertionIri);
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
			if (t.equals(LITERAL_PLACEHOLDER_CLASS)) return true;
			if (t.equals(LONG_LITERAL_PLACEHOLDER_CLASS)) return true;
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
					Nanopub valuesNanopub = new NanopubImpl(new URL(npIri.stringValue()));
					for (Statement st : valuesNanopub.getAssertion()) {
						if (st.getPredicate().equals(RDFS.LABEL)) {
							l.add((IRI) st.getSubject());
							labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
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

	public List<IRI> getRequiredPubinfoElements() {
		return requiredPubinfoElements;
	}

	public List<String> getPossibleValuesFromApi(IRI iri, String searchterm, Map<String,String> labelMap) {
		iri = transform(iri);
		List<String> values = new ArrayList<>();
		List<String> apiList = apiMap.get(iri);
		if (apiList != null) {
			for (String apiString : apiList) {
				if (apiString.startsWith("http://purl.org/nanopub/api/find_signed_things?")) {
					List<NameValuePair> urlParams = URLEncodedUtils.parse(apiString.substring(apiString.indexOf("?") + 1), StandardCharsets.UTF_8);
					getPossibleValuesFromNanopubApi(urlParams, searchterm, labelMap, values);
				} else {
					LookupApis.getPossibleValues(apiString, searchterm, labelMap, values);
				}
			}
		}
		return values;
	}

	public String getTag() {
		return tag;
	}

	private void getPossibleValuesFromNanopubApi(List<NameValuePair> urlParams, String searchterm, Map<String,String> labelMap, List<String> values) {
		try {
			Map<String,String> params = new HashMap<>();
			for (NameValuePair p : urlParams) {
				params.put(p.getName(), p.getValue());
			}
			params.put("searchterm", " " + searchterm);
			ApiResponse result = ApiAccess.getAll("find_signed_things", params);
			int count = 0;
			for (ApiResponseEntry r : result.getData()) {
				if (r.get("superseded").equals("1") || r.get("retracted").equals("1")) continue;
				String uri = r.get("thing");
				values.add(uri);
				String desc = r.get("description");
				if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
				if (!desc.isEmpty()) desc = " - " + desc;
				labelMap.put(uri, r.get("label") + " - by " + User.getShortDisplayNameForPubkey(r.get("pubkey")));
				count++;
				if (count > 9) return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void processTemplate(Nanopub templateNp) throws MalformedTemplateException {
		assertionIri = templateNp.getAssertionUri();
		for (Statement st : templateNp.getAssertion()) {
			if (st.getSubject().equals(assertionIri)) {
				if (st.getPredicate().equals(RDFS.LABEL)) {
					label = st.getObject().stringValue();
				} else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
					description = Utils.sanitizeHtml(st.getObject().stringValue());
				} else if (st.getObject() instanceof IRI) {
					if (st.getPredicate().equals(HAS_DEFAULT_PROVENANCE_PREDICATE)) {
						defaultProvenance = (IRI) st.getObject();
					} else if (st.getPredicate().equals(HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE)) {
						requiredPubinfoElements.add((IRI) st.getObject());
					}
				} else if (st.getPredicate().equals(HAS_TAG) && st.getObject() instanceof Literal) {
					// TODO This should be replaced at some point with a more sophisticated mechanism based on classes.
					// We are assuming that there is at most one tag.
					this.tag = st.getObject().stringValue();
				}
			}
			if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI) {
				List<IRI> l = typeMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					typeMap.put((IRI) st.getSubject(), l);
				}
				l.add((IRI) st.getObject());
			} else if (st.getPredicate().equals(HAS_STATEMENT_PREDICATE) && st.getObject() instanceof IRI) {
				List<IRI> l = statementMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					statementMap.put((IRI) st.getSubject(), l);
				}
				l.add((IRI) st.getObject());
			} else if (st.getPredicate().equals(POSSIBLE_VALUE_PREDICATE)) {
				List<Value> l = possibleValueMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					possibleValueMap.put((IRI) st.getSubject(), l);
				}
				l.add(st.getObject());
			} else if (st.getPredicate().equals(POSSIBLE_VALUES_FROM_PREDICATE)) {
				List<IRI> l = possibleValuesToLoadMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					possibleValuesToLoadMap.put((IRI) st.getSubject(), l);
				}
				if (st.getObject() instanceof IRI) {
					l.add((IRI) st.getObject());
				}
			} else if (st.getPredicate().equals(POSSIBLE_VALUES_FROM_API_PREDICATE)) {
				List<String> l = apiMap.get(st.getSubject());
				if (l == null) {
					l = new ArrayList<>();
					apiMap.put((IRI) st.getSubject(), l);
				}
				if (st.getObject() instanceof Literal) {
					l.add(st.getObject().stringValue());
				}
			} else if (st.getPredicate().equals(RDFS.LABEL) && st.getObject() instanceof Literal) {
				labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(HAS_PREFIX_PREDICATE) && st.getObject() instanceof Literal) {
				prefixMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(HAS_PREFIX_LABEL_PREDICATE) && st.getObject() instanceof Literal) {
				prefixLabelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(HAS_REGEX_PREDICATE) && st.getObject() instanceof Literal) {
				regexMap.put((IRI) st.getSubject(), st.getObject().stringValue());
			} else if (st.getPredicate().equals(RDF.SUBJECT) && st.getObject() instanceof IRI) {
				statementSubjects.put((IRI) st.getSubject(), (IRI) st.getObject());
			} else if (st.getPredicate().equals(RDF.PREDICATE) && st.getObject() instanceof IRI) {
				statementPredicates.put((IRI) st.getSubject(), (IRI) st.getObject());
			} else if (st.getPredicate().equals(RDF.OBJECT)) {
				statementObjects.put((IRI) st.getSubject(), st.getObject());
			} else if (st.getPredicate().equals(STATEMENT_ORDER_PREDICATE)) {
				if (st.getObject() instanceof Literal && st.getObject().stringValue().matches("[0-9]+")) {
					statementOrder.put((IRI) st.getSubject(), Integer.valueOf(st.getObject().stringValue()));
				}
			}
		}
		List<IRI> assertionTypes = typeMap.get(assertionIri);
		if (assertionTypes == null || (!assertionTypes.contains(ASSERTION_TEMPLATE_CLASS) &&
				!assertionTypes.contains(PROVENANCE_TEMPLATE_CLASS) && !assertionTypes.contains(PUBINFO_TEMPLATE_CLASS))) {
			throw new MalformedTemplateException("Unknown template type");
		}
		for (List<IRI> l : statementMap.values()) {
			l.sort(statementComparator);
		}
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

	public static IRI getTemplateId(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(WAS_CREATED_FROM_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			return (IRI) st.getObject();
		}
		return null;
	}

	public static IRI getProvenanceTemplateId(Nanopub nanopub) {
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			return (IRI) st.getObject();
		}
		return null;
	}

	public static Set<IRI> getPubinfoTemplateIds(Nanopub nanopub) {
		Set<IRI> iriSet = new HashSet<>();
		for (Statement st : nanopub.getPubinfo()) {
			if (!st.getSubject().equals(nanopub.getUri())) continue;
			if (!st.getPredicate().equals(WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE)) continue;
			if (!(st.getObject() instanceof IRI)) continue;
			iriSet.add((IRI) st.getObject());
		}
		return iriSet;
	}

}
