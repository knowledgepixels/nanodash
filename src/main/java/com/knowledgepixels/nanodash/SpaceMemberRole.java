package com.knowledgepixels.nanodash;

import com.google.common.collect.Multimap;
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
        regularProperties = stringToIriArray(e.get("regularProperties"));
        inverseProperties = stringToIriArray(e.get("inverseProperties"));
    }

    private SpaceMemberRole(IRI id, String label, String name, String title, IRI[] regularProperties, IRI[] inverseProperties) {
        this.id = id;
        this.label = label;
        this.name = name;
        this.title = title;
        this.regularProperties = regularProperties;
        this.inverseProperties = inverseProperties;
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

    /**
     * The IRI for the "hasAdmin" predicate.
     */
    public static final IRI HAS_ADMIN_PREDICATE = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin");
    private static final IRI ADMIN_ROLE_IRI = Utils.vf.createIRI("https://w3id.org/np/RAHlMUH4GnbkUmGTK_eecBk3OBFn55VyQHC0BDlpOcCPg/adminRole");
    /**
     * The predefined admin role.
     */
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "Admin role", "admin", "Admins", new IRI[]{HAS_ADMIN_PREDICATE}, new IRI[]{});

    private static IRI[] stringToIriArray(String string) {
        if (string == null || string.isBlank()) return new IRI[]{};
        return Stream.of(string.split(" ")).map(s -> Utils.vf.createIRI(s)).toArray(IRI[]::new);
    }

}
