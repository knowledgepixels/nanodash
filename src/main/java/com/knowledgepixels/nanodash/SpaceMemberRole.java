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
    }

    private SpaceMemberRole(IRI id, String label, String name, String title, Template roleAssignmentTemplate, IRI[] regularProperties, IRI[] inverseProperties) {
        this.id = id;
        this.label = label;
        this.name = name;
        this.title = title;
        this.roleAssignmentTemplate = roleAssignmentTemplate;
        this.regularProperties = regularProperties;
        this.inverseProperties = inverseProperties;
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
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "Admin role", "admin", "Admins", TemplateData.get().getTemplate(ADMIN_ROLE_ASSIGNMENT_TEMPLATE_ID), new IRI[]{}, new IRI[]{KPXL_TERMS.HAS_ADMIN_PREDICATE});

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
