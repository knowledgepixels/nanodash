package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

import com.google.common.collect.Multimap;

public class SpaceMemberRole implements Serializable {

    private IRI id;
    private String label, name, title;
    private IRI[] regularProperties, inverseProperties;

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

    public IRI getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public IRI[] getRegularProperties() {
        return regularProperties;
    }

    public IRI[] getInverseProperties() {
        return inverseProperties;
    }

    public void addRoleParams(Multimap<String, String> params) {
        for (IRI p : regularProperties) params.put("role", p.stringValue());
        for (IRI p : inverseProperties) params.put("invrole", p.stringValue());
    }

    public static final IRI HAS_ADMIN_PREDICATE = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin");
    private static final IRI ADMIN_ROLE_IRI = Utils.vf.createIRI("https://w3id.org/np/RAHlMUH4GnbkUmGTK_eecBk3OBFn55VyQHC0BDlpOcCPg/adminRole");
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "Admin role", "admin", "Admins", new IRI[] {HAS_ADMIN_PREDICATE}, new IRI[] {});

    private static IRI[] stringToIriArray(String string) {
        if (string == null || string.isBlank()) return new IRI[] {};
        return Stream.of(string.split(" ")).map(s -> Utils.vf.createIRI(s)).toArray(IRI[]::new);
    }

}
