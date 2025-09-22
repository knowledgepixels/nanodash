package com.knowledgepixels.nanodash;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.io.Serializable;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.shaded.com.google.common.collect.Ordering;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Class representing a "Space", which can be any kind of collaborative unit, like a project, group, or event.
 */
public class Space implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Space.class);

    /**
     * The predicate to assign the admins of the space.
     */
    public static final IRI HAS_ADMIN = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin");

    /**
     * The predicate for pinned templates in the space.
     */
    public static final IRI HAS_PINNED_TEMPLATE = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedTemplate");

    /**
     * The predicate for pinned queries in the space.
     */
    public static final IRI HAS_PINNED_QUERY = vf.createIRI("https://w3id.org/kpxl/gen/terms/hasPinnedQuery");

    private static List<Space> spaceList;
    private static Map<String, List<Space>> spaceListByType;
    private static Map<String,Space> spacesByCoreInfo = new HashMap<>();
    private static Map<String,Space> spacesById;
    private static Map<Space,Set<Space>> subspaceMap;
    private static Map<Space,Set<Space>> superspaceMap;
    private static boolean loaded = false;

    public static synchronized void refresh(ApiResponse resp) {
        spaceList = new ArrayList<>();
        spaceListByType = new HashMap<>();
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
            spaceListByType.computeIfAbsent(space.getType(), k -> new ArrayList<>()).add(space);
            spacesByCoreInfo.put(space.getCoreInfoString(), space);
            spacesById.put(space.getId(), space);
        }
        for (Space space : spaceList) {
            Space superSpace = space.getIdSuperspace();
            if (superSpace == null) continue;
            subspaceMap.computeIfAbsent(superSpace, k -> new HashSet<>()).add(space);
            superspaceMap.computeIfAbsent(space, k -> new HashSet<>()).add(superSpace);
        }
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void ensureLoaded() {
        if (spaceList == null) {
            refresh(QueryApiAccess.forcedGet(new QueryRef("get-spaces")));
        }
    }

    public static List<Space> getSpaceList() {
        ensureLoaded();
        return spaceList;
    }

    public static List<Space> getSpaceList(String type) {
        ensureLoaded();
        return spaceListByType.computeIfAbsent(type, k -> new ArrayList<>());
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
    private SpaceData data = new SpaceData();

    private static class SpaceData implements Serializable {

        List<String> altIds = new ArrayList<>();
    
        String description = null;
        Calendar startDate, endDate;
        IRI defaultProvenance = null;

        List<IRI> admins = new ArrayList<>();
        Map<IRI,Set<SpaceMemberRole>> members = new HashMap<>();
        List<SpaceMemberRole> roles = new ArrayList<>();
        Map<IRI,SpaceMemberRole> roleMap = new HashMap<>();

        Map<String,IRI> adminPubkeyMap = new HashMap<>();
        List<Serializable> pinnedResources = new ArrayList<>();
        Set<String> pinGroupTags = new HashSet<>();
        Map<String, List<Serializable>> pinnedResourceMap = new HashMap<>();

        void addAdmin(IRI admin) {
            // TODO This isn't efficient for long owner lists:
            if (admins.contains(admin)) return;
            admins.add(admin);
            UserData ud = User.getUserData();
            for (String pubkeyhash : ud.getPubkeyhashes(admin, true)) {
                adminPubkeyMap.put(pubkeyhash, admin);
            }
        }

    }

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private Space(ApiResponseEntry resp) {
        this.id = resp.get("space");
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);
        setCoreData(data);
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

    public Calendar getStartDate() {
        return data.startDate;
    }

    public Calendar getEndDate() {
        return data.endDate;
    }

    public String getTypeLabel() {
        return type.replaceFirst("^.*/", "");
    }

    public String getDescription() {
        return data.description;
    }

    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
    }

    public List<IRI> getAdmins() {
        triggerDataUpdate();
        return data.admins;
    }

    public List<IRI> getMembers() {
        triggerDataUpdate();
        List<IRI> members = new ArrayList<IRI>(data.members.keySet());
        members.sort(User.getUserData().userComparator);
        return members;
    }

    public Set<SpaceMemberRole> getMemberRoles(IRI memberId) {
        return data.members.get(memberId);
    }

    public boolean isMember(IRI userId) {
        triggerDataUpdate();
        return data.members.containsKey(userId);
    }

    public List<Serializable> getPinnedResources() {
        triggerDataUpdate();
        return data.pinnedResources;
    }

    public Set<String> getPinGroupTags() {
        triggerDataUpdate();
        return data.pinGroupTags;
    }

    public Map<String, List<Serializable>> getPinnedResourceMap() {
        triggerDataUpdate();
        return data.pinnedResourceMap;
    }

    public IRI getDefaultProvenance() {
        return data.defaultProvenance;
    }

    public List<SpaceMemberRole> getRoles() {
        return data.roles;
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

    public List<Space> getSubspaces(String type) {
        List<Space> l = new ArrayList<>();
        for (Space s : getSubspaces()) {
            if (s.getType().equals(type)) l.add(s);
        }
        return l;
    }

    public List<String> getAltIDs() {
        return data.altIds;
    }

    private synchronized void triggerDataUpdate() {
        if (dataNeedsUpdate) {
            new Thread(() -> {
                try {
                    SpaceData newData = new SpaceData();
                    setCoreData(newData);
    
                    newData.roles.add(SpaceMemberRole.ADMIN_ROLE);
                    newData.roleMap.put(SpaceMemberRole.ADMIN_ROLE_IRI, SpaceMemberRole.ADMIN_ROLE);

                    Multimap<String, String> spaceIds = ArrayListMultimap.create();
                    spaceIds.put("space", id);
                    for (String id : newData.altIds) {
                        spaceIds.put("space", id);
                    }
    
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-admins", spaceIds)).getData()) {
                        String pubkeyhash = r.get("pubkey");
                        if (newData.adminPubkeyMap.containsKey(pubkeyhash)) {
                            IRI adminId = Utils.vf.createIRI(r.get("admin"));
                            newData.addAdmin(adminId);
                            newData.members.computeIfAbsent(adminId, (k) -> new HashSet<>()).add(SpaceMemberRole.ADMIN_ROLE);
                        }
                    }
                    newData.admins.sort(User.getUserData().userComparator);
    
                    Multimap<String, String> getSpaceMemberParams = ArrayListMultimap.create(spaceIds);
    
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef( "get-space-member-roles", spaceIds)).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        SpaceMemberRole role = new SpaceMemberRole(r);
                        newData.roles.add(role);
    
                        // TODO Handle cases of overlapping properties:
                        newData.roleMap.put(role.getMainProperty(), role);
                        for (IRI p : role.getEquivalentProperties()) newData.roleMap.put(p, role);
                        for (IRI p : role.getInverseProperties()) newData.roleMap.put(p, role);
        
                        role.addRoleParams(getSpaceMemberParams);
                    }
    
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-space-members", getSpaceMemberParams)).getData()) {
                        IRI memberId = Utils.vf.createIRI(r.get("member"));
                        SpaceMemberRole role = newData.roleMap.get(Utils.vf.createIRI(r.get("role")));
                        newData.members.computeIfAbsent(memberId, (k) -> new HashSet<>()).add(role);
                    }
    
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-pinned-templates", spaceIds)).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        Template t = TemplateData.get().getTemplate(r.get("template"));
                        if (t == null) continue;
                        newData.pinnedResources.add(t);
                        String tag = r.get("tag");
                        if (tag != null && !tag.isEmpty()) {
                            newData.pinGroupTags.add(r.get("tag"));
                            newData.pinnedResourceMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(TemplateData.get().getTemplate(r.get("template")));
                        }
                    }
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-pinned-queries", spaceIds)).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        GrlcQuery query = GrlcQuery.get(r.get("query"));
                        if (query == null) continue;
                        newData.pinnedResources.add(query);
                        String tag = r.get("tag");
                        if (tag != null && !tag.isEmpty()) {
                            newData.pinGroupTags.add(r.get("tag"));
                            newData.pinnedResourceMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(query);
                        }
                    }
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update space data: {}", ex);
                }
            }).start();
            dataNeedsUpdate = false;
        }
    }

    private void setCoreData(SpaceData data) {
        for (Statement st : rootNanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(getId())) {
                if (st.getPredicate().equals(OWL.SAMEAS) && st.getObject() instanceof IRI objIri) {
                    data.altIds.add(objIri.stringValue());
                } else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    data.description = st.getObject().stringValue();
                } else if (st.getPredicate().stringValue().equals("http://schema.org/startDate")) {
                    try {
                        data.startDate = DatatypeConverter.parseDateTime(st.getObject().stringValue());
                    } catch (DateTimeParseException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().stringValue().equals("http://schema.org/endDate")) {
                    try {
                        data.endDate = DatatypeConverter.parseDateTime(st.getObject().stringValue());
                    } catch (IllegalArgumentException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().equals(HAS_ADMIN) && st.getObject() instanceof IRI obj) {
                    data.addAdmin(obj);
                } else if (st.getPredicate().equals(HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
                    data.pinnedResources.add(TemplateData.get().getTemplate(obj.stringValue()));
                } else if (st.getPredicate().equals(HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
                    data.pinnedResources.add(GrlcQuery.get(obj.stringValue()));
                } else if (st.getPredicate().equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE) && st.getObject() instanceof IRI obj) {
                    data.defaultProvenance = obj;
                }
            } else if (st.getPredicate().equals(NTEMPLATE.HAS_TAG) && st.getObject() instanceof Literal l) {
                data.pinGroupTags.add(l.stringValue());
                List<Serializable> list = data.pinnedResourceMap.get(l.stringValue());
                if (list == null) {
                    list = new ArrayList<>();
                    data.pinnedResourceMap.put(l.stringValue(), list);
                }
                list.add(TemplateData.get().getTemplate(st.getSubject().stringValue()));
            }
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
