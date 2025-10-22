package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceView implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

    private static final IRI RESOURCE_VIEW = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/ResourceView");
    private static final IRI HAS_VIEW_QUERY = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewQuery");
    private static final IRI HAS_VIEW_QUERY_TARGET_FIELD = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewQueryTargetField");

    private static Map<String, ResourceView> resourceViews = new HashMap<>();

    public static ResourceView get(String id) {
        if (!resourceViews.containsKey(id)) {
            try {
                Nanopub np = Utils.getAsNanopub(id.replaceFirst("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$2"));
                resourceViews.put(id, new ResourceView(id, np));
            } catch (Exception ex) {
                logger.error("Couldn't load nanopub for resource: " + id, ex);
            }
        }
        return resourceViews.get(id);
    }

    private String id;
    private Nanopub nanopub;
    private String label;
    private String title = "View";
    private GrlcQuery query;
    private String queryField = "resource";

    private ResourceView(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;
        boolean resourceViewTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(id)) {
                if (st.getPredicate().equals(RDF.TYPE)) {
                    if (st.getObject().equals(RESOURCE_VIEW)) {
                        resourceViewTypeFound = true;
                    }
                } else if (st.getPredicate().equals(RDFS.LABEL)) {
                    label = st.getObject().stringValue();
                } else if (st.getPredicate().equals(DCTERMS.TITLE)) {
                    title = st.getObject().stringValue();
                } else if (st.getPredicate().equals(HAS_VIEW_QUERY)) {
                    query = GrlcQuery.get(st.getObject().stringValue());
                } else if (st.getPredicate().equals(HAS_VIEW_QUERY_TARGET_FIELD)) {
                    queryField = st.getObject().stringValue();
                }
            }
        }
        if (!resourceViewTypeFound) throw new IllegalArgumentException("Not a proper resource view nanopub: " + id);
        if (query == null) throw new IllegalArgumentException("Query not found: " + id);
    }

    public String getId() {
        return id;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public String getLabel() {
        return label;
    }

    public String getTitle() {
        return title;
    }

    public GrlcQuery getQuery() {
        return query;
    }

    public String getQueryField() {
        return queryField;
    }

}
