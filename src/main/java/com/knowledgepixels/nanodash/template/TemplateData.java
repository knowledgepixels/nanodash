package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.QueryApiAccess;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;
import java.util.*;

public class TemplateData implements Serializable {

    private static final long serialVersionUID = 1L;

    private static TemplateData instance;

    public static void refreshTemplates() {
        instance = new TemplateData();
    }

    public static synchronized void ensureLoaded() {
        if (instance == null) refreshTemplates();
    }

    public static TemplateData get() {
        ensureLoaded();
        return instance;
    }

    private List<ApiResponseEntry> assertionTemplates, provenanceTemplates, pubInfoTemplates;
    private Map<String, Template> templateMap;

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
        System.err.println("Loading templates...");
        for (ApiResponseEntry entry : templateEntries.getData()) {
            if ("true".equals(entry.get("unlisted"))) continue;
            if (!entry.get("np").equals(previousId)) {
                templates.add(entry);
            }
            previousId = entry.get("np");
        }
        Collections.sort(templates, templateComparator);
    }

    public List<ApiResponseEntry> getAssertionTemplates() {
        return assertionTemplates;
    }

    public List<ApiResponseEntry> getProvenanceTemplates() {
        return provenanceTemplates;
    }

    public List<ApiResponseEntry> getPubInfoTemplates() {
        return pubInfoTemplates;
    }

    public Template getTemplate(String id) {
        Template template = templateMap.get(id);
        if (template != null) return template;
        if (TrustyUriUtils.isPotentialTrustyUri(id)) {
            try {
                Template t = new Template(id);
                templateMap.put(id, t);
                return t;
            } catch (Exception ex) {
                System.err.println("Exception: " + ex.getMessage());
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

    public Template getProvenanceTemplate(Nanopub np) {
        IRI templateId = getProvenanceTemplateId(np);
        if (templateId == null) return null;
        return getTemplate(templateId.stringValue());
    }

    public Set<Template> getPubinfoTemplates(Nanopub np) {
        Set<Template> templates = new HashSet<>();
        for (IRI id : getPubinfoTemplateIds(np)) {
            templates.add(getTemplate(id.stringValue()));
        }
        return templates;
    }

    public IRI getTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            return (IRI) st.getObject();
        }
        return null;
    }

    public IRI getProvenanceTemplateId(Nanopub nanopub) {
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_PROVENANCE_TEMPLATE_PREDICATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            return (IRI) st.getObject();
        }
        return null;
    }

    public Set<IRI> getPubinfoTemplateIds(Nanopub nanopub) {
        Set<IRI> iriSet = new HashSet<>();
        for (Statement st : nanopub.getPubinfo()) {
            if (!st.getSubject().equals(nanopub.getUri())) continue;
            if (!st.getPredicate().equals(Template.WAS_CREATED_FROM_PUBINFO_TEMPLATE_PREDICATE)) continue;
            if (!(st.getObject() instanceof IRI)) continue;
            iriSet.add((IRI) st.getObject());
        }
        return iriSet;
    }


    private static final TemplateComparator templateComparator = new TemplateComparator();

    private static class TemplateComparator implements Comparator<ApiResponseEntry>, Serializable {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(ApiResponseEntry o1, ApiResponseEntry o2) {
            return o1.get("label").compareTo(o2.get("label"));
        }

    }

}
