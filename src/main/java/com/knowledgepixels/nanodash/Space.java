package com.knowledgepixels.nanodash;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import com.github.jsonldjava.shaded.com.google.common.collect.Ordering;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

/**
 * Class representing a "Space", which can be any kind of collaborative unit, like a project, group, or event.
 */
public class Space implements Serializable {

    /**
     * The predicate for the owner of the space.
     */
    public static final IRI HAS_OWNER = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasOwner");

    /**
     * The predicate for pinned templates in the space.
     */
    public static final IRI HAS_PINNED_TEMPLATE = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedTemplate");

    /**
     * The predicate for pinned queries in the space.
     */
    public static final IRI HAS_PINNED_QUERY = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedQuery");

    private static List<Space> spaceList;
    private static Map<String,Space> spacesByCoreInfo = new HashMap<>();
    private static Map<String,Space> spacesById;
    private static Map<Space,Set<Space>> subspaceMap;
    private static Map<Space,Set<Space>> superspaceMap;

    public static synchronized void refresh(ApiResponse resp) {
        spaceList = new ArrayList<>();
        Map<String,Space> prevSpacesByCoreInfoPrev = spacesByCoreInfo;
        spacesByCoreInfo = new HashMap<>();
        spacesById = new HashMap<>();
        subspaceMap = new HashMap<>();
        superspaceMap = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            Space space = new Space(entry);
            Space prevSpace = prevSpacesByCoreInfoPrev.get(space.getCoreInfoString());
            if (prevSpace != null) space = prevSpace;
            spaceList.add(space);
            spacesByCoreInfo.put(space.getCoreInfoString(), space);
            spacesById.put(space.getId(), space);
        }
        for (Space space : spaceList) {
            Space superSpace = space.getIdSuperspace();
            if (superSpace == null) continue;
            subspaceMap.computeIfAbsent(superSpace, k -> new HashSet<>()).add(space);
            superspaceMap.computeIfAbsent(space, k -> new HashSet<>()).add(superSpace);
        }
    }

    public static void ensureLoaded() {
        if (spaceList == null) {
            refresh(QueryApiAccess.forcedGet("get-spaces"));
        }
    }

    public static List<Space> getSpaceList() {
        ensureLoaded();
        return spaceList;
    }

    public static Space get(String id) {
        ensureLoaded();
        return spacesById.get(id);
    }

    public static void refresh() {
        ensureLoaded();
        for (Space space : spaceList) {
            space.dataNeedsUpdate = true;
        }
    }

    private String id, label, rootNanopubId, type;
    private Nanopub rootNanopub = null;

    private String description = null;
    private List<IRI> owners = new ArrayList<>();
    private List<IRI> members = new ArrayList<>();
    private ConcurrentMap<String,IRI> ownerPubkeyMap = new ConcurrentHashMap<>();
    private List<Template> templates = new ArrayList<>();
    private Set<String> templateTags = new HashSet<>();
    private ConcurrentMap<String, List<Template>> templatesPerTag = new ConcurrentHashMap<>();
    private List<IRI> queryIds = new ArrayList<>();
    private IRI defaultProvenance = null;

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private Space(ApiResponseEntry resp) {
        this.id = resp.get("space");
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);

        for (Statement st : rootNanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(getId())) {
                if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    description = st.getObject().stringValue();
                } else if (st.getPredicate().equals(HAS_OWNER) && st.getObject() instanceof IRI obj) {
                    addOwner(obj);
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

    private void addOwner(IRI owner) {
        // TODO This isn't efficient for long owner lists:
        if (owners.contains(owner)) return;
        owners.add(owner);
        UserData ud = User.getUserData();
        for (String pubkeyhash : ud.getPubkeyhashes(owner, true)) {
            ownerPubkeyMap.put(pubkeyhash, owner);
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

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        return type.replaceFirst("^.*/", "");
    }

    public String getDescription() {
        return description;
    }

    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    public List<IRI> getOwners() {
        triggerDataUpdate();
        return owners;
    }

    public List<IRI> getMembers() {
        triggerDataUpdate();
        return members;
    }

    public List<Template> getTemplates() {
        triggerDataUpdate();
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

    public String getSuperId() {
        return null;
    }

    public Space getIdSuperspace() {
        if (!id.matches("https?://[^/]+/.*/[^/]*/?")) return null;
        String superId = id.replaceFirst("(https?://[^/]+/.*)/[^/]*/?", "$1");
        if (spacesById.containsKey(superId)) {
            return spacesById.get(superId);
        }
        return null;
    }

    public List<Space> getSuperspaces() {
        if (superspaceMap.containsKey(this)) {
            List<Space> superspaces = new ArrayList<>(superspaceMap.get(this));
            Collections.sort(superspaces, Ordering.usingToString());
            return superspaces;
        }
        return new ArrayList<>();
    }

    public List<Space> getSubspaces() {
        if (subspaceMap.containsKey(this)) {
            List<Space> subspaces = new ArrayList<>(subspaceMap.get(this));
            Collections.sort(subspaces, Ordering.usingToString());
            return subspaces;
        }
        return new ArrayList<>();
    }

    private synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                for (ApiResponseEntry r : QueryApiAccess.forcedGet("get-owners", "unit", id).getData()) {
                    String pubkeyhash = r.get("pubkeyhash");
                    if (ownerPubkeyMap.containsKey(pubkeyhash)) {
                        addOwner(Utils.vf.createIRI(r.get("owner")));
                    }
                }
                members = new ArrayList<>();
                for (ApiResponseEntry r : QueryApiAccess.forcedGet("get-members", "unit", id).getData()) {
                    IRI memberId = Utils.vf.createIRI(r.get("member"));
                    // TODO These checks are inefficient for long member lists:
                    if (owners.contains(memberId)) continue;
                    if (members.contains(memberId)) continue;
                    members.add(memberId);
                }
                owners.sort(User.getUserData().userComparator);
                members.sort(User.getUserData().userComparator);

                for (ApiResponseEntry r : QueryApiAccess.forcedGet("get-pinned-templates", "space", id).getData()) {
                    Template t = TemplateData.get().getTemplate(r.get("template"));
                    if (t == null) continue;
                    // Check pubkey with space owners
                    templates.add(t);
                    String tag = r.get("tag");
                    if (tag != null && !tag.isEmpty()) {
                        templateTags.add(r.get("tag"));
                        List<Template> list = templatesPerTag.get(r.get("tag"));
                        if (list == null) {
                            list = new ArrayList<>();
                            templatesPerTag.put(r.get("tag"), list);
                        }
                        list.add(TemplateData.get().getTemplate(r.get("template")));
                    }
                }
                dataInitialized = true;
            }).start();
            dataNeedsUpdate = false;
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
