package com.knowledgepixels.nanodash;

import static com.knowledgepixels.nanodash.Utils.vf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
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

    public static void triggerAllDataUpdates() {
        for (Space space : spaceList) {
            space.triggerDataUpdate();
        }
    }

    /**
     * Ensure that the spaces are loaded, fetching them from the API if necessary.
     */
    public static void ensureLoaded() {
        if (spaceList == null) {
            try {
                if (runRootUpdateAfter != null) {
                    while (System.currentTimeMillis() < runRootUpdateAfter) {
                        Thread.sleep(100);
                    }
                    runRootUpdateAfter = null;
                }
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
            refresh(QueryApiAccess.forcedGet(new QueryRef("get-spaces")));
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
        System.err.println("REFRESH...");
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
        refresh(QueryApiAccess.forcedGet(new QueryRef("get-spaces")));
        for (Space space : spaceList) {
            space.dataNeedsUpdate = true;
        }
    }

    public void forceRefresh(long waitMillis) {
        dataNeedsUpdate = true;
        dataInitialized = false;
        runUpdateAfter = System.currentTimeMillis() + waitMillis;
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
        // TODO Make Pair<SpaceMemberRole, String> a new class with SpaceMemberRole + nanopub URI
        Map<IRI, Set<Pair<SpaceMemberRole, String>>> users = new HashMap<>();
        List<Pair<SpaceMemberRole,String>> roles = new ArrayList<>();
        Map<IRI, SpaceMemberRole> roleMap = new HashMap<>();

        Map<String, IRI> adminPubkeyMap = new HashMap<>();
        Set<Serializable> pinnedResources = new HashSet<>();
        List<ViewDisplay> topLevelViews = new ArrayList<>();
        List<ViewDisplay> partLevelViews = new ArrayList<>();
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
            users.computeIfAbsent(admin, (k) -> new HashSet<>()).add(Pair.of(SpaceMemberRole.ADMIN_ROLE, npId));
        }

    }

    private boolean dataInitialized = false;
    private boolean dataNeedsUpdate = true;
    private Long runUpdateAfter = null;

    private Space(ApiResponseEntry resp) {
        this.id = resp.get("space");
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);
        setCoreData(data);
    }

    /**
     * Get the ID of the space.
     *
     * @return The space ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the root nanopublication ID of the space.
     *
     * @return The root nanopub ID.
     */
    public String getRootNanopubId() {
        return rootNanopubId;
    }

    /**
     * Get a string combining the space ID and root nanopub ID for core identification.
     *
     * @return The core info string.
     */
    public String getCoreInfoString() {
        return id + " " + rootNanopubId;
    }

    /**
     * Get the root nanopublication of the space.
     *
     * @return The root Nanopub object.
     */
    public Nanopub getRootNanopub() {
        return rootNanopub;
    }

    /**
     * Get the label of the space.
     *
     * @return The space label.
     */
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
     * Check if the space data has been initialized.
     *
     * @return true if initialized, false otherwise.
     */
    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized;
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
    public Set<Pair<SpaceMemberRole,String>> getMemberRoles(IRI userId) {
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

    /**
     * Returns the view displays and their associated nanopub IDs.
     *
     * @return Map of views to nanopub IDs
     */
    public List<ViewDisplay> getTopLevelViews() {
        return data.topLevelViews;
    }

    public List<ViewDisplay> getPartLevelViews(Set<IRI> classes) {
        triggerDataUpdate();
        List<ViewDisplay> viewDisplays = new ArrayList<>();
        for (ViewDisplay v : data.partLevelViews) {
            if (v.getView().hasTargetClasses()) {
                for (IRI c : classes) {
                    if (v.getView().hasTargetClass(c)) {
                        viewDisplays.add(v);
                        break;
                    }
                }
            } else {
                viewDisplays.add(v);
            }
        }
        return viewDisplays;
    }

    public boolean coversElement(String elementId) {
        triggerDataUpdate();
        for (ViewDisplay v : data.topLevelViews) {
            if (v.getView().coversElement(elementId)) return true;
        }
        for (ViewDisplay v : data.partLevelViews) {
            if (v.getView().coversElement(elementId)) return true;
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
    public List<Pair<SpaceMemberRole,String>> getRoles() {
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
        if (!id.matches("https?://[^/]+/.*/[^/]*/?")) return null;
        String superId = id.replaceFirst("(https?://[^/]+/.*)/[^/]*/?", "$1");
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
        Thread thread = triggerDataUpdate();
        if (!dataInitialized && thread != null) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                logger.error("failed to join thread", ex);
            }
        }
    }

    private synchronized Thread triggerDataUpdate() {
        if (dataNeedsUpdate) {
            Thread thread = new Thread(() -> {
                try {
                    if (runUpdateAfter != null) {
                        while (System.currentTimeMillis() < runUpdateAfter) {
                            Thread.sleep(100);
                        }
                        runUpdateAfter = null;
                    }
                    SpaceData newData = new SpaceData();
                    setCoreData(newData);

                    newData.roles.add(Pair.of(SpaceMemberRole.ADMIN_ROLE, null));
                    newData.roleMap.put(SpaceMemberRole.HAS_ADMIN_PREDICATE, SpaceMemberRole.ADMIN_ROLE);

                    // TODO Improve this:
                    Multimap<String, String> spaceIds = ArrayListMultimap.create();
                    Multimap<String, String> resourceIds = ArrayListMultimap.create();
                    spaceIds.put("space", id);
                    resourceIds.put("resource", id);
                    for (String id : newData.altIds) {
                        spaceIds.put("space", id);
                        resourceIds.put("resource", id);
                    }

                    ApiResponse getAdminsResponse = QueryApiAccess.get(new QueryRef("get-admins", spaceIds));
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

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-space-member-roles", spaceIds)).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        SpaceMemberRole role = new SpaceMemberRole(r);
                        newData.roles.add(Pair.of(role, r.get("np")));

                        // TODO Handle cases of overlapping properties:
                        for (IRI p : role.getRegularProperties()) newData.roleMap.put(p, role);
                        for (IRI p : role.getInverseProperties()) newData.roleMap.put(p, role);

                        role.addRoleParams(getSpaceMemberParams);
                    }

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-space-members", getSpaceMemberParams)).getData()) {
                        IRI memberId = Utils.vf.createIRI(r.get("member"));
                        SpaceMemberRole role = newData.roleMap.get(Utils.vf.createIRI(r.get("role")));
                        newData.users.computeIfAbsent(memberId, (k) -> new HashSet<>()).add(Pair.of(role, r.get("np")));
                    }

                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-pinned-templates", spaceIds)).getData()) {
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
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-pinned-queries", spaceIds)).getData()) {
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
                    for (ApiResponseEntry r : QueryApiAccess.get(new QueryRef("get-view-displays", resourceIds)).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        try {
                            ViewDisplay vd = ViewDisplay.get(r.get("display"));
                            if (ResourceView.PART_LEVEL_VIEW_DISPLAY.stringValue().equals(r.get("displayType"))) {
                                newData.partLevelViews.add(vd);
                            } else {
                                newData.topLevelViews.add(vd);
                            }
                        } catch (IllegalArgumentException ex) {
                            logger.error("Couldn't generate view display object", ex);
                        }
                    }
                    Collections.sort(newData.topLevelViews);
                    Collections.sort(newData.partLevelViews);
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
                } else if (st.getPredicate().equals(HAS_ADMIN) && st.getObject() instanceof IRI obj) {
                    data.addAdmin(obj, rootNanopub.getUri().stringValue());
                } else if (st.getPredicate().equals(HAS_PINNED_TEMPLATE) && st.getObject() instanceof IRI obj) {
                    data.pinnedResources.add(TemplateData.get().getTemplate(obj.stringValue()));
                } else if (st.getPredicate().equals(HAS_PINNED_QUERY) && st.getObject() instanceof IRI obj) {
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

    @Override
    public String toString() {
        return id;
    }

}
