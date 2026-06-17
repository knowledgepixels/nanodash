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

/**
 * Class representing a "Space", which can be any kind of collaborative unit, like a project, group, or event.
 */
public class Space extends AbstractResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(Space.class);

    private String label, rootNanopubId, type;
    private Nanopub rootNanopub = null;
    // Space-ref identity (space IRI + root-definition NPID). Populated only by the
    // ref-aware get-spaces query (v3); null/empty with the pre-v3 query. See
    // docs/space-ref-identity.md. refRootId = the representative ref's root nanopub;
    // refRoots = the roots of all distinct refs that claim this space IRI.
    private String refRootId = null;
    private Set<String> refRoots = Collections.emptySet();

    // Core data — derived directly from the root nanopub assertion.
    private final List<String> altIds = new ArrayList<>();
    private final List<IRI> rootAdmins = new ArrayList<>();
    private String description = null;
    private Calendar startDate, endDate;
    private IRI defaultProvenance = null;

    // Derived data — admins / members / roles loaded from the spaces repo and
    // memoised by ApiResponse object identity (rebuild when ApiCache returns a
    // fresh response for any of the three underlying queries).
    private volatile ApiResponse cachedAdminsResp, cachedRolesResp, cachedMembersResp;
    private volatile SpaceData cachedData = SpaceData.EMPTY;

    private static class SpaceData implements Serializable {

        final List<IRI> admins;
        final Map<IRI, Set<SpaceMemberRoleRef>> users;
        final List<SpaceMemberRoleRef> roles;
        final Map<IRI, SpaceMemberRole> roleMap;
        // Members whose every role instantiation is un-introduced/unvalidated (not in the
        // trust-validated state). Shown, but flagged. See docs/space-ref-identity.md.
        final Set<IRI> unvalidatedMembers;

        SpaceData(List<IRI> admins, Map<IRI, Set<SpaceMemberRoleRef>> users,
                  List<SpaceMemberRoleRef> roles, Map<IRI, SpaceMemberRole> roleMap,
                  Set<IRI> unvalidatedMembers) {
            this.admins = admins;
            this.users = users;
            this.roles = roles;
            this.roleMap = roleMap;
            this.unvalidatedMembers = unvalidatedMembers;
        }

        static final SpaceData EMPTY = new SpaceData(
                Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyMap(), Collections.emptySet());
    }

    private static final Map<String, String> TYPE_EMOJIS = Map.ofEntries(
            Map.entry("Alliance", "🤝"),
            Map.entry("Consortium", "☂️"),
            Map.entry("Organization", "🏢"),
            Map.entry("Taskforce", "🎯"),
            Map.entry("Division", "🏗️"),
            Map.entry("Taskunit", "⚙️"),
            Map.entry("Group", "👥"),
            Map.entry("Project", "🚀"),
            Map.entry("Program", "📋"),
            Map.entry("Initiative", "💡"),
            Map.entry("Outlet", "📰"),
            Map.entry("Campaign", "📣"),
            Map.entry("Community", "🌐"),
            Map.entry("Event", "🎪")
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

    Space(ApiResponseEntry resp) {
        super(resp.get("space"));
        initSpace(this);
        this.label = resp.get("label");
        this.type = resp.get("type");
        this.rootNanopubId = resp.get("np");
        this.rootNanopub = Utils.getAsNanopub(rootNanopubId);
        this.refRootId = resp.get("ref_root");
        readCoreData();
    }

    void updateFromApi(ApiResponseEntry resp) {
        this.refRootId = resp.get("ref_root");
        String newNpId = resp.get("np");
        if (!newNpId.equals(this.rootNanopubId)) {
            this.label = resp.get("label");
            this.type = resp.get("type");
            this.rootNanopubId = newNpId;
            this.rootNanopub = Utils.getAsNanopub(newNpId);
            altIds.clear();
            rootAdmins.clear();
            description = null;
            startDate = null;
            endDate = null;
            defaultProvenance = null;
            readCoreData();
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
     * Get the root-definition nanopub ID of this space's ref (the representative ref
     * when several refs claim the same space IRI). The space ref identity is the space
     * IRI plus this root NPID. Populated only by the ref-aware get-spaces query (v3);
     * null with the pre-v3 query. See docs/space-ref-identity.md.
     *
     * @return The representative ref's root nanopub ID, or null if unknown.
     */
    public String getRefRootId() {
        return refRootId;
    }

    /**
     * Scope a space's view displays to its representative ref so a multi-ref identifier doesn't
     * merge Content-tab displays across rival definitions. {@code ?root=}-pinned pages pass the
     * pinned ref explicitly via {@link #getTopLevelViewDisplays(String)} instead.
     */
    @Override
    protected String getViewDisplayRefRoot() {
        return refRootId;
    }

    /**
     * Get the root nanopub IDs of all distinct refs that claim this space IRI. More than
     * one means several space definitions claim the same IRI (distinct spaces, not a
     * conflict to resolve). Empty with the pre-v3 query. See docs/space-ref-identity.md.
     *
     * @return The set of ref root nanopub IDs (possibly empty).
     */
    public Set<String> getRefRoots() {
        return refRoots;
    }

    /**
     * Set the root nanopub IDs of all distinct refs claiming this space IRI. Called by
     * the repository while building the space listing from the ref-aware get-spaces query.
     *
     * @param refRoots The set of ref root nanopub IDs (may be null/empty).
     */
    public void setRefRoots(Set<String> refRoots) {
        this.refRoots = (refRoots == null || refRoots.isEmpty()) ? Collections.emptySet() : refRoots;
    }

    /**
     * Get the number of distinct refs claiming this space IRI (at least 1).
     *
     * @return The ref count (1 when ref data is unavailable).
     */
    public int getRefCount() {
        return Math.max(1, refRoots.size());
    }

    /**
     * Whether this space IRI is claimed by multiple refs (root definitions) with
     * <em>differing</em> admin sets — i.e. rival definitions, as opposed to same-owner
     * stray duplicates (which share an admin set and stay collapsed). Resolves each ref's
     * validated admin set via the ref-scoped admins query and reports a conflict as soon as
     * two of them differ. Returns false for the common single-ref case. See
     * docs/space-ref-identity.md.
     *
     * @return true if at least two refs of this IRI have different admin agent sets
     */
    public boolean hasConflictingRefs() {
        if (refRoots.size() < 2) return false;
        Set<Set<String>> distinctAdminSets = new HashSet<>();
        for (String rootNp : refRoots) {
            distinctAdminSets.add(adminAgentsForRef(rootNp));
            if (distinctAdminSets.size() > 1) return true;
        }
        return false;
    }

    /**
     * All refs (root definitions) claiming this space's IRI, each with its validated admin
     * agents — the representative (currently-shown) ref first. Used by the disambiguation
     * claimants view. See docs/space-ref-identity.md.
     *
     * @return the claimants, representative first (a singleton for the common single-ref case)
     */
    public List<RefClaimant> getRefClaimants() {
        List<String> ordered = new ArrayList<>();
        if (refRootId != null && !refRootId.isEmpty()) ordered.add(refRootId);
        for (String root : refRoots) {
            if (!root.equals(refRootId)) ordered.add(root);
        }
        List<RefClaimant> claimants = new ArrayList<>();
        for (String root : ordered) {
            claimants.add(new RefClaimant(root, adminAgentsForRef(root), root.equals(refRootId)));
        }
        return claimants;
    }

    /** A single ref (root definition) claiming a space's IRI, with its validated admins. */
    public static final class RefClaimant implements Serializable {
        private final String rootNp;
        private final List<String> admins;
        private final boolean representative;

        RefClaimant(String rootNp, Set<String> admins, boolean representative) {
            this.rootNp = rootNp;
            this.admins = new ArrayList<>(admins);
            this.representative = representative;
        }

        /** The root-definition nanopub ID identifying this ref. */
        public String getRootNp() {
            return rootNp;
        }

        /** The validated admin agent IRIs of this ref. */
        public List<String> getAdmins() {
            return admins;
        }

        /** Whether this is the representative ref currently shown on the space page. */
        public boolean isRepresentative() {
            return representative;
        }
    }

    /** The validated admin agent IRIs for a single ref (by its root nanopub). */
    private Set<String> adminAgentsForRef(String rootNp) {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("root_np", rootNp);
        ApiResponse resp = ApiCache.retrieveResponseSync(
                new QueryRef(QueryApiAccess.GET_SPACE_ADMINS_REF, params), false);
        Set<String> agents = new HashSet<>();
        if (resp != null) {
            for (ApiResponseEntry r : resp.getData()) {
                String agent = r.get("agent");
                if (agent != null && !agent.isEmpty()) agents.add(agent);
            }
        }
        return agents;
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
    protected Set<IRI> getOwnClasses() {
        return Set.of(KPXL_TERMS.SPACE);
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
        return startDate;
    }

    /**
     * Get the end date of the space.
     *
     * @return The end date as a Calendar object, or null if not set.
     */
    public Calendar getEndDate() {
        return endDate;
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
        return description;
    }


    /**
     * Get the list of admins in this space.
     *
     * @return List of admin IRIs.
     */
    public List<IRI> getAdmins() {
        return currentData().admins;
    }

    /**
     * Get the list of members in this space.
     *
     * @return List of member IRIs.
     */
    public List<IRI> getUsers() {
        List<IRI> users = new ArrayList<>(currentData().users.keySet());
        users.sort(User.getUserData().userComparator);
        return users;
    }

    /**
     * Whether a member's membership is trust-validated (their key has a trust-approved
     * AccountState from an accepted introduction). Admins and any member with at least one
     * validated role instantiation are considered validated; un-introduced self-declared
     * members are not. See docs/space-ref-identity.md.
     *
     * @param userId The IRI of the member.
     * @return false only when the member is present but every instantiation is unvalidated.
     */
    public boolean isMemberValidated(IRI userId) {
        return !currentData().unvalidatedMembers.contains(userId);
    }

    /**
     * Get the roles of a specific member in this space.
     *
     * @param userId The IRI of the member.
     * @return Set of roles assigned to the member, or null if the member is not part of this space.
     */
    public Set<SpaceMemberRoleRef> getMemberRoles(IRI userId) {
        return currentData().users.get(userId);
    }

    /**
     * Check if a user is a member of this space.
     *
     * @param userId The IRI of the user to check.
     * @return true if the user is a member, false otherwise.
     */
    public boolean isMember(IRI userId) {
        return currentData().users.containsKey(userId);
    }

    /**
     * Get the highest role tier the given user holds in this space, as a numeric
     * rank for threshold comparisons (admin {@literal >} maintainer {@literal >}
     * member {@literal >} observer). A user holding no role gets
     * {@link SpaceMemberRole#EVERYONE_RANK}.
     *
     * @param userId The IRI of the user.
     * @return the highest tier rank held, or the "everyone" floor if none.
     */
    public int userTier(IRI userId) {
        Set<SpaceMemberRoleRef> roles = getMemberRoles(userId);
        if (roles == null || roles.isEmpty()) return SpaceMemberRole.EVERYONE_RANK;
        int max = SpaceMemberRole.EVERYONE_RANK;
        for (SpaceMemberRoleRef ref : roles) {
            max = Math.max(max, ref.getRole().getTierRank());
        }
        return max;
    }

    /**
     * Check whether the given user holds the specific role (by role IRI) in this
     * space.
     *
     * @param userId  The IRI of the user.
     * @param roleIri The specific role IRI.
     * @return true if the user holds that exact role, false otherwise.
     */
    public boolean viewerHoldsRole(IRI userId, IRI roleIri) {
        Set<SpaceMemberRoleRef> roles = getMemberRoles(userId);
        if (roles == null) return false;
        for (SpaceMemberRoleRef ref : roles) {
            if (ref.getRole().getId().equals(roleIri)) return true;
        }
        return false;
    }

    /**
     * Check if a public key is associated with an admin of this space.
     *
     * @param pubkey The public key hash to check.
     * @return true if the public key is associated with an admin, false otherwise.
     */
    public boolean isAdminPubkey(String pubkey) {
        if (pubkey == null) return false;
        ApiResponse resp = ApiCache.retrieveResponseSync(
                spaceQueryRef(QueryApiAccess.GET_SPACE_ADMIN_PUBKEY_HASHES_REF, QueryApiAccess.GET_SPACE_ADMIN_PUBKEY_HASHES), false);
        if (resp == null) return false;
        for (ApiResponseEntry r : resp.getData()) {
            if (pubkey.equals(r.get("pkh"))) return true;
        }
        return false;
    }

    /**
     * Get the default provenance IRI for this space.
     *
     * @return The default provenance IRI, or null if not set.
     */
    public IRI getDefaultProvenance() {
        return defaultProvenance;
    }

    /**
     * Get the roles defined in this space.
     *
     * @return List of roles.
     */
    public List<SpaceMemberRoleRef> getRoles() {
        return currentData().roles;
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
        return altIds;
    }

    @Override
    public void forceRefresh(long waitMillis) {
        super.forceRefresh(waitMillis);
        ApiCache.clearCache(spaceQueryRef(QueryApiAccess.GET_SPACE_ADMINS_REF, QueryApiAccess.GET_SPACE_ADMINS), waitMillis);
        ApiCache.clearCache(spaceQueryRef(QueryApiAccess.GET_SPACE_ROLES_REF, QueryApiAccess.GET_SPACE_ROLES), waitMillis);
        ApiCache.clearCache(spaceQueryRef(QueryApiAccess.GET_SPACE_MEMBERS_REF, QueryApiAccess.GET_SPACE_MEMBERS), waitMillis);
        ApiCache.clearCache(spaceQueryRef(QueryApiAccess.GET_SPACE_ADMIN_PUBKEY_HASHES_REF, QueryApiAccess.GET_SPACE_ADMIN_PUBKEY_HASHES), waitMillis);
    }

    private List<String> allSpaceIris() {
        List<String> iris = new ArrayList<>(altIds.size() + 1);
        iris.add(getId());
        iris.addAll(altIds);
        return iris;
    }

    private synchronized SpaceData currentData() {
        ApiResponse adminsResp = ApiCache.retrieveResponseSync(
                spaceQueryRef(QueryApiAccess.GET_SPACE_ADMINS_REF, QueryApiAccess.GET_SPACE_ADMINS), false);
        ApiResponse rolesResp = ApiCache.retrieveResponseSync(
                spaceQueryRef(QueryApiAccess.GET_SPACE_ROLES_REF, QueryApiAccess.GET_SPACE_ROLES), false);
        ApiResponse membersResp = ApiCache.retrieveResponseSync(
                spaceQueryRef(QueryApiAccess.GET_SPACE_MEMBERS_REF, QueryApiAccess.GET_SPACE_MEMBERS), false);
        if (adminsResp == cachedAdminsResp && rolesResp == cachedRolesResp && membersResp == cachedMembersResp) {
            return cachedData;
        }
        SpaceData newData = buildData(adminsResp, rolesResp, membersResp);
        cachedAdminsResp = adminsResp;
        cachedRolesResp = rolesResp;
        cachedMembersResp = membersResp;
        cachedData = newData;
        return newData;
    }

    private SpaceData buildData(ApiResponse adminsResp, ApiResponse rolesResp, ApiResponse membersResp) {
        List<IRI> admins = new ArrayList<>();
        Map<IRI, Set<SpaceMemberRoleRef>> users = new HashMap<>();
        List<SpaceMemberRoleRef> roles = new ArrayList<>();
        Map<IRI, SpaceMemberRole> roleMap = new HashMap<>();

        // Seed from rootNanopub-derived state
        for (IRI rootAdmin : rootAdmins) {
            admins.add(rootAdmin);
            users.computeIfAbsent(rootAdmin, k -> new HashSet<>())
                    .add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, rootNanopubId));
        }
        roles.add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, null));
        roleMap.put(KPXL_TERMS.HAS_ADMIN_PREDICATE, SpaceMemberRole.ADMIN_ROLE);

        loadAdmins(admins, users, adminsResp);
        admins.sort(User.getUserData().userComparator);

        loadRoles(roles, roleMap, rolesResp);
        Set<IRI> unvalidatedMembers = new HashSet<>();
        loadMembers(users, roleMap, membersResp, unvalidatedMembers);

        return new SpaceData(admins, users, roles, roleMap, unvalidatedMembers);
    }

    private static Multimap<String, String> spaceParams(List<String> spaceIris) {
        Multimap<String, String> params = ArrayListMultimap.create();
        for (String iri : spaceIris) params.put("space", iri);
        return params;
    }

    /**
     * Query reference for a per-space query that has both a ref-scoped variant (param
     * root_np = the ref's root nanopub) and an IRI-keyed variant. When the ref root is
     * known (ref-aware get-spaces) use the ref-scoped query so multi-ref spaces don't merge
     * authority across refs; otherwise fall back to the IRI-keyed query. See
     * docs/space-ref-identity.md.
     */
    private QueryRef spaceQueryRef(String refQueryId, String iriQueryId) {
        if (refRootId != null && !refRootId.isEmpty()) {
            Multimap<String, String> p = ArrayListMultimap.create();
            p.put("root_np", refRootId);
            return new QueryRef(refQueryId, p);
        }
        return new QueryRef(iriQueryId, spaceParams(allSpaceIris()));
    }

    private static void loadAdmins(List<IRI> admins, Map<IRI, Set<SpaceMemberRoleRef>> users, ApiResponse resp) {
        if (resp == null) return;
        for (ApiResponseEntry r : resp.getData()) {
            IRI adminId = Utils.vf.createIRI(r.get("agent"));
            String np = r.get("np");
            if (admins.contains(adminId)) continue;
            admins.add(adminId);
            users.computeIfAbsent(adminId, k -> new HashSet<>())
                    .add(new SpaceMemberRoleRef(SpaceMemberRole.ADMIN_ROLE, np));
        }
    }

    private static void loadRoles(List<SpaceMemberRoleRef> roles, Map<IRI, SpaceMemberRole> roleMap, ApiResponse resp) {
        if (resp == null) return;
        for (ApiResponseEntry r : resp.getData()) {
            ApiResponseEntry entry = new ApiResponseEntry();
            for (String k : List.of("role", "roleLabel", "roleName", "roleTitle",
                    "roleAssignmentTemplate", "regularProperties", "inverseProperties", "roleType")) {
                String v = r.get(k);
                if (v != null && !v.isEmpty()) entry.add(k, v);
            }
            SpaceMemberRole role = new SpaceMemberRole(entry);
            String raNp = r.get("ra_np");
            if (raNp != null && raNp.isEmpty()) raNp = null;
            roles.add(new SpaceMemberRoleRef(role, raNp));
            for (IRI p : role.getRegularProperties()) roleMap.put(p, role);
            for (IRI p : role.getInverseProperties()) roleMap.put(p, role);
        }
    }

    private static void loadMembers(Map<IRI, Set<SpaceMemberRoleRef>> users, Map<IRI, SpaceMemberRole> roleMap, ApiResponse resp, Set<IRI> unvalidatedMembers) {
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
        if (resp == null) return;
        // The ref-scoped query carries a ?validated flag per row (true when the membership
        // is in the trust-validated state). When present, a member with no validated row is
        // flagged as unvalidated (shown, but un-introduced). The IRI-keyed fallback query
        // has no flag, so nothing is flagged.
        Set<IRI> seenMembers = new HashSet<>();
        Set<IRI> validatedMembers = new HashSet<>();
        boolean hasValidationFlag = false;
        for (ApiResponseEntry r : resp.getData()) {
            SpaceMemberRole role = null;
            for (String key : new String[] {"regProp", "invProp"}) {
                String val = r.get(key);
                if (val != null && !val.isEmpty()) {
                    IRI pred = Utils.vf.createIRI(val);
                    SpaceMemberRole candidate = roleMap.get(pred);
                    if (candidate != null) { role = candidate; break; }
                }
            }
            // Gate by role-predicate match: only admit members whose predicate
            // corresponds to a role that was attached to this space by an admin
            // (roleMap is populated from validated gen:RoleAssignment rows
            // in loadRoles).
            if (role == null) continue;
            IRI memberId = Utils.vf.createIRI(r.get("member"));
            String np = r.get("np");
            users.computeIfAbsent(memberId, k -> new HashSet<>())
                    .add(new SpaceMemberRoleRef(role, np));
            seenMembers.add(memberId);
            String validated = r.get("validated");
            if (validated != null) {
                hasValidationFlag = true;
                if ("true".equals(validated)) validatedMembers.add(memberId);
            }
        }
        if (hasValidationFlag) {
            for (IRI memberId : seenMembers) {
                if (!validatedMembers.contains(memberId)) unvalidatedMembers.add(memberId);
            }
        }
    }

    private void readCoreData() {
        for (Statement st : rootNanopub.getAssertion()) {
            if (st.getSubject().stringValue().equals(getId())) {
                if (st.getPredicate().equals(OWL.SAMEAS) && st.getObject() instanceof IRI objIri) {
                    altIds.add(objIri.stringValue());
                } else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                    description = st.getObject().stringValue();
                } else if (st.getPredicate().stringValue().equals("http://schema.org/startDate")) {
                    try {
                        startDate = DatatypeConverter.parseDateTime(st.getObject().stringValue());
                    } catch (IllegalArgumentException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().stringValue().equals("http://schema.org/endDate")) {
                    try {
                        endDate = DatatypeConverter.parseDateTime(st.getObject().stringValue());
                    } catch (IllegalArgumentException ex) {
                        logger.error("Failed to parse date {}", st.getObject().stringValue());
                    }
                } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ADMIN) && st.getObject() instanceof IRI obj) {
                    if (!rootAdmins.contains(obj)) rootAdmins.add(obj);
                } else if (st.getPredicate().equals(NTEMPLATE.HAS_DEFAULT_PROVENANCE) && st.getObject() instanceof IRI obj) {
                    defaultProvenance = obj;
                }
            }
        }
    }

}
