package com.knowledgepixels.nanodash;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.vocabulary.NTEMPLATE;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

/**
 * Class representing a Nanodash project.
 */
public class Project implements Serializable {

    /**
     * The predicate for the owner of the project.
     */
    public static final IRI HAS_OWNER = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasOwner");

    /**
     * The predicate for pinned templates in the project.
     */
    public static final IRI HAS_PINNED_TEMPLATE = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedTemplate");

    /**
     * The predicate for pinned queries in the project.
     */
    public static final IRI HAS_PINNED_QUERY = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedQuery");

    private static List<Project> projectList = new ArrayList<>();
    private static ConcurrentMap<String,Project> projectsByCoreInfo = new ConcurrentHashMap<>();
    private static ConcurrentMap<String,Project> projectsById = new ConcurrentHashMap<>();

    public static synchronized void refresh(ApiResponse resp) {
        projectList.clear();
        ConcurrentMap<String,Project> prevProjectsByCoreInfoPrev = projectsByCoreInfo;
        projectsByCoreInfo = new ConcurrentHashMap<>();
        projectsById.clear();
        for (ApiResponseEntry entry : resp.getData()) {
            Project project = new Project(entry.get("project"), entry.get("label"), entry.get("np"));
            Project prevProject = prevProjectsByCoreInfoPrev.get(project.getCoreInfoString());
            if (prevProject != null) project = prevProject;
            projectList.add(project);
            projectsByCoreInfo.put(project.getCoreInfoString(), project);
            projectsById.put(project.getId(), project);
        }
    }

    public static List<Project> getProjectList() {
        return projectList;
    }

    public static Project get(String id) {
        return projectsById.get(id);
    }

    private String id, label, rootNanopubId;
    private Nanopub rootNanopub = null;

    private String description = null;
    private List<IRI> owners = new ArrayList<>();
    private List<Template> templates = new ArrayList<>();
    private Set<String> templateTags = new HashSet<>();
    private ConcurrentMap<String, List<Template>> templatesPerTag = new ConcurrentHashMap<>();
    private List<IRI> queryIds = new ArrayList<>();
    private IRI defaultProvenance = null;

    private Project(String id, String label, String rootNanopubId) {
        this.id = id;
        this.label = label;
        this.rootNanopubId = rootNanopubId;
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);

        for (Statement st : rootNanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(getId())) {
                if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    description = st.getObject().stringValue();
                } else if (st.getPredicate().equals(HAS_OWNER) && st.getObject() instanceof IRI obj) {
                    owners.add(obj);
                } else if (st.getPredicate().equals(HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
                    templates.add(TemplateData.get().getTemplate(obj.stringValue()));
                } else if (st.getPredicate().equals(HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
                    queryIds.add(obj);
                } else if (st.getPredicate().equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE) && st.getObject() instanceof IRI obj) {
                    defaultProvenance = obj;
                }
            } else if (st.getPredicate().equals(NTEMPLATE.HAS_TAG) && st.getObject() instanceof Literal l) {
                templateTags.add(l.stringValue());
                List<Template> list = templatesPerTag.get(l.stringValue());
                if (list == null) {
                    list = new ArrayList<>();
                    templatesPerTag.put(l.stringValue(), list);
                }
                list.add(TemplateData.get().getTemplate(st.getSubject().stringValue()));
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getRootNanopubId() {
        return rootNanopubId;
    }

    public String getCoreInfoString() {
        return id + " " + rootNanopubId;
    }

    public Nanopub getRootNanopub() {
        return rootNanopub;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public List<IRI> getOwners() {
        return owners;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public Set<String> getTemplateTags() {
        return templateTags;
    }

    public ConcurrentMap<String, List<Template>> getTemplatesPerTag() {
        return templatesPerTag;
    }

    public List<IRI> getQueryIds() {
        return queryIds;
    }

    public IRI getDefaultProvenance() {
        return defaultProvenance;
    }
}
