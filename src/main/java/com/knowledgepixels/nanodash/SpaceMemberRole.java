package com.knowledgepixels.nanodash;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;

public class SpaceMemberRole implements Serializable {

    private IRI property;
    private String name;
    
    public SpaceMemberRole(IRI property, String name) {
        this.property = property;
        this.name = name;
    }

    public IRI getProperty() {
        return property;
    }

    public String getName() {
        return name;
    }

    public static final IRI ADMIN_ROLE_IRI = Utils.vf.createIRI("https://w3id.org/kpxl/gen/terms/hasAdmin");
    public static final SpaceMemberRole ADMIN_ROLE = new SpaceMemberRole(ADMIN_ROLE_IRI, "admin");

}
