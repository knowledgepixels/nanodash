package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.QueryApiAccess;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Singleton class that manages templates data.
 */
public class TemplateData implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TemplateData.class);

    private static TemplateData instance;

    /**
     * Refreshes the templates data by creating a new instance of TemplateData.
     */
    public static synchronized void refreshTemplates() {
        instance = new TemplateData();
    }

    /**
     * Ensures that the TemplateData instance is loaded.
     */
    public static synchronized void ensureLoaded() {
        if (instance == null) refreshTemplates();
    }

    /**
     * Gets the singleton instance of TemplateData.
     *
     * @return the TemplateData instance
     */
    public static TemplateData get() {
        ensureLoaded();
        return instance;
    }

    private List<ApiResponseEntry> assertionTemplates, provenanceTemplates, pubInfoTemplates;
    private Map<String, Template> templateMap;

    /**
     * Constructor to initialize the TemplateData instance.
     */
    public TemplateData() {
        assertionTemplates = new ArrayList<>();
        provenanceTemplates = new ArrayList<>();
        pubInfoTemplates = new ArrayList<>();
        templateMap = new HashMap<>();
        refreshTemplates(assertionTemplates, "get-assertion-templates");
        refreshTemplates(provenanceTemplates, "get-provenance-templates");
        refreshTemplates(pubInfoTemplates, "get-pubinfo-templates");
    }

    private void refreshTemplates(List<ApiResponseEntry> templates, String queryId) {
        ApiResponse templateEntries = QueryApiAccess.forcedGet(queryId);
        String previousId = null;
        logger.info("Loading templates...");
        for (ApiResponseEntry entry : templateEntries.getData()) {
            if ("true".equals(entry.get("unlisted"))) continue;
            if (!entry.get("np").equals(previousId)) {
                templates.add(entry);
            }
            previousId = entry.get("np");
        }
        Collections.sort(templates, templateComparator);
    }

    /**
     * Returns the list of assertion templates.
     *
     * @return a list of assertion templates
     */
    public List<ApiResponseEntry> getAssertionTemplates() {
        return assertionTemplates;
    }

    /**
     * Returns the list of provenance templates.
     *
     * @return a list of provenance templates
     */
    public List<ApiResponseEntry> getProvenanceTemplates() {
        return provenanceTemplates;
    }

    /**
     * Returns the list of publication information templates.
     *
     * @return a list of publication information templates
     */
    public List<ApiResponseEntry> getPubInfoTemplates() {
        return pubInfoTemplates;
    }

    /**
     * Returns a Template object for the given template ID.
     *
     * @param id the ID of the template
     * @return the Template object if found, or null if not found or invalid
     */
    public Template getTemplate(String id) {
        Template template = templateMap.get(id);
        if (template != null) return template;
        if (TrustyUriUtils.isPotentialTrustyUri(id)) {
            try {
                Template t = new Template(id);
                templateMap.put(id, t);
                return t;
            } catch (Exception ex) {
                logger.error("Exception: {}", ex.getMessage());
                return null;
            }
        }
        return null;
    }

    public Template getTemplate(Nanopub np) {
        IRI templateId = getTemplateId(np);
        if (templateId == null) return null;
        return getTemplate(templateId.stringValue());
    }

    /**
     * Returns a Template object for the provenance template of the given Nanopub.
     *
     * @param np the Nanopub from which to extract the provenance template
     * @return the Template object if found, or null if not found or invalid
     */
    public Template getProvenanceTemplate(Nanopub np) {
        IRI templateId = getProvenanceTemplateId(np);
        if (templateId == null) return null;
        return getTemplate(templateId.stringValue());
    }

    /**
     * Returns a set of Template objects for the publication information templates of the given Nanopub.
     *
     * @param np the Nanopub from which to extract the publication information templates
     * @return a set of Template objects
     */
    public Set<Template> getPubinfoTemplates(Nanopub np) {
        Set<Template> templates = new HashSet<>();
        for (IRI id : getPubinfoTemplateIds(np)) {
            templates.add(getTemplate(id.stringValue()));
        }
        return templates;
    }

    /**
     * Returns the template ID of the given Nanopub.
     *
     * @param nanopub the Nanopub from which to extract the template ID
     * @return the IRI of the template ID, or null if not found
     */
    public IRI getTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            return (IRI) st.getObject();
        }
        return null;
    }

    /**
     * Returns the provenance template ID of the given Nanopub.
     *
     * @param nanopub the Nanopub from which to extract the provenance template ID
     * @return the IRI of the provenance template ID, or null if not found
     */
    public IRI getProvenanceTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            return (IRI) st.getObject();
        }
        return null;
    }

    /**
     * Returns the set of publication information template IDs for the given Nanopub.
     *
     * @param nanopub the Nanopub from which to extract the publication information template IDs
     * @return a set of IRI objects representing the publication information template IDs
     */
    public Set<IRI> getPubinfoTemplateIds(Nanopub nanopub) {
        Set<IRI> iriSet = new HashSet<>();
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            iriSet.add((IRI) st.getObject());
        }
        return iriSet;
    }


    private static final TemplateComparator templateComparator = new TemplateComparator();

    private static class TemplateComparator implements Comparator<ApiResponseEntry>, Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Compares two Template objects based on their labels.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            return o1.get("label").compareTo(o2.get("label"));
        }

    }

}
