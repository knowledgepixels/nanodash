package com.knowledgepixels.nanodash;

import java.io.Serializable;
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
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;

import jakarta.xml.bind.DatatypeConverter;

/**
 * Class representing a "Space", which can be any kind of collaborative unit, like a project, group, or event.
 */
public class Space extends ResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(Space.class);

    private static List<Space> spaceList;
    private static Map<String, List<Space>> spaceListByType;
    private static Map<String, Space> spacesByCoreInfo = new HashMap<>();
    private static Map<String, Space> spacesById;
    private static Map<Space, Set<Space>> subspaceMap;
    private static Map<Space, Set<Space>> superspaceMap;
    private static boolean loaded = false;
    private static Long runRootUpdateAfter = null;

    /**
     * Refresh the list of spaces from the API response.
     *
     * @param resp The API response containing space data.
     */
    public static synchronized void refresh(ApiResponse resp) {
        spaceList = new ArrayList<>();
        spaceListByType = new HashMap<>();
        Map<String, Space> prevSpacesByCoreInfoPrev = spacesByCoreInfo;
        spacesByCoreInfo = new HashMap<>();
        spacesById = new HashMap<>();
        subspaceMap = new HashMap<>();
        superspaceMap = new HashMap<>();
        for (ApiResponseEntry entry : resp.getData()) {
            String id = getCoreInfoString(entry);
            Space space;
            if (prevSpacesByCoreInfoPrev.containsKey(id)) {
                space = prevSpacesByCoreInfoPrev.get(id);
            } else {
                space = new Space(entry);
            }
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
            space.setDataNeedsUpdate();
        }
        loaded = true;
    }

    /**
     * Check if the spaces have been loaded.
     *
     * @return true if loaded, false otherwise.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean areAllSpacesInitialized() {
        for (Space space : spaceList) {
            if (!space.isDataInitialized()) return false;
        }
        return true;
    }

    @Override
    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized && super.isDataInitialized();
    }

    public static void triggerAllDataUpdates() {
        for (Space space : spaceList) {
            space.triggerDataUpdate();
        }
    }

    private static final String ensureLoadedLock = "";

    /**
     * Ensure that the spaces are loaded, fetching them from the API if necessary.
     */
    public static void ensureLoaded() {
        if (spaceList == null) {
            try {
                synchronized (ensureLoadedLock) {
                    if (runRootUpdateAfter != null) {
                        while (System.currentTimeMillis() < runRootUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runRootUpdateAfter = null;
                    }
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
            refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), true));
        }
    }

    public static void forceRootRefresh(long waitMillis) {
        spaceList = null;
        runRootUpdateAfter = System.currentTimeMillis() + waitMillis;
    }

    /**
     * Get the list of all spaces.
     *
     * @return List of spaces.
     */
    public static List<Space> getSpaceList() {
        ensureLoaded();
        return spaceList;
    }

    /**
     * Get the list of spaces of a specific type.
     *
     * @param type The type of spaces to retrieve.
     *             System.err.println("REFRESH...");
     * @return List of spaces of the specified type.
     */
    public static List<Space> getSpaceList(String type) {
        ensureLoaded();
        return spaceListByType.computeIfAbsent(type, k -> new ArrayList<>());
    }

    /**
     * Get a space by its id.
     *
     * @param id The id of the space.
     * @return The corresponding Space object, or null if not found.
     */
    public static Space get(String id) {
        ensureLoaded();
        return spacesById.get(id);
    }

    /**
     * Mark all spaces as needing a data update.
     */
    public static void refresh() {
        refresh(ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACES), true));
        for (Space space : spaceList) {
            space.dataNeedsUpdate = true;
        }
    }

    public void forceRefresh(long waitMillis) {
        super.forceRefresh(waitMillis);
        dataNeedsUpdate = true;
        dataInitialized = false;
    }

    private String label, rootNanopubId, type;
    private Nanopub rootNanopub = null;
    private SpaceData data = new SpaceData();

    private static class SpaceData implements Serializable {

        List<String> altIds = new ArrayList<>();

        String description = null;
        Calendar startDate, endDate;
        IRI defaultProvenance = null;

        List<IRI> admins = new ArrayList<>();
        Map<IRI, Set<SpaceMemberRoleRef>> users = new HashMap<>();
        List<SpaceMemberRoleRef> roles = new ArrayList<>();
        Map<IRI, SpaceMemberRole> roleMap = new HashMap<>();

        Map<String, IRI> adminPubkeyMap = new HashMap<>();
        Set<Serializable> pinnedResources = new HashSet<>();
        Set<String> pinGroupTags = new HashSet<>();
        Map<String, Set<Serializable>> pinnedResourceMap = new HashMap<>();

        void addAdmin(IRI admin, String npId) {
            // TODO This isn't efficient for long owner lists:
            if (admins.contains(admin)) return;
            admins.add(admin);
            UserData ud = User.getUserData();
            for (String pubkeyhash : ud.getPubkeyhashes(admin, true)) {
                adminPubkeyMap.put(pubkeyhash, admin);
            }
            users.computeIfAbsent(admin, (k) -> new HashSet<>()).add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, npId));
        }

    }

    private static String getCoreInfoString(ApiResponseEntry resp) {
        String id = resp.get("space");
        String rootNanopubId = resp.get("np");
        return id + " " + rootNanopubId;
    }

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;

    private Space(ApiResponseEntry resp) {
        super(resp.get("space"));
        initSpace(this);
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);
        setCoreData(data);
    }

    /**
     * Get the root nanopublication ID of the space.
     *
     * @return The root nanopub ID.
     */
    @Override
    public String getNanopubId() {
        return rootNanopubId;
    }

    /**
     * Get a string combining the space ID and root nanopub ID for core identification.
     *
     * @return The core info string.
     */
    public String getCoreInfoString() {
        return getId() + " " + rootNanopubId;
    }

    /**
     * Get the root nanopublication of the space.
     *
     * @return The root Nanopub object.
     */
    @Override
    public Nanopub getNanopub() {
        return rootNanopub;
    }

    /**
     * Get the label of the space.
     *
     * @return The space label.
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Get the type of the space.
     *
     * @return The space type.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the start date of the space.
     *
     * @return The start date as a Calendar object, or null if not set.
     */
    public Calendar getStartDate() {
        return data.startDate;
    }

    /**
     * Get the end date of the space.
     *
     * @return The end date as a Calendar object, or null if not set.
     */
    public Calendar getEndDate() {
        return data.endDate;
    }

    /**
     * Get a simplified label for the type of space by removing any namespace prefix.
     *
     * @return The simplified type label.
     */
    public String getTypeLabel() {
        return type.replaceFirst("^.*/", "");
    }

    /**
     * Get the description of the space.
     *
     * @return The description string.
     */
    public String getDescription() {
        return data.description;
    }


    /**
     * Get the list of admins in this space.
     *
     * @return List of admin IRIs.
     */
    public List<IRI> getAdmins() {
        ensureInitialized();
        return data.admins;
    }

    /**
     * Get the list of members in this space.
     *
     * @return List of member IRIs.
     */
    public List<IRI> getUsers() {
        ensureInitialized();
        List<IRI> users = new ArrayList<IRI>(data.users.keySet());
        users.sort(User.getUserData().userComparator);
        return users;
    }

    /**
     * Get the roles of a specific member in this space.
     *
     * @param userId The IRI of the member.
     * @return Set of roles assigned to the member, or null if the member is not part of this space.
     */
    public Set<SpaceMemberRoleRef> getMemberRoles(IRI userId) {
        ensureInitialized();
        return data.users.get(userId);
    }

    /**
     * Check if a user is a member of this space.
     *
     * @param userId The IRI of the user to check.
     * @return true if the user is a member, false otherwise.
     */
    public boolean isMember(IRI userId) {
        ensureInitialized();
        return data.users.containsKey(userId);
    }

    public boolean isAdminPubkey(String pubkey) {
        ensureInitialized();
        return data.adminPubkeyMap.containsKey(pubkey);
    }

    /**
     * Get the list of pinned resources in this space.
     *
     * @return List of pinned resources.
     */
    public Set<Serializable> getPinnedResources() {
        ensureInitialized();
        return data.pinnedResources;
    }

    /**
     * Get the set of tags used for grouping pinned resources.
     *
     * @return Set of tags.
     */
    public Set<String> getPinGroupTags() {
        ensureInitialized();
        return data.pinGroupTags;
    }

    /**
     * Get a map of pinned resources grouped by their tags.
     *
     * @return Map where keys are tags and values are lists of pinned resources (Templates or GrlcQueries).
     */
    public Map<String, Set<Serializable>> getPinnedResourceMap() {
        ensureInitialized();
        return data.pinnedResourceMap;
    }

    public boolean appliesTo(String elementId, Set<IRI> classes) {
        triggerDataUpdate();
        for (ViewDisplay v : getViewDisplays()) {
            if (v.appliesTo(elementId, classes)) return true;
        }
        return false;
    }

    /**
     * Get the default provenance IRI for this space.
     *
     * @return The default provenance IRI, or null if not set.
     */
    public IRI getDefaultProvenance() {
        return data.defaultProvenance;
    }

    /**
     * Get the roles defined in this space.
     *
     * @return List of roles.
     */
    public List<SpaceMemberRoleRef> getRoles() {
        return data.roles;
    }

    /**
     * Get the super ID of the space.
     *
     * @return Always returns null. Use getIdSuperspace() instead.
     */
    public String getSuperId() {
        return null;
    }

    /**
     * Get the superspace ID.
     *
     * @return The superspace, or null if not applicable.
     */
    public Space getIdSuperspace() {
        if (!getId().matches("https?://[^/]+/.*/[^/]*/?")) return null;
        String superId = getId().replaceFirst("(https?://[^/]+/.*)/[^/]*/?", "$1");
        if (spacesById.containsKey(superId)) {
            return spacesById.get(superId);
        }
        return null;
    }

    /**
     * Get superspaces of this space.
     *
     * @return List of superspaces.
     */
    public List<Space> getSuperspaces() {
        if (superspaceMap.containsKey(this)) {
            List<Space> superspaces = new ArrayList<>(superspaceMap.get(this));
            Collections.sort(superspaces, Ordering.usingToString());
            return superspaces;
        }
        return new ArrayList<>();
    }

    /**
     * Get subspaces of this space.
     *
     * @return List of subspaces.
     */
    public List<Space> getSubspaces() {
        if (subspaceMap.containsKey(this)) {
            List<Space> subspaces = new ArrayList<>(subspaceMap.get(this));
            Collections.sort(subspaces, Ordering.usingToString());
            return subspaces;
        }
        return new ArrayList<>();
    }

    /**
     * Get subspaces of a specific type.
     *
     * @param type The type of subspaces to retrieve.
     * @return List of subspaces of the specified type.
     */
    public List<Space> getSubspaces(String type) {
        List<Space> l = new ArrayList<>();
        for (Space s : getSubspaces()) {
            if (s.getType().equals(type)) l.add(s);
        }
        return l;
    }

    /**
     * Get alternative IDs for the space.
     *
     * @return List of alternative IDs.
     */
    public List<String> getAltIDs() {
        return data.altIds;
    }

    private synchronized void ensureInitialized() {
        Thread thread = triggerSpaceDataUpdate();
        if (!dataInitialized && thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                logger.error("failed to join thread", ex);
            }
        }
        thread = super.triggerDataUpdate();
        if (!dataInitialized && thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                logger.error("failed to join thread", ex);
            }
        }
    }

    @Override
    public synchronized Thread triggerDataUpdate() {
        triggerSpaceDataUpdate();
        return super.triggerDataUpdate();
    }

    private synchronized Thread triggerSpaceDataUpdate() {
        if (dataNeedsUpdate) {
            Thread thread = new Thread(() -> {
                try {
                    if (getRunUpdateAfter() != null) {
                        while (System.currentTimeMillis() < getRunUpdateAfter()) {
                            Thread.sleep(100);
                        }
                    }
                    SpaceData newData = new SpaceData();
                    setCoreData(newData);

                    newData.roles.add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, null));
                    newData.roleMap.put(KPXL_TERMS.HAS_ADMIN_PREDICATE, SpaceMemberRole.ADMIN_ROLE);

                    // TODO Improve this:
                    Multimap<String, String> spaceIds = ArrayListMultimap.create();
                    Multimap<String, String> resourceIds = ArrayListMultimap.create();
                    spaceIds.put("space", getId());
                    resourceIds.put("resource", getId());
                    for (String id : newData.altIds) {
                        spaceIds.put("space", id);
                        resourceIds.put("resource", id);
                    }

                    ApiResponse getAdminsResponse = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_ADMINS, spaceIds), false);
                    boolean continueAddingAdmins = true;
                    while (continueAddingAdmins) {
                        continueAddingAdmins = false;
                        for (ApiResponseEntry r : getAdminsResponse.getData()) {
                            String pubkeyhash = r.get("pubkey");
                            if (newData.adminPubkeyMap.containsKey(pubkeyhash)) {
                                IRI adminId = Utils.vf.createIRI(r.get("admin"));
                                if (!newData.admins.contains(adminId)) {
                                    continueAddingAdmins = true;
                                    newData.addAdmin(adminId, r.get("np"));
                                }
                            }
                        }
                    }
                    newData.admins.sort(User.getUserData().userComparator);

                    Multimap<String, String> getSpaceMemberParams = ArrayListMultimap.create(spaceIds);

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACE_MEMBER_ROLES, spaceIds), false).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        SpaceMemberRole role = new SpaceMemberRole(r);
                        newData.roles.add(new SpaceMemberRoleRef(role, r.get("np")));

                        // TODO Handle cases of overlapping properties:
                        for (IRI p : role.getRegularProperties()) newData.roleMap.put(p, role);
                        for (IRI p : role.getInverseProperties()) newData.roleMap.put(p, role);

                        role.addRoleParams(getSpaceMemberParams);
                    }

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACE_MEMBERS, getSpaceMemberParams), false).getData()) {
                        IRI memberId = Utils.vf.createIRI(r.get("member"));
                        SpaceMemberRole role = newData.roleMap.get(Utils.vf.createIRI(r.get("role")));
                        newData.users.computeIfAbsent(memberId, (k) -> new HashSet<>()).add(new SpaceMemberRoleRef(role, r.get("np")));
                    }

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_PINNED_TEMPLATES, spaceIds), false).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        Template t = TemplateData.get().getTemplate(r.get("template"));
                        if (t == null) continue;
                        newData.pinnedResources.add(t);
                        String tag = r.get("tag");
                        if (tag != null && !tag.isEmpty()) {
                            newData.pinGroupTags.add(r.get("tag"));
                            newData.pinnedResourceMap.computeIfAbsent(tag, k -> new HashSet<>()).add(TemplateData.get().getTemplate(r.get("template")));
                        }
                    }
                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_PINNED_QUERIES, spaceIds), false).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        GrlcQuery query = GrlcQuery.get(r.get("query"));
                        if (query == null) continue;
                        newData.pinnedResources.add(query);
                        String tag = r.get("tag");
                        if (tag != null && !tag.isEmpty()) {
                            newData.pinGroupTags.add(r.get("tag"));
                            newData.pinnedResourceMap.computeIfAbsent(tag, k -> new HashSet<>()).add(query);
                        }
                    }
                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update space data: {}", ex);
                }
            });
            thread.start();
            dataNeedsUpdate = false;
            return thread;
        }
        return null;
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
                    } catch (IllegalArgumentException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().stringValue().equals("http://schema.org/endDate")) {
                    try {
                        data.endDate = DatatypeConverter.parseDateTime(st.getObject().stringValue());
                    } catch (IllegalArgumentException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ADMIN) && st.getObject() instanceof IRI obj) {
                    data.addAdmin(obj, rootNanopub.getUri().stringValue());
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
                    data.pinnedResources.add(TemplateData.get().getTemplate(obj.stringValue()));
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
                    data.pinnedResources.add(GrlcQuery.get(obj.stringValue()));
                } else if (st.getPredicate().equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE) && st.getObject() instanceof IRI obj) {
                    data.defaultProvenance = obj;
                }
            } else if (st.getPredicate().equals(NTEMPLATE.HAS_TAG) && st.getObject() instanceof Literal l) {
                data.pinGroupTags.add(l.stringValue());
                Set<Serializable> list = data.pinnedResourceMap.get(l.stringValue());
                if (list == null) {
                    list = new HashSet<>();
                    data.pinnedResourceMap.put(l.stringValue(), list);
                }
                list.add(TemplateData.get().getTemplate(st.getSubject().stringValue()));
            }
        }
    }

}
