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
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.nanopub.Nanopub;

import java.io.Serializable;
import java.util.*;


public class GrlcQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Map<String, GrlcQuery> instanceMap = new HashMap<>();

    public static GrlcQuery get(String id) {
        if (!instanceMap.containsKey(id)) {
            instanceMap.put(id, new GrlcQuery(id));
        }
        return instanceMap.get(id);
    }

    public final static IRI GRLC_QUERY_CLASS = Utils.vf.createIRI("https://w3id.org/kpxl/grlc/grlc-query");
    public final static IRI GRLC_HAS_SPARQL = Utils.vf.createIRI("https://w3id.org/kpxl/grlc/sparql");
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
            if (id.matches("https?://.*[^A-Za-z0-9-_]RA[^A-Za-z0-9-_]{43}[/#][^/#]+")) {
                queryId = id.replaceFirst("^https?://.*[^A-Za-z0-9-_](RA[A-Za-z0-9-_]{43}[/#][^/#]+)$", "$1").replace("#", "/");
            } else if (id.matches(id)) {
                queryId = id.replace("#", "/");
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
        query.getTupleExpr().visitChildren(new AbstractSimpleQueryModelVisitor<>() {

            @Override
            public void meet(Var node) throws RuntimeException {
                super.meet(node);
                if (!node.isConstant() && !node.isAnonymous() && node.getName().startsWith("_")) {
                    placeholders.add(node.getName());
                }
            }

        });
        List<String> placeholdersListPre = new ArrayList<>(placeholders);
        Collections.sort(placeholdersListPre);
        placeholdersList = Collections.unmodifiableList(placeholdersListPre);
    }

    public String getQueryId() {
        return queryId;
    }

    public String getArtifactCode() {
        return artifactCode;
    }

    public String getQuerySuffix() {
        return querySuffix;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public IRI getQueryUri() {
        return queryUri;
    }

    public String getSparql() {
        return sparql;
    }

    public IRI getEndpoint() {
        return endpoint;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPlaceholdersList() {
        return placeholdersList;
    }

    public List<QueryParamField> createParamFields(String markupId) {
        List<QueryParamField> l = new ArrayList<>();
        for (String s : placeholdersList) {
            l.add(new QueryParamField(markupId, s));
        }
        return l;
    }

}
