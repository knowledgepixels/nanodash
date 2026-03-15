package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.component.QueryParamField;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents a GRLC query extracted from a nanopublication.
 * This class parses the query details, including SPARQL, endpoint, label, description, and placeholders.
 */
public class GrlcQuery implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GrlcQuery.class);

    private static final Cache<String, GrlcQuery> instanceMap = CacheBuilder.newBuilder()
        .maximumSize(5_000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    /**
     * Returns a singleton instance of GrlcQuery for the given QueryRef.
     *
     * @param ref the QueryRef object containing the query name
     * @return a GrlcQuery instance
     */
    public static GrlcQuery get(QueryRef ref) {
        return get(ref.getQueryId());
    }

    /**
     * Returns a singleton instance of GrlcQuery for the given query ID.
     *
     * @param id the unique identifier or URI of the query
     * @return a GrlcQuery instance
     */
    public static GrlcQuery get(String id) {
        if (id == null) return null;
        GrlcQuery cached = instanceMap.getIfPresent(id);
        if (cached == null) {
            try {
                GrlcQuery q = new GrlcQuery(id);
                id = q.getQueryId();
                cached = instanceMap.getIfPresent(id);
                if (cached != null) return cached;
                instanceMap.put(id, q);
                cached = q;
            } catch (Exception ex) {
                logger.error("Could not load query: {}", id, ex);
            }
        }
        return cached;
    }

    /**
     * The IRI for the GRLC query class and properties.
     */
    public final static IRI GRLC_QUERY_CLASS = Utils.vf.createIRI("https://w3id.org/kpxl/grlc/grlc-query");

    /**
     * The IRI for the SPARQL property and endpoint property in GRLC queries.
     */
    public final static IRI GRLC_HAS_SPARQL = Utils.vf.createIRI("https://w3id.org/kpxl/grlc/sparql");

    /**
     * The IRI for the endpoint property in GRLC queries.
     */
    public final static IRI GRLC_HAS_ENDPOINT = Utils.vf.createIRI("https://w3id.org/kpxl/grlc/endpoint");

    private final String queryId;
    private final String artifactCode;
    private final String querySuffix;
    private final Nanopub nanopub;
    private IRI queryUri;
    private String sparql;
    private IRI endpoint;
    private String label;
    private String description;
    private final List<String> placeholdersList;
    private boolean constructQuery;

    /**
     * Constructs a GrlcQuery object by parsing the provided query ID or URI.
     *
     * @param id The query ID or URI.
     * @throws IllegalArgumentException If the ID is null, invalid, or the nanopublication defines multiple queries.
     */
    private GrlcQuery(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Null value for query ID");
        }
        if (TrustyUriUtils.isPotentialTrustyUri(id)) {
            artifactCode = TrustyUriUtils.getArtifactCode(id);
            nanopub = Utils.getNanopub(artifactCode);
            for (Statement st : nanopub.getAssertion()) {
                if (st.getPredicate().equals(RDF.TYPE) && st.getObject().equals(GRLC_QUERY_CLASS)) {
                    if (queryUri != null) {
                        throw new IllegalArgumentException("Nanopublication defines more than one query: " + id);
                    }
                    queryUri = (IRI) st.getSubject();
                }
            }
            if (queryUri == null) {
                throw new IllegalArgumentException("No query found in nanopublication: " + id);
            }
            queryId = queryUri.stringValue().replaceFirst("^https?://.*[^A-Za-z0-9-_](RA[A-Za-z0-9-_]{43}[/#][^/#]+)$", "$1").replace("#", "/");
        } else {
            if (id.matches("https?://.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43}[/#][^/#]+")) {
                queryId = id.replaceFirst("^https?://.*[^A-Za-z0-9-_](RA[A-Za-z0-9-_]{43}[/#][^/#]+)$", "$1").replace("#", "/");
            } else if (id.matches("RA[A-Za-z0-9-_]{43}[/#][^/#]+")) {
                queryId = id;
            } else {
                throw new IllegalArgumentException("Not a valid query ID or URI: " + id);
            }
            artifactCode = queryId.replaceFirst("[/#].*$", "");
            nanopub = Utils.getNanopub(artifactCode);
        }
        querySuffix = queryId.replaceFirst("^.*[/#]", "");
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().stringValue().replace("#", "/").endsWith(queryId)) continue;
            queryUri = (IRI) st.getSubject();
            if (st.getPredicate().equals(GRLC_HAS_SPARQL) && st.getObject() instanceof Literal objLiteral) {
                sparql = objLiteral.stringValue();
            } else if (st.getPredicate().equals(GRLC_HAS_ENDPOINT) && st.getObject() instanceof IRI objIri) {
                endpoint = objIri;
            } else if (st.getPredicate().equals(RDFS.LABEL)) {
                label = st.getObject().stringValue();
            } else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                description = st.getObject().stringValue();
            }
        }

        final Set<String> placeholders = new HashSet<>();
        ParsedQuery query = new SPARQLParser().parseQuery(sparql, null);
        constructQuery = query instanceof ParsedGraphQuery;
        try {
            query.getTupleExpr().visitChildren(new AbstractSimpleQueryModelVisitor<Exception>() {

                @Override
                public void meet(Var node) throws Exception {
                    super.meet(node);
                    if (!node.isConstant() && !node.isAnonymous() && node.getName().startsWith("_")) {
                        placeholders.add(node.getName());
                    }
                }

            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String> placeholdersListPre = new ArrayList<>(placeholders);
        Collections.sort(placeholdersListPre);
        placeholdersList = Collections.unmodifiableList(placeholdersListPre);
    }

    /**
     * Returns the unique query ID.
     *
     * @return The query ID.
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Returns the artifact code extracted from the nanopublication.
     *
     * @return The artifact code.
     */
    public String getArtifactCode() {
        return artifactCode;
    }

    /**
     * Returns the suffix of the query.
     *
     * @return The query suffix.
     */
    public String getQuerySuffix() {
        return querySuffix;
    }

    /**
     * Returns the nanopublication containing the query.
     *
     * @return The nanopublication.
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Returns the URI of the query.
     *
     * @return The query URI.
     */
    public IRI getQueryUri() {
        return queryUri;
    }

    /**
     * Returns the SPARQL query string.
     *
     * @return The SPARQL query.
     */
    public String getSparql() {
        return sparql;
    }

    /**
     * Returns the endpoint URI for the query.
     *
     * @return The endpoint URI.
     */
    public IRI getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the label of the query.
     *
     * @return The query label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the description of the query.
     *
     * @return The query description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns a list of placeholders in the query.
     *
     * @return The list of placeholders.
     */
    public List<String> getPlaceholdersList() {
        return placeholdersList;
    }

    /**
     * Returns true if this is a CONSTRUCT query (returns RDF graph data instead of tabular data).
     *
     * @return true if CONSTRUCT query
     */
    public boolean isConstructQuery() {
        return constructQuery;
    }

    /**
     * Creates a list of query parameter fields for the placeholders in the query.
     *
     * @param markupId The markup ID for the fields.
     * @return A list of query parameter fields.
     */
    public List<QueryParamField> createParamFields(String markupId) {
        List<QueryParamField> l = new ArrayList<>();
        for (String s : placeholdersList) {
            l.add(new QueryParamField(markupId, s));
        }
        return l;
    }

    // NOTE: The following methods are duplicated from nanopub-query's GrlcSpec.java.
    // They should eventually be moved to nanopub-java to avoid duplication.

    /**
     * Expands the SPARQL query by substituting placeholder values from the given param fields.
     * Unlike the server-side version in nanopub-query, missing mandatory params are simply skipped
     * (not thrown as errors) to support partial substitution for the Yasgui link.
     *
     * @param paramFields the list of query parameter fields with user-entered values
     * @return the expanded SPARQL query string
     */
    public String expandQuery(List<QueryParamField> paramFields) {
        Map<String, QueryParamField> fieldMap = new HashMap<>();
        for (QueryParamField f : paramFields) {
            fieldMap.put(f.getParamName(), f);
        }
        String expandedQueryContent = sparql;
        for (String ph : placeholdersList) {
            String paramName = QueryParamField.getParamName(ph);
            QueryParamField field = fieldMap.get(paramName);
            if (field == null || !field.isSet()) continue;
            if (QueryParamField.isMultiPlaceholder(ph)) {
                String[] values = field.getValues();
                StringBuilder valueList = new StringBuilder();
                for (String v : values) {
                    if (isIriPlaceholder(ph)) {
                        valueList.append(serializeIri(v)).append(" ");
                    } else {
                        valueList.append(serializeLiteral(v)).append(" ");
                    }
                }
                expandedQueryContent = expandedQueryContent.replaceAll(
                    "values\\s*\\?" + ph + "\\s*\\{\\s*\\}",
                    "values ?" + ph + " { " + escapeSlashes(valueList.toString()) + "}"
                );
            } else {
                String val = field.getValues()[0];
                if (isIriPlaceholder(ph)) {
                    expandedQueryContent = expandedQueryContent.replaceAll("\\?" + ph + "(?![A-Za-z0-9_])", escapeSlashes(serializeIri(val)));
                } else {
                    expandedQueryContent = expandedQueryContent.replaceAll("\\?" + ph + "(?![A-Za-z0-9_])", escapeSlashes(serializeLiteral(val)));
                }
            }
        }
        return expandedQueryContent;
    }

    /**
     * Returns true if all mandatory (non-optional) param fields have values set.
     *
     * @param paramFields the list of query parameter fields
     * @return true if all mandatory fields are set
     */
    public static boolean allMandatoryFieldsSet(List<QueryParamField> paramFields) {
        for (QueryParamField f : paramFields) {
            if (!f.isOptional() && !f.isSet()) return false;
        }
        return true;
    }

    private static boolean isIriPlaceholder(String placeholder) {
        return placeholder.endsWith("_iri");
    }

    private static String escapeLiteral(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"");
    }

    private static String serializeIri(String iriString) {
        return "<" + iriString + ">";
    }

    private static String serializeLiteral(String literalString) {
        return "\"" + escapeLiteral(literalString) + "\"";
    }

    private static String escapeSlashes(String string) {
        return string.replace("\\", "\\\\");
    }

}
