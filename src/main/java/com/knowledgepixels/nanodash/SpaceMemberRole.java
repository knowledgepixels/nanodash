package com.knowledgepixels.nanodash;

import java.io.Serializable;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponseEntry;

import com.google.common.collect.Multimap;

public class SpaceMemberRole implements Serializable {

    private IRI mainProperty;
    private String name;
    private boolean isInverse;
    private IRI[] equivalentProperties, inverseProperties;

    public SpaceMemberRole(ApiResponseEntry e) {
        this.mainProperty = Utils.vf.createIRI(e.get("role"));
        this.name = e.get("roleName");
        this.isInverse = "true".equals(e.get("isInverse"));
        equivalentProperties = stringToIriArray(e.get("equivalent"));
        inverseProperties = stringToIriArray(e.get("inverse"));
    }

    public SpaceMemberRole(IRI mainProperty, String name, boolean isInverse, IRI[] equivalentProperties, IRI[] inverseProperties) {
        this.mainProperty = mainProperty;
        this.name = name;
        this.isInverse = isInverse;
        this.equivalentProperties = equivalentProperties;
        this.inverseProperties = inverseProperties;
    }

    public IRI getMainProperty() {
        return mainProperty;
    }

    public String getName() {
        return name;
    }

    public boolean isInverse() {
        return isInverse;
    }

    public IRI[] getEquivalentProperties() {
        return equivalentProperties;
    }

    public IRI[] getInverseProperties() {
        return inverseProperties;
    }

    public void addRoleParams(Multimap<String, String> params) {
        if (isInverse) {
            params.put("invrole", mainProperty.stringValue());
            for (IRI p : equivalentProperties) params.put("invrole", p.stringValue());
            for (IRI p : inverseProperties) params.put("role", p.stringValue());
        } else {
            params.put("role", mainProperty.stringValue());
            for (IRI p : equivalentProperties) params.put("role", p.stringValue());
            for (IRI p : inverseProperties) params.put("invrole", p.stringValue());
        }
    }

    public static final IRI ADMIN_ROLE_IRI = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin");
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "admin", true, new IRI[] {}, new IRI[] {});

    private static IRI[] stringToIriArray(String string) {
        if (string == null || string.isBlank()) return new IRI[] {};
        return Stream.of(string.split(" ")).map(s -> Utils.vf.createIRI(s)).toArray(IRI[]::new);
    }

}
