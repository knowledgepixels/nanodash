package com.knowledgepixels.nanodash.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import jakarta.xml.bind.DatatypeConverter;
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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Class representing a "Space", which can be any kind of collaborative unit, like a project, group, or event.
 */
public class Space extends AbstractResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(Space.class);

    @Override
    public boolean isDataInitialized() {
        triggerDataUpdate();
        return dataInitialized && super.isDataInitialized();
    }

    @Override
    public void setDataNeedsUpdate() {
        super.setDataNeedsUpdate();
        dataNeedsUpdate = true;
    }

    @Override
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
            for (String pubkeyHash : ud.getPubkeyHashes(admin, true)) {
                adminPubkeyMap.put(pubkeyHash, admin);
            }
            users.computeIfAbsent(admin, (k) -> new HashSet<>()).add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, npId));
        }

    }

    private static final Map<String, String> TYPE_EMOJIS = Map.ofEntries(
            Map.entry("Alliance", "\uD83E\uDD1D"),
            Map.entry("Consortium", "\u2602\uFE0F"),
            Map.entry("Organization", "\uD83C\uDFE2"),
            Map.entry("Taskforce", "\uD83C\uDFAF"),
            Map.entry("Division", "\uD83C\uDFD7\uFE0F"),
            Map.entry("Taskunit", "\u2699\uFE0F"),
            Map.entry("Group", "\uD83D\uDC65"),
            Map.entry("Project", "\uD83D\uDE80"),
            Map.entry("Program", "\uD83D\uDCCB"),
            Map.entry("Initiative", "\uD83D\uDCA1"),
            Map.entry("Outlet", "\uD83D\uDCF0"),
            Map.entry("Campaign", "\uD83D\uDCE3"),
            Map.entry("Community", "\uD83C\uDF10"),
            Map.entry("Event", "\uD83C\uDFAA")
    );

    /**
     * Get the emoji associated with a space type name.
     *
     * @param typeName The short type name (e.g., "Alliance").
     * @return The emoji string, or an empty string if not found.
     */
    public static String getTypeEmoji(String typeName) {
        return TYPE_EMOJIS.getOrDefault(typeName, "");
    }

    private static String getCoreInfoString(ApiResponseEntry resp) {
        String id = resp.get("space");
        String rootNanopubId = resp.get("np");
        return id + " " + rootNanopubId;
    }

    private volatile boolean dataInitialized = false;
    private volatile boolean dataNeedsUpdate = true;

    Space(ApiResponseEntry resp) {
        super(resp.get("space"));
        initSpace(this);
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);
        setCoreData(data);
    }

    void updateFromApi(ApiResponseEntry resp) {
        String newNpId = resp.get("np");
        if (!newNpId.equals(this.rootNanopubId)) {
            this.label = resp.get("label");
            this.type = resp.get("type");
            this.rootNanopubId = newNpId;
            this.rootNanopub = Utils.getAsNanopub(newNpId);
            setDataNeedsUpdate();
        }
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

    @Override
    public String getNamespace() {
        // FIXME this will be removed in the future
        return null;
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

    /**
     * Check if a public key is associated with an admin of this space.
     *
     * @param pubkey The public key hash to check.
     * @return true if the public key is associated with an admin, false otherwise.
     */
    public boolean isAdminPubkey(String pubkey) {
        ensureInitialized();
        return data.adminPubkeyMap.containsKey(pubkey);
    }

    /**
     * Get the list of pinned resources in this space.
     *
     * @return Set of pinned resources.
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

    @Override
    public boolean appliesTo(String elementId, Set<IRI> classes) {
        triggerSpaceDataUpdate();
        return super.appliesTo(elementId, classes);
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
     * Get alternative IDs for the space.
     *
     * @return List of alternative IDs.
     */
    public List<String> getAltIDs() {
        return data.altIds;
    }

    private synchronized void ensureInitialized() {
        Future<?> future = triggerSpaceDataUpdate();
        if (!dataInitialized && future != null) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                logger.error("failed to await space data update", ex);
            }
        }
        future = super.triggerDataUpdate();
        if (!dataInitialized && future != null) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                logger.error("failed to await data update", ex);
            }
        }
    }

    @Override
    public synchronized Future<?> triggerDataUpdate() {
        triggerSpaceDataUpdate();
        return super.triggerDataUpdate();
    }

    private synchronized Future<?> triggerSpaceDataUpdate() {
        if (dataNeedsUpdate) {
            logger.info("Data needs update for space {} core data, starting update thread", getId());
            dataNeedsUpdate = false;
            return NanodashThreadPool.submit(() -> {
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

                    ApiResponse getAdminsResponse = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_ADMINS, spaceIds), true);
                    boolean continueAddingAdmins = true;
                    while (continueAddingAdmins) {
                        continueAddingAdmins = false;
                        for (ApiResponseEntry r : getAdminsResponse.getData()) {
                            String pubkeyHash = r.get("pubkey");
                            if (newData.adminPubkeyMap.containsKey(pubkeyHash)) {
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

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACE_MEMBER_ROLES, spaceIds), true).getData()) {
                        if (!newData.adminPubkeyMap.containsKey(r.get("pubkey"))) continue;
                        SpaceMemberRole role = new SpaceMemberRole(r);
                        newData.roles.add(new SpaceMemberRoleRef(role, r.get("np")));

                        // TODO Handle cases of overlapping properties:
                        for (IRI p : role.getRegularProperties()) newData.roleMap.put(p, role);
                        for (IRI p : role.getInverseProperties()) newData.roleMap.put(p, role);

                        role.addRoleParams(getSpaceMemberParams);
                    }

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_SPACE_MEMBERS, getSpaceMemberParams), true).getData()) {
                        IRI memberId = Utils.vf.createIRI(r.get("member"));
                        SpaceMemberRole role = newData.roleMap.get(Utils.vf.createIRI(r.get("role")));
                        newData.users.computeIfAbsent(memberId, (k) -> new HashSet<>()).add(new SpaceMemberRoleRef(role, r.get("np")));
                    }

                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_PINNED_TEMPLATES, spaceIds), true).getData()) {
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
                    for (ApiResponseEntry r : ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_PINNED_QUERIES, spaceIds), true).getData()) {
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
                    logger.error("Error while trying to update space data: {}", ex.getMessage());
                    dataNeedsUpdate = true;
                }
            });
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
