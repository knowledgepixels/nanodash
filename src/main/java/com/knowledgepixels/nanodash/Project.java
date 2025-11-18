package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.knowledgepixels.nanodash.Utils.vf;

/**
 * Class representing a Nanodash project.
 */
public class Project implements Serializable {

    private static List<Project> projectList = null;
    private static ConcurrentMap<String, Project> projectsByCoreInfo = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Project> projectsById = new ConcurrentHashMap<>();

    /**
     * Refresh the list of projects from the given API response.
     *
     * @param resp The API response containing project data.
     */
    public static synchronized void refresh(ApiResponse resp) {
        projectList = new ArrayList<>();
        ConcurrentMap<String, Project> prevProjectsByCoreInfoPrev = projectsByCoreInfo;
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

    /**
     * Ensure that the project list is loaded. If not, it fetches the data from the API.
     */
    public static void ensureLoaded() {
        if (projectList == null) {
            refresh(QueryApiAccess.forcedGet(new QueryRef("get-projects")));
        }
    }

    /**
     * Get the list of all projects.
     */
    public static List<Project> getProjectList() {
        ensureLoaded();
        return projectList;
    }

    /**
     * Get a project by its ID.
     *
     * @param id The ID of the project.
     * @return The project with the given ID, or null if not found.
     */
    public static Project get(String id) {
        ensureLoaded();
        return projectsById.get(id);
    }

    /**
     * Mark all projects as needing data refresh.
     */
    public static void refresh() {
        ensureLoaded();
        for (Project project : projectList) {
            project.dataNeedsUpdate = true;
        }
    }

    private String id, label, rootNanopubId;
    private Nanopub rootNanopub = null;

    private String description = null;
    private List<IRI> owners = new ArrayList<>();
    private List<IRI> members = new ArrayList<>();
    private ConcurrentMap<String, IRI> ownerPubkeyMap = new ConcurrentHashMap<>();
    private List<Template> templates = new ArrayList<>();
    private Set<String> templateTags = new HashSet<>();
    private ConcurrentMap<String, List<Template>> templatesPerTag = new ConcurrentHashMap<>();
    private List<IRI> queryIds = new ArrayList<>();
    private IRI defaultProvenance = null;

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private Project(String id, String label, String rootNanopubId) {
        this.id = id;
        this.label = label;
        this.rootNanopubId = rootNanopubId;
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);

        for (Statement st : rootNanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(getId())) {
                if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    description = st.getObject().stringValue();
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_OWNER) && st.getObject() instanceof IRI obj) {
                    addOwner(obj);
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
                    templates.add(TemplateData.get().getTemplate(obj.stringValue()));
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
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

    private void addOwner(IRI owner) {
        // TODO This isn't efficient for long owner lists:
        if (owners.contains(owner)) return;
        owners.add(owner);
        UserData ud = User.getUserData();
        for (String pubkeyhash : ud.getPubkeyhashes(owner, true)) {
            ownerPubkeyMap.put(pubkeyhash, owner);
        }
    }

    /**
     * Get the ID of the project.
     *
     * @return The project ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the root nanopublication ID of the project.
     *
     * @return The root nanopublication ID.
     */
    public String getRootNanopubId() {
        return rootNanopubId;
    }

    /**
     * Get a string containing the core information of the project (ID and root nanopub ID).
     *
     * @return A string with the project ID and root nanopub ID.
     */
    public String getCoreInfoString() {
        return id + " " + rootNanopubId;
    }

    /**
     * Get the root nanopublication of the project.
     *
     * @return The root nanopublication.
     */
    public Nanopub getRootNanopub() {
        return rootNanopub;
    }

    /**
     * Get the label of the project.
     *
     * @return The project label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the description of the project.
     *
     * @return The project description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if the project data has been initialized.
     *
     * @return True if the data is initialized, false otherwise.
     */
    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    /**
     * Get the list of owners of the project.
     *
     * @return A list of IRIs representing the owners.
     */
    public List<IRI> getOwners() {
        triggerDataUpdate();
        return owners;
    }

    /**
     * Get the list of members of the project.
     *
     * @return A list of IRIs representing the members.
     */
    public List<IRI> getMembers() {
        triggerDataUpdate();
        return members;
    }

    /**
     * Get the list of templates associated with the project.
     *
     * @return A list of templates.
     */
    public List<Template> getTemplates() {
        return templates;
    }

    /**
     * Get the set of template tags associated with the project.
     *
     * @return A set of template tags.
     */
    public Set<String> getTemplateTags() {
        return templateTags;
    }

    /**
     * Get a map of templates categorized by their tags.
     *
     * @return A concurrent map where keys are tags and values are lists of templates.
     */
    public ConcurrentMap<String, List<Template>> getTemplatesPerTag() {
        return templatesPerTag;
    }

    /**
     * Get the list of query IDs associated with the project.
     *
     * @return A list of IRIs representing the query IDs.
     */
    public List<IRI> getQueryIds() {
        return queryIds;
    }

    /**
     * Get the default provenance IRI for the project.
     *
     * @return The default provenance IRI.
     */
    public IRI getDefaultProvenance() {
        return defaultProvenance;
    }

    private synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                for (ApiResponseEntry r : QueryApiAccess.forcedGet(new QueryRef("get-owners", "unit", id)).getData()) {
                    String pubkeyhash = r.get("pubkeyhash");
                    if (ownerPubkeyMap.containsKey(pubkeyhash)) {
                        addOwner(Utils.vf.createIRI(r.get("owner")));
                    }
                }
                members = new ArrayList<>();
                for (ApiResponseEntry r : QueryApiAccess.forcedGet(new QueryRef("get-members", "unit", id)).getData()) {
                    IRI memberId = Utils.vf.createIRI(r.get("member"));
                    // TODO These checks are inefficient for long member lists:
                    if (owners.contains(memberId)) continue;
                    if (members.contains(memberId)) continue;
                    members.add(memberId);
                }
                owners.sort(User.getUserData().userComparator);
                members.sort(User.getUserData().userComparator);
                dataInitialized = true;
            }).start();
            dataNeedsUpdate = false;
        }
    }

}
