package com.knowledgepixels.nanodash.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;

import com.knowledgepixels.nanodash.LookupApis;
import com.knowledgepixels.nanodash.Utils;

import net.trustyuri.TrustyUriUtils;

/**
 * Represents a template for creating nanopublications.
 */
public class Template implements Serializable {

    private static final long serialVersionUID = 1L;

    private static ValueFactory vf = SimpleValueFactory.getInstance();
    /**
     * Represents the class for assertion templates.
     */
    public static final IRI ASSERTION_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AssertionTemplate");

    /**
     * Represents the class for provenance templates.
     */
    public static final IRI PROVENANCE_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ProvenanceTemplate");

    /**
     * Represents the class for publication information templates.
     */
    public static final IRI PUBINFO_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/PubinfoTemplate");

    /**
     * Represents the class for unlisted templates.
     */
    public static final IRI UNLISTED_TEMPLATE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UnlistedTemplate");

    /**
     * Predicate indicating a statement in the template.
     */
    public static final IRI HAS_STATEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasStatement");

    /**
     * Represents the class for local resources.
     */
    public static final IRI LOCAL_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LocalResource");

    /**
     * Represents the class for introduced resources.
     */
    public static final IRI INTRODUCED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/IntroducedResource");

    /**
     * Represents the class for embedded resources.
     */
    public static final IRI EMBEDDED_RESOURCE_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/EmbeddedResource");

    /**
     * Represents the class for value placeholders.
     */
    public static final IRI VALUE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ValuePlaceholder");

    /**
     * Represents the class for URI placeholders.
     */
    public static final IRI URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/UriPlaceholder");

    /**
     * Represents the class for auto-escaped URI placeholders.
     */
    public static final IRI AUTO_ESCAPE_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AutoEscapeUriPlaceholder");

    /**
     * Represents the class for external URI placeholders.
     */
    public static final IRI EXTERNAL_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/ExternalUriPlaceholder");

    /**
     * Represents the class for trusty URI placeholders.
     */
    public static final IRI TRUSTY_URI_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/TrustyUriPlaceholder");

    /**
     * Represents the class for literal placeholders.
     */
    public static final IRI LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LiteralPlaceholder");

    /**
     * Represents the class for long literal placeholders.
     */
    public static final IRI LONG_LITERAL_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/LongLiteralPlaceholder");

    /**
     * Represents the class for restricted choice placeholders.
     */
    public static final IRI RESTRICTED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RestrictedChoicePlaceholder");

    /**
     * Represents the class for guided choice placeholders.
     */
    public static final IRI GUIDED_CHOICE_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/GuidedChoicePlaceholder");

    /**
     * Represents the class for agent placeholders.
     */
    public static final IRI AGENT_PLACEHOLDER_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/AgentPlaceholder");

    /**
     * Represents the placeholder for the creator.
     */
    public static final IRI CREATOR_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/CREATOR");

    /**
     * Represents the placeholder for assertions.
     */
    public static final IRI ASSERTION_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/ASSERTION");

    /**
     * Represents the placeholder for nanopublications.
     */
    public static final IRI NANOPUB_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/NANOPUB");

    /**
     * Predicate indicating creation from a template.
     */
    public static final IRI WAS_CREATED_FROM_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromTemplate");

    /**
     * Predicate indicating creation from a provenance template.
     */
    public static final IRI WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromProvenanceTemplate");

    /**
     * Predicate indicating creation from a pubinfo template.
     */
    public static final IRI WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/wasCreatedFromPubinfoTemplate");

    /**
     * Predicate indicating the order of statements.
     */
    public static final IRI STATEMENT_ORDER_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/statementOrder");

    /**
     * Predicate indicating possible values.
     */
    public static final IRI POSSIBLE_VALUE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValue");

    /**
     * Predicate indicating the source of possible values.
     */
    public static final IRI POSSIBLE_VALUES_FROM_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValuesFrom");

    /**
     * Predicate indicating possible values from an API.
     */
    public static final IRI POSSIBLE_VALUES_FROM_API_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/possibleValuesFromApi");

    /**
     * Predicate indicating a datatype for a literal placeholder.
     */
    public static final IRI HAS_DATATYPE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDatatype");

    /**
     * Predicate indicating the language attribute for a literal placeholder.
     */
    public static final IRI HAS_LANGUAGE_TAG_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasLanguageTag");

    /**
     * Predicate indicating a prefix.
     */
    public static final IRI HAS_PREFIX_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefix");

    /**
     * Predicate indicating a regular expression.
     */
    public static final IRI HAS_REGEX_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasRegex");

    /**
     * Predicate indicating a prefix label.
     */
    public static final IRI HAS_PREFIX_LABEL_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasPrefixLabel");

    /**
     * Represents the class for optional statements.
     */
    public static final IRI OPTIONAL_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/OptionalStatement");

    /**
     * Represents the class for grouped statements.
     */
    public static final IRI GROUPED_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/GroupedStatement");

    /**
     * Represents the class for repeatable statements.
     */
    public static final IRI REPEATABLE_STATEMENT_CLASS = vf.createIRI("https://w3id.org/np/o/ntemplate/RepeatableStatement");

    /**
     * Predicate indicating default provenance.
     */
    public static final IRI HAS_DEFAULT_PROVENANCE_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDefaultProvenance");

    /**
     * Predicate indicating required pubinfo elements.
     */
    public static final IRI HAS_REQUIRED_PUBINFO_ELEMENT_PREDICATE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasRequiredPubinfoElement");

    /**
     * Predicate indicating a tag.
     */
    public static final IRI HAS_TAG = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTag");

    /**
     * Predicate indicating a label from an API.
     */
    public static final IRI HAS_LABEL_FROM_API = vf.createIRI("https://w3id.org/np/o/ntemplate/hasLabelFromApi");

    /**
     * Predicate indicating a default value.
     */
    public static final IRI HAS_DEFAULT_VALUE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasDefaultValue");

    /**
     * Predicate indicating a target namespace.
     */
    public static final IRI HAS_TARGET_NAMESPACE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTargetNamespace");

    /**
     * Predicate indicating a nanopublication label pattern.
     */
    public static final IRI HAS_NANOPUB_LABEL_PATTERN = vf.createIRI("https://w3id.org/np/o/ntemplate/hasNanopubLabelPattern");

    /**
     * Predicate indicating a target nanopublication type.
     */
    public static final IRI HAS_TARGET_NANOPUB_TYPE = vf.createIRI("https://w3id.org/np/o/ntemplate/hasTargetNanopubType");

    /**
     * Represents the placeholder for sequence elements.
     */
    public static final IRI SEQUENCE_ELEMENT_PLACEHOLDER = vf.createIRI("https://w3id.org/np/o/ntemplate/SequenceElementPlaceholder");

    /**
     * Default target namespace for templates.
     */
    public static final String DEFAULT_TARGET_NAMESPACE = "https://w3id.org/np/";

    private Nanopub nanopub;
    private String label;
    private String description;

    // TODO: Make all these maps more generic and the code simpler:
    private IRI templateIri;
    private Map<IRI, List<IRI>> typeMap = new HashMap<>();
    private Map<IRI, List<Value>> possibleValueMap = new HashMap<>();
    private Map<IRI, List<IRI>> possibleValuesToLoadMap = new HashMap<>();
    private Map<IRI, List<String>> apiMap = new HashMap<>();
    private Map<IRI, String> labelMap = new HashMap<>();
    private Map<IRI, IRI> datatypeMap = new HashMap<>();
    private Map<IRI, String> languageTagMap = new HashMap<>();
    private Map<IRI, String> prefixMap = new HashMap<>();
    private Map<IRI, String> prefixLabelMap = new HashMap<>();
    private Map<IRI, String> regexMap = new HashMap<>();
    private Map<IRI, List<IRI>> statementMap = new HashMap<>();
    private Map<IRI, IRI> statementSubjects = new HashMap<>();
    private Map<IRI, IRI> statementPredicates = new HashMap<>();
    private Map<IRI, Value> statementObjects = new HashMap<>();
    private Map<IRI, Integer> statementOrder = new HashMap<>();
    private IRI defaultProvenance;
    private List<IRI> requiredPubinfoElements = new ArrayList<>();
    private String tag = null;
    private Map<IRI, Value> defaultValues = new HashMap<>();
    private String targetNamespace = null;
    private String nanopubLabelPattern;
    private List<IRI> targetNanopubTypes = new ArrayList<>();

    /**
     * Creates a Template object from a template id.
     *
     * @param templateId the id of the template, which is the URI of a nanopublication that contains the template definition.
     * @throws RDF4JException             if there is an error retrieving the nanopublication.
     * @throws MalformedTemplateException if the template is malformed or not a valid nanopub template.
     */
    Template(String templateId) throws RDF4JException, MalformedTemplateException {
        nanopub = Utils.getNanopub(templateId);
        processTemplate(nanopub);
    }

    /**
     * Checks if the template is unlisted.
     *
     * @return true if the template is unlisted, false otherwise.
     */
    public boolean isUnlisted() {
        return typeMap.get(templateIri).contains(UNLISTED_TEMPLATE_CLASS);
    }

    /**
     * Returns the Nanopub object representing the template.
     *
     * @return the Nanopub object of the template.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the ID of the template, which is the URI of the nanopublication.
     *
     * @return the ID of the template as a string.
     */
    public String getId() {
        return nanopub.getUri().toString();
    }

    /**
     * Returns the label of the template.
     *
     * @return the label of the template, or a default label if not set.
     */
    public String getLabel() {
        if (label == null) {
            return "Template " + TrustyUriUtils.getArtifactCode(nanopub.getUri().stringValue()).substring(0, 10);
        }
        return label;
    }

    /**
     * Returns the description of the template.
     *
     * @return the description of the template, or an empty string if not set.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the IRI of the template.
     *
     * @param iri the IRI to transform.
     * @return the transformed IRI, or the original IRI if no transformation is needed.
     */
    public String getLabel(IRI iri) {
        iri = transform(iri);
        return labelMap.get(iri);
    }

    /**
     * Returns the IRI of the template, transforming it if necessary.
     *
     * @param iri the IRI to transform.
     * @return the transformed IRI, or the original IRI if no transformation is needed.
     */
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

    /**
     * Returns the datatype for the given literal placeholder IRI.
     *
     * @param iri the literal placeholder IRI.
     * @return the datatype for the literal.
     */
    public IRI getDatatype(IRI iri) {
        iri = transform(iri);
        return datatypeMap.get(iri);
    }

    /**
     * Returns the language tag for the given literal placeholder IRI.
     *
     * @param iri the literal placeholder IRI.
     * @return the language tag for the literal.
     */
    public String getLanguageTag(IRI iri) {
        iri = transform(iri);
        return languageTagMap.get(iri);
    }

    /**
     * Returns the prefix for the given IRI.
     *
     * @param iri the IRI.
     * @return the prefix for the IRI.
     */
    public String getPrefix(IRI iri) {
        iri = transform(iri);
        return prefixMap.get(iri);
    }

    /**
     * Returns the prefix label for a given IRI.
     *
     * @param iri the IRI to get the prefix label for.
     * @return the prefix label for the IRI, or null if not found.
     */
    public String getPrefixLabel(IRI iri) {
        iri = transform(iri);
        return prefixLabelMap.get(iri);
    }

    /**
     * Returns the regex pattern for a given IRI.
     *
     * @param iri the IRI to get the regex for.
     * @return the regex pattern for the IRI, or null if not found.
     */
    public String getRegex(IRI iri) {
        iri = transform(iri);
        return regexMap.get(iri);
    }

    /**
     * Transforms an IRI by removing the artifact code if it is present.
     *
     * @param iri the IRI to transform.
     * @return the transformed IRI, or the original IRI if no transformation is needed.
     */
    public Value getDefault(IRI iri) {
        if (iri.stringValue().matches(".*__[0-9]+")) {
            String baseIri = iri.stringValue().replaceFirst("__[0-9]+$", "");
            Value v = defaultValues.get(vf.createIRI(baseIri));
            if (v instanceof IRI vIri) {
                int repeatSuffix = Integer.parseInt(iri.stringValue().replaceFirst("^.*__([0-9]+)$", "$1"));
                return vf.createIRI(vIri.stringValue() + (repeatSuffix + 1));
            }
        }
        iri = transform(iri);
        return defaultValues.get(iri);
    }

    /**
     * Returns the statement IRIs associated with the template.
     *
     * @return the list of statement IRIs for the template.
     */
    public List<IRI> getStatementIris() {
        return statementMap.get(templateIri);
    }

    /**
     * Returns the statement IRIs associated with a specific group IRI.
     *
     * @param groupIri the IRI of the group for which to retrieve statement IRIs.
     * @return the list of statement IRIs for the specified group IRI, or null if no statements are associated with that group.
     */
    public List<IRI> getStatementIris(IRI groupIri) {
        return statementMap.get(groupIri);
    }

    /**
     * Returns the subject, predicate, and object of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the subject, predicate, and object of the statement as a triple.
     */
    public IRI getSubject(IRI statementIri) {
        return statementSubjects.get(statementIri);
    }

    /**
     * Returns the predicate of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the predicate of the statement, or null if not found.
     */
    public IRI getPredicate(IRI statementIri) {
        return statementPredicates.get(statementIri);
    }

    /**
     * Returns the object of a statement given its IRI.
     *
     * @param statementIri the IRI of the statement to retrieve.
     * @return the object of the statement, or null if not found.
     */
    public Value getObject(IRI statementIri) {
        return statementObjects.get(statementIri);
    }

    /**
     * Checks if the template is a local resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a local resource, false otherwise.
     */
    public boolean isLocalResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(LOCAL_RESOURCE_CLASS);
    }

    /**
     * Checks if the template is an introduced resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an introduced resource, false otherwise.
     */
    public boolean isIntroducedResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(INTRODUCED_RESOURCE_CLASS);
    }

    /**
     * Checks if the template is an embedded resource.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an embedded resource, false otherwise.
     */
    public boolean isEmbeddedResource(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(EMBEDDED_RESOURCE_CLASS);
    }

    /**
     * Checks if the IRI is a value placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a value placeholder, false otherwise.
     */
    public boolean isValuePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(VALUE_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is a URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a URI placeholder, false otherwise.
     */
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

    /**
     * Checks if the IRI is an external URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an external URI placeholder, false otherwise.
     */
    public boolean isExternalUriPlaceholder(IRI iri) {
        iri = transform(iri);
        if (!typeMap.containsKey(iri)) return false;
        for (IRI t : typeMap.get(iri)) {
            if (t.equals(EXTERNAL_URI_PLACEHOLDER_CLASS)) return true;
            if (t.equals(TRUSTY_URI_PLACEHOLDER_CLASS)) return true;
        }
        return false;
    }

    /**
     * Checks if the IRI is a trusty URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a trusty URI placeholder, false otherwise.
     */
    public boolean isTrustyUriPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(TRUSTY_URI_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is an auto-escape URI placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an auto-escape URI placeholder, false otherwise.
     */
    public boolean isAutoEscapePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(AUTO_ESCAPE_URI_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is a literal placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a literal placeholder, false otherwise.
     */
    public boolean isLiteralPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && (typeMap.get(iri).contains(LITERAL_PLACEHOLDER_CLASS) || typeMap.get(iri).contains(LONG_LITERAL_PLACEHOLDER_CLASS));
    }

    /**
     * Checks if the IRI is a long literal placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a long literal placeholder, false otherwise.
     */
    public boolean isLongLiteralPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(LONG_LITERAL_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is a restricted choice placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a restricted choice placeholder, false otherwise.
     */
    public boolean isRestrictedChoicePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(RESTRICTED_CHOICE_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is a guided choice placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a guided choice placeholder, false otherwise.
     */
    public boolean isGuidedChoicePlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(GUIDED_CHOICE_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is an agent placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an agent placeholder, false otherwise.
     */
    public boolean isAgentPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(AGENT_PLACEHOLDER_CLASS);
    }

    /**
     * Checks if the IRI is a sequence element placeholder.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a sequence element placeholder, false otherwise.
     */
    public boolean isSequenceElementPlaceholder(IRI iri) {
        iri = transform(iri);
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(SEQUENCE_ELEMENT_PLACEHOLDER);
    }

    /**
     * Checks if the IRI is a placeholder of any type.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a placeholder, false otherwise.
     */
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

    /**
     * Checks if the IRI is an optional statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is an optional statement, false otherwise.
     */
    public boolean isOptionalStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(OPTIONAL_STATEMENT_CLASS);
    }

    /**
     * Checks if the IRI is a grouped statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a grouped statement, false otherwise.
     */
    public boolean isGroupedStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(GROUPED_STATEMENT_CLASS);
    }

    /**
     * Checks if the IRI is a repeatable statement.
     *
     * @param iri the IRI to check.
     * @return true if the IRI is a repeatable statement, false otherwise.
     */
    public boolean isRepeatableStatement(IRI iri) {
        return typeMap.containsKey(iri) && typeMap.get(iri).contains(REPEATABLE_STATEMENT_CLASS);
    }

    /**
     * Returns the possible values for a given IRI.
     *
     * @param iri the IRI for which to get possible values.
     * @return a list of possible values for the IRI. If no values are found, an empty list is returned.
     */
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

    /**
     * Returns the IRI of the default provenance for the template.
     *
     * @return the IRI of the default provenance, or null if not set.
     */
    public IRI getDefaultProvenance() {
        return defaultProvenance;
    }

    /**
     * Returns the target namespace for the template.
     *
     * @return the target namespace as a string, or null if not set.
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }

    /**
     * Returns the nanopub label pattern.
     *
     * @return the nanopub label pattern as a string, or null if not set.
     */
    public String getNanopubLabelPattern() {
        return nanopubLabelPattern;
    }

    /**
     * Returns the list of target nanopub types.
     *
     * @return a list of IRI objects representing the target nanopub types.
     */
    public List<IRI> getTargetNanopubTypes() {
        return targetNanopubTypes;
    }

    /**
     * Returns the list of the required pubinfo elements for the template.
     *
     * @return a list of IRI objects representing the required pubinfo elements.
     */
    public List<IRI> getRequiredPubinfoElements() {
        return requiredPubinfoElements;
    }

    /**
     * Returns the possible values from an API for a given IRI and search term.
     *
     * @param iri        the IRI for which to get possible values from the API.
     * @param searchterm the search term to filter the possible values.
     * @param labelMap   a map to store labels for the possible values.
     * @return a list of possible values from the API, filtered by the search term.
     */
    public List<String> getPossibleValuesFromApi(IRI iri, String searchterm, Map<String, String> labelMap) {
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

    /**
     * Returns the tag associated with the template.
     *
     * @return the tag as a string, or null if not set.
     */
    public String getTag() {
        return tag;
    }

    private void processTemplate(Nanopub templateNp) throws MalformedTemplateException {
        boolean isNpTemplate = false;
        for (Statement st : templateNp.getAssertion()) {
            if (st.getSubject().equals(templateNp.getAssertionUri()) && st.getPredicate().equals(RDF.TYPE)) {
                if (st.getObject().equals(ASSERTION_TEMPLATE_CLASS) || st.getObject().equals(PROVENANCE_TEMPLATE_CLASS) || st.getObject().equals(PUBINFO_TEMPLATE_CLASS)) {
                    isNpTemplate = true;
                    break;
                }
            }
        }

        if (isNpTemplate) {
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
            } else if (pred.equals(HAS_DATATYPE_PREDICATE) && obj instanceof IRI objIri) {
            	datatypeMap.put(subj, objIri);
            } else if (pred.equals(HAS_LANGUAGE_TAG_PREDICATE) && obj instanceof Literal) {
            	languageTagMap.put(subj, Literals.normalizeLanguageTag(objS));
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
        Map<IRI, Integer> minCounts = new HashMap<>();
        Map<IRI, Integer> maxCounts = new HashMap<>();

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
            } else if (pred.equals(SHACL.HAS_VALUE) && obj instanceof IRI objIri) {
                statementObjects.put(subj, objIri);
            } else if (pred.equals(SHACL.TARGET_CLASS) && obj instanceof IRI objIri) {
                IRI stIri = vf.createIRI(templateNp.getUri() + "/$type");
                statementList.add(stIri);
                List<IRI> l = statementMap.get(subj);
                if (l == null) {
                    l = new ArrayList<>();
                    statementMap.put(subj, l);
                }
                l.add((IRI) stIri);
                statementSubjects.put(stIri, baseSubj);
                statementPredicates.put(stIri, RDF.TYPE);
                statementObjects.put(stIri, objIri);
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
            } else if (pred.equals(HAS_DATATYPE_PREDICATE) && obj instanceof IRI objIri) {
            	datatypeMap.put(subj, objIri);
            } else if (pred.equals(HAS_LANGUAGE_TAG_PREDICATE) && obj instanceof Literal) {
            	languageTagMap.put(subj,  Literals.normalizeLanguageTag(objS));
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
            if (!statementObjects.containsKey(iri)) {
                IRI stObjIri = vf.createIRI(iri.stringValue() + "+obj");
                statementObjects.put(iri, stObjIri);
                addType(stObjIri, VALUE_PLACEHOLDER_CLASS);
                if (!minCounts.containsKey(iri) || minCounts.get(iri) <= 0) {
                    addType(iri, OPTIONAL_STATEMENT_CLASS);
                }
                if (!maxCounts.containsKey(iri) || maxCounts.get(iri) > 1) {
                    addType(iri, REPEATABLE_STATEMENT_CLASS);
                }
            }
        }
        if (!labelMap.containsKey(baseSubj) && typeMap.get(baseSubj).contains(URI_PLACEHOLDER_CLASS) && typeMap.get(baseSubj).contains(LOCAL_RESOURCE_CLASS)) {
            labelMap.put(baseSubj, "short ID as URI suffix");
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

    private void addStatement(IRI thing, IRI type) {
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

        /**
         * Compares two IRIs based on their order in the template.
         *
         * @param arg0 the first object to be compared.
         * @param arg1 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(IRI arg0, IRI arg1) {
            Integer i0 = statementOrder.get(arg0);
            Integer i1 = statementOrder.get(arg1);
            if (i0 == null && i1 == null) return arg0.stringValue().compareTo(arg1.stringValue());
            if (i0 == null) return 1;
            if (i1 == null) return -1;
            return i0 - i1;
        }

    }

}
