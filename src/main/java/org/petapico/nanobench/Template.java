package org.petapico.nanobench;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.util.file.File;
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
import org.nanopub.NanopubImpl;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

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
				Template t = new Template(entry.get("np"));
				templates.add(t);
				templateMap.put(t.getId(), t);
			}
		} catch (IOException ex) {
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
		if (id.startsWith("file://") || TrustyUriUtils.isPotentialTrustyUri(id)) return new Template(id);
		return null;
	}


	private static ValueFactory vf = SimpleValueFactory.getInstance();
	public static final IRI ASSERTION_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate");
	public static final IRI PROVENANCE_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ProvenanceTemplate");
	public static final IRI PUBINFO_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/PubinfoTemplate");
	public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");
	public static final IRI LOCAL_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LocalResource");
	public static final IRI INTRODUCED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/IntroducedResource");
	public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");
	public static final IRI TRUSTY_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/TrustyUriPlaceholder");
	public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");
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
	public static final IRI HAS_DEFAULT_PROVENANCE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDefaultProvenance");
	public static final IRI HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasRequiredPubinfoElement");


	private Nanopub nanopub;
	private String label;

	// TODO: Make all these maps more generic and the code simpler:
	private Map<IRI,List<IRI>> typeMap = new HashMap<>();
	private Map<IRI,List<Value>> possibleValueMap = new HashMap<>();
	private Map<IRI,List<IRI>> possibleValuesToLoadMap = new HashMap<>();
	private Map<IRI,List<String>> apiMap = new HashMap<>();
	private Map<IRI,String> labelMap = new HashMap<>();
	private Map<IRI,String> prefixMap = new HashMap<>();
	private Map<IRI,String> prefixLabelMap = new HashMap<>();
	private Map<IRI,String> regexMap = new HashMap<>();
	private List<IRI> statementIri;
	private Map<IRI,IRI> statementSubjects = new HashMap<>();
	private Map<IRI,IRI> statementPredicates = new HashMap<>();
	private Map<IRI,Value> statementObjects = new HashMap<>();
	private Map<IRI,Integer> statementOrder = new HashMap<>();
	private IRI defaultProvenance;
	private List<IRI> requiredPubinfoElements = new ArrayList<>();

	private Template(String templateId) {
		if (templateId.startsWith("file://")) {
			try {
				nanopub = new NanopubImpl(new File(templateId.substring(7)));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} else {
			nanopub = Utils.getNanopub(templateId);
		}
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

	public String getRegex(IRI iri) {
		return regexMap.get(iri);
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
		return typeMap.containsKey(iri) && (
				typeMap.get(iri).contains(LOCAL_RESOURCE_CLASS) || typeMap.get(iri).contains(INTRODUCED_RESOURCE_CLASS)
			);
	}

	public boolean isIntroducedResource(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(INTRODUCED_RESOURCE_CLASS);
	}

	public boolean isUriPlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && 
				(typeMap.get(iri).contains(URI_PLACEHOLDER_CLASS) || typeMap.get(iri).contains(TRUSTY_URI_PLACEHOLDER_CLASS));
	}

	public boolean isTrustyUriPlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(TRUSTY_URI_PLACEHOLDER_CLASS);
	}

	public boolean isLiteralPlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(LITERAL_PLACEHOLDER_CLASS);
	}

	public boolean isRestrictedChoicePlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(RESTRICTED_CHOICE_PLACEHOLDER_CLASS);
	}

	public boolean isGuidedChoicePlaceholder(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(GUIDED_CHOICE_PLACEHOLDER_CLASS);
	}

	public boolean isOptionalStatement(IRI iri) {
		return typeMap.containsKey(iri) && typeMap.get(iri).contains(OPTIONAL_STATEMENT_CLASS);
	}

	public List<Value> getPossibleValues(IRI iri) {
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
		List<String> values = new ArrayList<>();
		List<String> apiList = apiMap.get(iri);
		if (apiList != null) {
			for (String apiString : apiList) {
				if (apiString.startsWith("http://purl.org/nanopub/api/find_signed_things?")) {
					List<NameValuePair> urlParams = URLEncodedUtils.parse(apiString.substring(apiString.indexOf("?") + 1), StandardCharsets.UTF_8);
					getPossibleValuesFromNanopubApi(urlParams, searchterm, labelMap, values);
				} else {
					getPossibleValuesFromApi(apiString, searchterm, labelMap, values);
				}
			}
		}
		return values;
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
				String userString = "";
				User user = User.getUserForPubkey(r.get("pubkey"));
				if (user != null) userString = " - by " + user.getShortDisplayName();
				labelMap.put(uri, r.get("label") + desc + userString);
				count++;
				if (count > 9) return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void parseNanopubGrlcApi(JSONObject grlcJsonObject, Map<String,String> labelMap, List<String> values) throws IOException {
		// Aimed to resolve Nanopub grlc API: http://grlc.nanopubs.lod.labs.vu.nl/api/local/local/find_signed_nanopubs_with_text?text=covid
		JSONArray resultsArray = grlcJsonObject.getJSONObject("results").getJSONArray("bindings");
		for (int i = 0; i < resultsArray.length(); i++) {
			JSONObject resultObject = resultsArray.getJSONObject(i);
			// Get the nanopub URI
			String uri = resultObject.getJSONObject("np").getString("value");
			// Get the string which matched with the search term  
			String label = resultObject.getJSONObject("v").getString("value");
			values.add(uri);
			labelMap.put(uri, label);
		}
	}

	private void getPossibleValuesFromApi(String apiString, String searchterm, Map<String,String> labelMap, List<String> values) {
		try {
			HttpGet get = new HttpGet(apiString + URLEncoder.encode(searchterm, StandardCharsets.UTF_8));
			
			// Quick fix to resolve Nanopubs grlc API as JSON
			// Otherwise call fails if no ACCEPT header provided
			// TODO: can also be done using the Nanobench ApiAccess class:
			// nanopubResults = ApiAccess.getAll("find_nanopubs_with_text", nanopubParams).getData();
			if (apiString.startsWith("http://purl.org/nanopub/api/"))
				get.setHeader(HttpHeaders.ACCEPT, "application/json");
			
			HttpResponse resp = HttpClientBuilder.create().build().execute(get);

			if (resp.getStatusLine().getStatusCode() == 405) {
				// Method not allowed, trying POST
				HttpPost post = new HttpPost(apiString + URLEncoder.encode(searchterm, StandardCharsets.UTF_8));
				resp = HttpClientBuilder.create().build().execute(post);
			}
			// TODO: support other content types (CSV, XML, ...)
			// System.err.println(resp.getHeaders("Content-Type")[0]);
			InputStream in = resp.getEntity().getContent();
			String respString = IOUtils.toString(in, StandardCharsets.UTF_8);
			// System.out.println(respString);
			JSONObject json = new JSONObject(respString);

			if (apiString.startsWith("http://purl.org/nanopub/api/")) {
				parseNanopubGrlcApi(json, labelMap, values);
			} else {
				// TODO: create parseJsonApi() ?
				boolean foundId = false;
				for (String key : json.keySet()) {
					if (values.size() > 9) break;
					if (!(json.get(key) instanceof JSONArray)) continue;
					JSONArray a = json.getJSONArray(key);
					for (int i = 0; i < a.length(); i++) {
						if (values.size() > 9) break;
						if (!(a.get(i) instanceof JSONObject)) continue;
						JSONObject o = a.getJSONObject(i);
						String uri = null;
						for (String s : new String[] { "@id", "concepturi", "uri" }) {
							if (o.has(s)) {
								uri = o.get(s).toString();
								foundId = true;
								break;
							}
						}
						if (uri != null) {
							values.add(uri);
							String label = "";
							for (String s : new String[] { "prefLabel", "label" }) {
								if (o.has(s)) {
									label = o.get(s).toString();
									break;
								}
							}
							String desc = "";
							for (String s : new String[] { "definition", "description" }) {
								if (o.has(s)) {
									desc = o.get(s).toString();
									break;
								}
							}
							if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
							if (!label.isEmpty() && !desc.isEmpty()) desc = " - " + desc;
							labelMap.put(uri, label + desc);
						}
					}
				}
				if (foundId == false) {
					// ID key not found, try to get results for following format
					// {result1: ["label 1", "label 2"], result2: ["label 3", "label 4"]}
					for (String key : json.keySet()) {
						if (!(json.get(key) instanceof JSONArray)) continue;
						JSONArray labelArray = json.getJSONArray(key);
						String uri = key;
						String label = "";
						String desc = "";
						if (labelArray.length() > 0) label = labelArray.getString(0);
						if (labelArray.length() > 1) desc = labelArray.getString(1);
						if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
						if (!label.isEmpty() && !desc.isEmpty()) desc = " - " + desc;
						// Quick fix to convert CURIE to URI, as Nanobench only accept URI here
						if (!(uri.startsWith("http://") || uri.startsWith("https://"))) {
							uri = "https://identifiers.org/" + uri;
						}
						values.add(uri);
						labelMap.put(uri, label + desc);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void processTemplate(Nanopub templateNp) {
		Map<IRI,Boolean> statementIriMap = new HashMap<>();
		for (Statement st : templateNp.getAssertion()) {
			if (st.getSubject().equals(templateNp.getAssertionUri())) {
				if (st.getPredicate().equals(RDFS.LABEL)) {
					label = st.getObject().stringValue();
				} else if (st.getObject() instanceof IRI) {
					if (st.getPredicate().equals(HAS_STATEMENT_PREDICATE)) {
						statementIriMap.put((IRI) st.getObject(), true);
					} else if (st.getPredicate().equals(HAS_DEFAULT_PROVENANCE_PREDICATE)) {
						defaultProvenance = (IRI) st.getObject();
					} else if (st.getPredicate().equals(HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE)) {
						requiredPubinfoElements.add((IRI) st.getObject());
					}
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
			}
		}
		List<IRI> assertionTypes = typeMap.get(templateNp.getAssertionUri());
		if (assertionTypes == null || (!assertionTypes.contains(ASSERTION_TEMPLATE_CLASS) &&
				!assertionTypes.contains(PROVENANCE_TEMPLATE_CLASS) && !assertionTypes.contains(PUBINFO_TEMPLATE_CLASS))) {
			throw new RuntimeException("Unknown template type");
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
