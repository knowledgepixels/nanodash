package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * A class representing a Resource View.
 */
public class ResourceView implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceView.class);

    static Map<IRI, Integer> columnWidths = new HashMap<>();

    static {
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_1_OF_12, 1);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_2_OF_12, 2);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_3_OF_12, 3);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_4_OF_12, 4);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_5_OF_12, 5);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_6_OF_12, 6);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_7_OF_12, 7);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_8_OF_12, 8);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_9_OF_12, 9);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_10_OF_12, 10);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_11_OF_12, 11);
        columnWidths.put(KPXL_TERMS.COLUMN_WIDTH_12_OF_12, 12);
    }

    private static Map<String, ResourceView> resourceViews = new HashMap<>();

    /**
     * Get a ResourceView by its ID.
     *
     * @param id the ID of the ResourceView
     * @return the ResourceView object
     */
    public static ResourceView get(String id) {
        String npId = id.replaceFirst("^(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
        // Automatically selecting latest version of view definition:
        // TODO This should be made configurable at some point, so one can make it a fixed version.
        String latestNpId = QueryApiAccess.getLatestVersionId(npId);
        String latestId = id;
        Nanopub np = Utils.getAsNanopub(latestNpId);
        if (!latestNpId.equals(npId)) {
            Set<String> embeddedIris = NanopubUtils.getEmbeddedIriIds(np);
            if (embeddedIris.size() == 1) {
                latestId = embeddedIris.iterator().next();
            } else {
                latestNpId = npId;
                np = Utils.getAsNanopub(npId);
            }
        }
        if (!resourceViews.containsKey(latestId)) {
            try {
                resourceViews.put(latestId, new ResourceView(latestId, np));
            } catch (Exception ex) {
                logger.error("Couldn't load nanopub for resource: " + id, ex);
            }
        }
        return resourceViews.get(latestId);
    }

    private String id;
    private Nanopub nanopub;
    private IRI viewKind;
    private String label;
    private String title = "View";
    private GrlcQuery query;
    private String queryField = "resource";
    private Integer pageSize;
    private Integer displayWidth;
    private String structuralPosition;
    private List<IRI> viewResultActionList = new ArrayList<>();
    private List<IRI> viewEntryActionList = new ArrayList<>();
    private Set<IRI> appliesToClasses = new HashSet<>();
    private Set<IRI> appliesToNamespaces = new HashSet<>();
    private Map<IRI, Template> actionTemplateMap = new HashMap<>();
    private Map<IRI, String> actionTemplateTargetFieldMap = new HashMap<>();
    private Map<IRI, IRI> actionTemplateTypeMap = new HashMap<>();
    private Map<IRI, String> actionTemplatePartFieldMap = new HashMap<>();
    private Map<IRI, String> actionTemplateQueryMappingMap = new HashMap<>();
    private Map<IRI, String> labelMap = new HashMap<>();
    private IRI viewType;

    private ResourceView(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;
        List<IRI> actionList = new ArrayList<>();
        boolean resourceViewTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(id)) {
                if (st.getPredicate().equals(RDF.TYPE)) {
                    if (st.getObject().equals(KPXL_TERMS.RESOURCE_VIEW)) {
                        resourceViewTypeFound = true;
                    }
                    if (st.getObject().equals(KPXL_TERMS.TABULAR_VIEW) || st.getObject().equals(KPXL_TERMS.LIST_VIEW) || st.getObject().equals(KPXL_TERMS.PLAIN_PARAGRAPH_VIEW)) {
                        viewType = (IRI) st.getObject();
                    }
                } else if (st.getPredicate().equals(DCTERMS.IS_VERSION_OF) && st.getObject() instanceof IRI objIri) {
                    viewKind = objIri;
                } else if (st.getPredicate().equals(RDFS.LABEL)) {
                    label = st.getObject().stringValue();
                } else if (st.getPredicate().equals(DCTERMS.TITLE)) {
                    title = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_QUERY)) {
                    query = GrlcQuery.get(st.getObject().stringValue());
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_QUERY_TARGET_FIELD)) {
                    queryField = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_ACTION) && st.getObject() instanceof IRI objIri) {
                    actionList.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_NAMESPACE) && st.getObject() instanceof IRI objIri) {
                    appliesToNamespaces.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_INSTANCES_OF) && st.getObject() instanceof IRI objIri) {
                    appliesToClasses.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW_TARGET_CLASS) && st.getObject() instanceof IRI objIri) {
                    // Deprecated
                    appliesToClasses.add(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PAGE_SIZE) && st.getObject() instanceof Literal objL) {
                    try {
                        pageSize = Integer.parseInt(objL.stringValue());
                    } catch (NumberFormatException ex) {
                        logger.error("Invalid page size value: " + objL.stringValue(), ex);
                    }
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_DISPLAY_WIDTH) && st.getObject() instanceof IRI objIri) {
                    displayWidth = columnWidths.get(objIri);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_STRUCTURAL_POSITION) && st.getObject() instanceof Literal objL) {
                    structuralPosition = objL.stringValue();
                }
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE)) {
                Template template = TemplateData.get().getTemplate(st.getObject().stringValue());
                actionTemplateMap.put((IRI) st.getSubject(), template);
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_TARGET_FIELD)) {
                actionTemplateTargetFieldMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_PART_FIELD)) {
                actionTemplatePartFieldMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ACTION_TEMPLATE_QUERY_MAPPING)) {
                actionTemplateQueryMappingMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(RDFS.LABEL)) {
                labelMap.put((IRI) st.getSubject(), st.getObject().stringValue());
            } else if (st.getPredicate().equals(RDF.TYPE)) {
                if (st.getObject().equals(KPXL_TERMS.VIEW_ACTION) || st.getObject().equals(KPXL_TERMS.VIEW_ENTRY_ACTION)) {
                    actionTemplateTypeMap.put((IRI) st.getSubject(), (IRI) st.getObject());
                }
            }
        }
        for (IRI actionIri : actionList) {
            if (actionTemplateTypeMap.containsKey(actionIri) && actionTemplateTypeMap.get(actionIri).equals(KPXL_TERMS.VIEW_ENTRY_ACTION)) {
                viewEntryActionList.add(actionIri);
            } else {
                viewResultActionList.add(actionIri);
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

    public IRI getViewKindIri() {
        return viewKind;
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
     * Returns the preferred page size.
     *
     * @return page size (0 = everything on first page)
     */
    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getDisplayWidth() {
        return displayWidth;
    }

    public String getStructuralPosition() {
        return structuralPosition;
    }

    /**
     * Gets the list of action IRIs associated with the ResourceView.
     *
     * @return the list of action IRIs
     */
    public List<IRI> getViewResultActionList() {
        return viewResultActionList;
    }

    public List<IRI> getViewEntryActionList() {
        return viewEntryActionList;
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

    public String getTemplateQueryMapping(IRI actionIri) {
        return actionTemplateQueryMappingMap.get(actionIri);
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

    public boolean appliesTo(String resourceId, Set<IRI> classes) {
        for (IRI namespace : appliesToNamespaces) {
            if (resourceId.startsWith(namespace.stringValue())) return true;
        }
        if (classes != null) {
            for (IRI c : classes) {
                if (appliesToClasses.contains(c)) return true;
            }
        }
        return false;
    }

    /**
     * Checks if the ResourceView has target classes.
     *
     * @return true if the ResourceView has target classes, false otherwise
     */
    public boolean appliesToClasses() {
        return !appliesToClasses.isEmpty();
    }

    /**
     * Checks if the ResourceView has a specific target class.
     *
     * @param targetClass the target class IRI
     * @return true if the ResourceView has the target class, false otherwise
     */
    public boolean appliesToClass(IRI targetClass) {
        return appliesToClasses.contains(targetClass);
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
