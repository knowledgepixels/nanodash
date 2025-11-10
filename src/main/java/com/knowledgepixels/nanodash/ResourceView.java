package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * A class representing a Resource View.
 */
public class ResourceView implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

    public static final IRI RESOURCE_VIEW = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/ResourceView");
    public static final IRI TOP_LEVEL_VIEW_DISPLAY = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/TopLevelViewDisplay");
    public static final IRI PART_LEVEL_VIEW_DISPLAY = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/PartLevelViewDisplay");
    public static final IRI HAS_VIEW_QUERY = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewQuery");
    public static final IRI HAS_VIEW_QUERY_TARGET_FIELD = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewQueryTargetField");
    public static final IRI HAS_VIEW_TARGET_CLASS = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewTargetClass");
    public static final IRI HAS_ELEMENT_NAMESPACE = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasElementNamespace");
    public static final IRI HAS_VIEW_ACTION = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasViewAction");
    public static final IRI HAS_ACTION_TEMPLATE = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasActionTemplate");
    public static final IRI HAS_ACTION_TEMPLATE_TARGET_FIELD = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasActionTemplateTargetField");
    public static final IRI TABULAR_VIEW = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/TabularView");
    public static final IRI LIST_VIEW = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/ListView");
    public static final IRI HAS_ACTION_TEMPLATE_PART_FIELD = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasActionTemplatePartField");

    private static Map<String, ResourceView> resourceViews = new HashMap<>();

    /**
     * Get a ResourceView by its ID.
     *
     * @param id the ID of the ResourceView
     * @return the ResourceView object
     */
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
    private List<IRI> actionList = new ArrayList<>();
    private Set<IRI> targetClasses = new HashSet<>();
    private Set<IRI> elementNamespaces = new HashSet<>();
    private Map<IRI,Template> actionTemplateMap = new HashMap<>();
    private Map<IRI,String> actionTemplateTargetFieldMap = new HashMap<>();
    private Map<IRI,String> actionTemplatePartFieldMap = new HashMap<>();
    private Map<IRI,String> labelMap = new HashMap<>();

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
                    if (st.getObject().equals(TABULAR_VIEW) || st.getObject().equals(LIST_VIEW)) {
                        viewType = (IRI) st.getObject();
                    }
                } else if (st.getPredicate().equals(RDFS.LABEL)) {
                    label = st.getObject().stringValue();
                } else if (st.getPredicate().equals(DCTERMS.TITLE)) {
                    title = st.getObject().stringValue();
                } else if (st.getPredicate().equals(HAS_VIEW_QUERY)) {
                    query = GrlcQuery.get(st.getObject().stringValue());
                } else if (st.getPredicate().equals(HAS_VIEW_QUERY_TARGET_FIELD)) {
                    queryField = st.getObject().stringValue();
                } else if (st.getPredicate().equals(HAS_VIEW_ACTION) && st.getObject() instanceof IRI objIri) {
                    actionList.add(objIri);
                } else if (st.getPredicate().equals(HAS_ELEMENT_NAMESPACE) && st.getObject() instanceof IRI objIri) {
                    elementNamespaces.add(objIri);
                } else if (st.getPredicate().equals(HAS_VIEW_TARGET_CLASS) && st.getObject() instanceof IRI objIri) {
                    targetClasses.add(objIri);
                }
            } else if (st.getPredicate().equals(HAS_ACTION_TEMPLATE)) {
                Template template = TemplateData.get().getTemplate(st.getObject().stringValue());
                actionTemplateMap.put((IRI) st.getSubject(), template);
            } else if (st.getPredicate().equals(HAS_ACTION_TEMPLATE_TARGET_FIELD)) {
                actionTemplateTargetFieldMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(HAS_ACTION_TEMPLATE_PART_FIELD)) {
                actionTemplatePartFieldMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(RDFS.LABEL)) {
                labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            }
        }
        if (!resourceViewTypeFound) throw new IllegalArgumentException("Not a proper resource view nanopub: " + id);
        if (query == null) throw new IllegalArgumentException("Query not found: " + id);
    }

    /**
     * Gets the ID of the ResourceView.
     *
     * @return the ID of the ResourceView
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the Nanopub defining this ResourceView.
     *
     * @return the Nanopub defining this ResourceView
     */
    public Nanopub getNanopub() {
        return nanopub;
    }

    /**
     * Gets the label of the ResourceView.
     *
     * @return the label of the ResourceView
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the title of the ResourceView.
     *
     * @return the title of the ResourceView
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the GrlcQuery associated with the ResourceView.
     *
     * @return the GrlcQuery associated with the ResourceView
     */
    public GrlcQuery getQuery() {
        return query;
    }

    /**
     * Gets the query field of the ResourceView.
     *
     * @return the query field
     */
    public String getQueryField() {
        return queryField;
    }

    /**
     * Gets the list of action IRIs associated with the ResourceView.
     *
     * @return the list of action IRIs
     */
    public List<IRI> getActionList() {
        return actionList;
    }

    /**
     * Gets the Template for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the Template for the action IRI
     */
    public Template getTemplateForAction(IRI actionIri) {
        return actionTemplateMap.get(actionIri);
    }

    /**
     * Gets the template field for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the template field for the action IRI
     */
    public String getTemplateTargetFieldForAction(IRI actionIri) {
        return actionTemplateTargetFieldMap.get(actionIri);
    }

    public String getTemplatePartFieldForAction(IRI actionIri) {
        return actionTemplatePartFieldMap.get(actionIri);
    }

    /**
     * Gets the label for a given action IRI.
     *
     * @param actionIri the action IRI
     * @return the label for the action IRI
     */
    public String getLabelForAction(IRI actionIri) {
        return labelMap.get(actionIri);
    }

    public boolean coversElement(String elementId) {
        for (IRI namespace : elementNamespaces) {
            if (elementId.startsWith(namespace.stringValue())) return true;
        }
        return false;
    }

    /**
     * Checks if the ResourceView has target classes.
     *
     * @return true if the ResourceView has target classes, false otherwise
     */
    public boolean hasTargetClasses() {
        return !targetClasses.isEmpty();
    }

    /**
     * Checks if the ResourceView has a specific target class.
     *
     * @param targetClass the target class IRI
     * @return true if the ResourceView has the target class, false otherwise
     */
    public boolean hasTargetClass(IRI targetClass) {
        return targetClasses.contains(targetClass);
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Gets the view type of the ResourceView.
     *
     * @return the view type mode IRI
     */
    public IRI getViewType() {
        return viewType;
    }

}
