package com.knowledgepixels.nanodash.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.IRI;
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
        Map<String, IRI> userPubkeyMap = new HashMap<>();

        void addAdmin(IRI admin, String npId) {
            // TODO This isn't efficient for long owner lists:
            if (admins.contains(admin)) return;
            admins.add(admin);
            // adminPubkeyMap is populated in bulk by loadAdminPubkeyHashesFromSpacesRepo,
            // sourcing trust-state-validated (agent, pubkey-hash) pairs from the
            // spaces repo's npa:AccountState rows.
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
    private transient volatile Future<?> spaceDataFuture = null;

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
     * Get the trust-state-validated pubkey hashes of this space's admins.
     *
     * @return Set of admin pubkey hashes, sourced from the spaces repo.
     */
    public Set<String> getAdminPubkeyHashes() {
        ensureInitialized();
        return data.adminPubkeyMap.keySet();
    }

    /**
     * Get the trust-state-validated pubkey hashes of this space's users
     * (admins + members).
     *
     * @return Set of user pubkey hashes, sourced from the spaces repo.
     */
    public Set<String> getUserPubkeyHashes() {
        ensureInitialized();
        return data.userPubkeyMap.keySet();
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
        triggerSpaceDataUpdate();
        if (!dataInitialized && spaceDataFuture != null) {
            try {
                spaceDataFuture.get(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                logger.error("failed to await space data update", ex);
            }
        }
        Future<?> future = super.triggerDataUpdate();
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
            spaceDataFuture = NanodashThreadPool.submit(() -> {
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

                    List<String> spaceIris = new ArrayList<>();
                    spaceIris.add(getId());
                    spaceIris.addAll(newData.altIds);

                    loadAdminsFromSpacesRepo(newData, spaceIris);
                    newData.admins.sort(User.getUserData().userComparator);
                    loadAdminPubkeyHashesFromSpacesRepo(newData, spaceIris);

                    loadRolesFromSpacesRepo(newData, spaceIris);
                    loadMembersFromSpacesRepo(newData, spaceIris);
                    loadUserPubkeyHashesFromSpacesRepo(newData);

                    data = newData;
                    dataInitialized = true;
                } catch (Exception ex) {
                    logger.error("Error while trying to update space data: {}", ex.getMessage());
                    dataNeedsUpdate = true;
                }
            });
            return spaceDataFuture;
        }
        return spaceDataFuture;
    }

    private static Multimap<String, String> spaceParams(List<String> spaceIris) {
        Multimap<String, String> params = ArrayListMultimap.create();
        for (String iri : spaceIris) params.put("space", iri);
        return params;
    }

    private static List<ApiResponseEntry> runSpacesQuery(String queryId, Multimap<String, String> params) {
        // Like the prior direct-SPARQL path, a failed/empty query yields an empty
        // list rather than aborting the rest of the space-data load.
        ApiResponse resp = ApiCache.retrieveResponseSync(new QueryRef(queryId, params), true);
        return resp == null ? List.of() : resp.getData();
    }

    private static void loadAdminsFromSpacesRepo(SpaceData data, List<String> spaceIris) {
        for (ApiResponseEntry r : runSpacesQuery(QueryApiAccess.GET_SPACE_ADMINS, spaceParams(spaceIris))) {
            IRI adminId = Utils.vf.createIRI(r.get("agent"));
            String np = r.get("np");
            if (!data.admins.contains(adminId)) data.addAdmin(adminId, np);
        }
    }

    private static void loadAdminPubkeyHashesFromSpacesRepo(SpaceData data, List<String> spaceIris) {
        // TODO Push the view-display admin gate server-side (a published
        // get-view-displays variant that joins through the spaces-repo admins) so
        // the adminPubkeyMap dependency disappears entirely — see callers of
        // Space.isAdminPubkey in AbstractResourceWithProfile and DownloadRdfPage.
        //
        // Source: trust-state-validated (agent, pubkey-hash) pairs for the
        // space's admin RIs. Stricter than the prior UserData.getPubkeyHashes
        // source (only pubkeys that are in the current trust state count).
        for (ApiResponseEntry r : runSpacesQuery(QueryApiAccess.GET_SPACE_ADMIN_PUBKEY_HASHES, spaceParams(spaceIris))) {
            data.adminPubkeyMap.put(r.get("pkh"), Utils.vf.createIRI(r.get("agent")));
        }
    }

    private static void loadRolesFromSpacesRepo(SpaceData data, List<String> spaceIris) {
        for (ApiResponseEntry r : runSpacesQuery(QueryApiAccess.GET_SPACE_ROLES, spaceParams(spaceIris))) {
            ApiResponseEntry entry = new ApiResponseEntry();
            for (String k : List.of("role", "roleLabel", "roleName", "roleTitle",
                    "roleAssignmentTemplate", "regularProperties", "inverseProperties")) {
                String v = r.get(k);
                if (v != null && !v.isEmpty()) entry.add(k, v);
            }
            SpaceMemberRole role = new SpaceMemberRole(entry);
            String raNp = r.get("ra_np");
            if (raNp != null && raNp.isEmpty()) raNp = null;
            data.roles.add(new SpaceMemberRoleRef(role, raNp));
            for (IRI p : role.getRegularProperties()) data.roleMap.put(p, role);
            for (IRI p : role.getInverseProperties()) data.roleMap.put(p, role);
        }
    }

    private static void loadMembersFromSpacesRepo(SpaceData data, List<String> spaceIris) {
        // Pulls RI candidates from the extraction graph (npa:spacesGraph) rather
        // than the validated state graph. The materialiser is correctly stricter
        // (e.g., it stops admitting RIs when their role declaration is
        // invalidated, even if the role assignment still points at the old IRI;
        // and the design admits non-admin-published observer-tier RIs only via
        // AccountState self-evidence, dropping any whose agents aren't in the
        // trust state). Nanodash matches the looser pre-migration semantic of
        // get-space-members: any RI whose predicate corresponds to a role
        // attached to the space (the admin-gating happens server-side on the
        // role attachment, not on the per-member nanopub) is admitted,
        // regardless of who signed the member RI. Invalidated rows are filtered
        // server-side via the npx:invalidates triple in npa:graph.
        for (ApiResponseEntry r : runSpacesQuery(QueryApiAccess.GET_SPACE_MEMBERS, spaceParams(spaceIris))) {
            SpaceMemberRole role = null;
            for (String key : new String[] {"regProp", "invProp"}) {
                String val = r.get(key);
                if (val != null && !val.isEmpty()) {
                    IRI pred = Utils.vf.createIRI(val);
                    SpaceMemberRole candidate = data.roleMap.get(pred);
                    if (candidate != null) { role = candidate; break; }
                }
            }
            // Gate by role-predicate match: only admit members whose predicate
            // corresponds to a role that was attached to this space by an admin
            // (data.roleMap is populated from validated gen:RoleAssignment rows
            // in loadRolesFromSpacesRepo).
            if (role == null) continue;
            IRI memberId = Utils.vf.createIRI(r.get("member"));
            String np = r.get("np");
            data.users.computeIfAbsent(memberId, k -> new HashSet<>())
                    .add(new SpaceMemberRoleRef(role, np));
        }
    }

    private static void loadUserPubkeyHashesFromSpacesRepo(SpaceData data) {
        // TODO Push the per-view pubkey filtering server-side too, so view queries
        // gate by user-of-space without nanodash having to expand the placeholder
        // client-side at all — see callers of Space.getUserPubkeyHashes /
        // getAdminPubkeyHashes in ViewList and DownloadRdfPage.
        //
        // Source: trust-state-validated (agent, pubkey-hash) pairs from
        // npa:AccountState for every user agent (admin or admin-gated member)
        // already loaded into data.users. Stricter than the prior
        // UserData.getPubkeyHashes source — pubkey hashes of users not in
        // the current trust state are not returned.
        if (data.users.isEmpty()) return;
        Multimap<String, String> params = ArrayListMultimap.create();
        for (IRI agent : data.users.keySet()) {
            params.put("agent", agent.stringValue());
        }
        for (ApiResponseEntry r : runSpacesQuery(QueryApiAccess.GET_AGENT_PUBKEY_HASHES, params)) {
            data.userPubkeyMap.put(r.get("pkh"), Utils.vf.createIRI(r.get("agent")));
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
                } else if (st.getPredicate().equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE) && st.getObject() instanceof IRI obj) {
                    data.defaultProvenance = obj;
                }
            }
        }
    }

}
