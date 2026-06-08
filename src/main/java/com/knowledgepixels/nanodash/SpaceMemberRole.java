package com.knowledgepixels.nanodash;

import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * A role that a space member can have, with associated properties.
 */
public class SpaceMemberRole implements Serializable {

    private IRI id;
    private String label, name, title;
    private Template roleAssignmentTemplate = null;
    private IRI[] regularProperties, inverseProperties;
    private IRI tier;

    /**
     * Rank of the "everyone" floor (no role held). Below {@link #OBSERVER_RANK}.
     */
    public static final int EVERYONE_RANK = 0;
    private static final int OBSERVER_RANK = 1;
    private static final int MEMBER_RANK = 2;
    private static final int MAINTAINER_RANK = 3;
    private static final int ADMIN_RANK = 4;

    /**
     * Construct a SpaceMemberRole from an API response entry.
     *
     * @param e The API response entry.
     */
    public SpaceMemberRole(ApiResponseEntry e) {
        this.id = Utils.vf.createIRI(e.get("role"));
        this.label = e.get("roleLabel");
        this.name = e.get("roleName");
        this.title = e.get("roleTitle");
        if (e.get("roleAssignmentTemplate") != null && !e.get("roleAssignmentTemplate").isBlank()) {
            this.roleAssignmentTemplate = TemplateData.get().getTemplate(e.get("roleAssignmentTemplate"));
        }
        regularProperties = stringToIriArray(e.get("regularProperties"));
        inverseProperties = stringToIriArray(e.get("inverseProperties"));
        this.tier = parseTier(e.get("roleType"));
    }

    private SpaceMemberRole(IRI id, String label, String name, String title, Template roleAssignmentTemplate, IRI[] regularProperties, IRI[] inverseProperties, IRI tier) {
        this.id = id;
        this.label = label;
        this.name = name;
        this.title = title;
        this.roleAssignmentTemplate = roleAssignmentTemplate;
        this.regularProperties = regularProperties;
        this.inverseProperties = inverseProperties;
        this.tier = tier;
    }

    /**
     * Parse the role tier from the {@code roleType} query column (the
     * server-materialized {@code npa:hasRoleType} value). Defaults to
     * {@link KPXL_TERMS#OBSERVER_ROLE} when absent, matching the server-side
     * default for roles that declare no tier subclass.
     *
     * @param roleType the role-type IRI string, or null/blank
     * @return the tier IRI (never null)
     */
    private static IRI parseTier(String roleType) {
        if (roleType == null || roleType.isBlank()) return KPXL_TERMS.OBSERVER_ROLE;
        return Utils.vf.createIRI(roleType);
    }

    /**
     * Check if this role is the admin role.
     *
     * @return True if this role is the admin role, false otherwise.
     */
    public boolean isAdminRole() {
        return id.equals(ADMIN_ROLE_IRI);
    }

    /**
     * Get the IRI of this role.
     *
     * @return The IRI of this role.
     */
    public IRI getId() {
        return id;
    }

    /**
     * Get the label of this role.
     *
     * @return The label of this role.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the name of this role.
     *
     * @return The name of this role.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the title of this role.
     *
     * @return The title of this role.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the template used for assigning this role.
     *
     * @return The template used for assigning this role.
     */
    public Template getRoleAssignmentTemplate() {
        return roleAssignmentTemplate;
    }

    /**
     * Get the regular properties associated with this role.
     *
     * @return The regular properties associated with this role.
     */
    public IRI[] getRegularProperties() {
        return regularProperties;
    }

    /**
     * Get the inverse properties associated with this role.
     *
     * @return The inverse properties associated with this role.
     */
    public IRI[] getInverseProperties() {
        return inverseProperties;
    }

    /**
     * Get the tier (role class) of this role — one of the role-tier IRIs in
     * {@link KPXL_TERMS} ({@code ADMIN_ROLE_TYPE} / {@code MAINTAINER_ROLE} /
     * {@code MEMBER_ROLE} / {@code OBSERVER_ROLE}).
     *
     * @return The tier IRI (never null; defaults to observer).
     */
    public IRI getTier() {
        return tier;
    }

    /**
     * Get the numeric rank of this role's tier, for threshold comparisons
     * (admin {@literal >} maintainer {@literal >} member {@literal >} observer).
     *
     * @return The tier rank (1..4).
     */
    public int getTierRank() {
        return tierRank(tier);
    }

    /**
     * Numeric rank of a role-tier IRI, for threshold comparisons. Unknown or
     * null tiers (the "everyone" floor) rank below observer.
     *
     * @param tier a role-tier IRI, or null
     * @return the rank: admin=4, maintainer=3, member=2, observer=1, else 0
     */
    public static int tierRank(IRI tier) {
        if (KPXL_TERMS.ADMIN_ROLE_TYPE.equals(tier)) return ADMIN_RANK;
        if (KPXL_TERMS.MAINTAINER_ROLE.equals(tier)) return MAINTAINER_RANK;
        if (KPXL_TERMS.MEMBER_ROLE.equals(tier)) return MEMBER_RANK;
        if (KPXL_TERMS.OBSERVER_ROLE.equals(tier)) return OBSERVER_RANK;
        return EVERYONE_RANK;
    }

    /**
     * Whether the given IRI is one of the known role-tier IRIs (as opposed to a
     * specific role IRI). Used to interpret {@code gen:isVisibleTo} objects.
     *
     * @param iri an IRI, or null
     * @return true if the IRI is a role tier
     */
    public static boolean isTier(IRI iri) {
        return KPXL_TERMS.ADMIN_ROLE_TYPE.equals(iri)
                || KPXL_TERMS.MAINTAINER_ROLE.equals(iri)
                || KPXL_TERMS.MEMBER_ROLE.equals(iri)
                || KPXL_TERMS.OBSERVER_ROLE.equals(iri);
    }

    /**
     * Add the role parameters to the given multimap.
     *
     * @param params The multimap to add the parameters to.
     */
    public void addRoleParams(Multimap<String, String> params) {
        for (IRI p : regularProperties) params.put("role", p.stringValue());
        for (IRI p : inverseProperties) params.put("invrole", p.stringValue());
    }


    private static final IRI ADMIN_ROLE_IRI = Utils.vf.createIRI("https://w3id.org/np/RA_eEJjQbxzSqYSwPzfjzOZi5sMPpUmHskFNsgJYSws8I/adminRole");
    private static final String ADMIN_ROLE_ASSIGNMENT_TEMPLATE_ID = "https://w3id.org/np/RAsOQ7k3GNnuUqZuLm57PWwWopQJR_4onnCpNR457CZg8";

    /**
     * The predefined admin role.
     */
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "Admin role", "admin", "Admins", TemplateData.get().getTemplate(ADMIN_ROLE_ASSIGNMENT_TEMPLATE_ID), new IRI[]{}, new IRI[]{KPXL_TERMS.HAS_ADMIN_PREDICATE}, KPXL_TERMS.ADMIN_ROLE_TYPE);

    /**
     * Convert a space-separated string of IRIs to an array of IRI objects.
     *
     * @param string The space-separated string of IRIs.
     * @return An array of IRI objects.
     */
    private static IRI[] stringToIriArray(String string) {
        if (string == null || string.isBlank()) return new IRI[]{};
        return Stream.of(string.split(" ")).map(Utils.vf::createIRI).toArray(IRI[]::new);
    }

    /**
     * Check if the current user is a member of the given space.
     *
     * @param space The space to check.
     * @return True if the current user is a member of the space, false otherwise.
     */
    public static boolean isCurrentUserMember(Space space) {
        if (space == null) return false;
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) return false;
        return space.isMember(userIri);
    }

    /**
     * Check if the current user is an admin of the given space.
     *
     * @param space The space to check.
     * @return True if the current user is an admin of the space, false otherwise.
     */
    public static boolean isCurrentUserAdmin(Space space) {
        if (space == null) return false;
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) return false;
        if (space.getMemberRoles(userIri) == null) return false;
        for (SpaceMemberRoleRef spaceMemberRoleRef : space.getMemberRoles(userIri)) {
            if (spaceMemberRoleRef.getRole().isAdminRole()) return true;
        }
        return false;
    }

}
