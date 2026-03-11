package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MaintainedResource extends AbstractResourceWithProfile {

    private static final Logger logger = LoggerFactory.getLogger(MaintainedResource.class);

    /**
     * Get the namespace from a string or IRI by removing the last segment after a slash or hash.
     *
     * @param stringOrIri A string or IRI representing the namespace to search for.
     * @return The namespace extracted from the input string or IRI, or null if the input is null.
     */
    public static String getNamespace(Object stringOrIri) {
        return stringOrIri.toString().replaceFirst("([#/])[^#/]+$", "$1");
    }

    private String label, nanopubId, namespace;
    private Nanopub nanopub;

    /**
     * Constructor for MaintainedResource, initializing the object based on the provided API response entry and associated space.
     *
     * @param resp  the API response entry containing the data for this maintained resource
     * @param space the space associated with this maintained resource
     */
    MaintainedResource(ApiResponseEntry resp, Space space) {
        super(resp.get("resource"));
        initialize(resp, space);
    }

    private void initialize(ApiResponseEntry resp, Space space) {
        initSpace(space);
        this.label = resp.get("label");
        this.nanopubId = resp.get("np");
        this.namespace = resp.get("namespace");
        if (namespace != null && namespace.isBlank()) {
            namespace = null;
        }
        this.nanopub = Utils.getAsNanopub(nanopubId);
    }

    /**
     * Get the ID of the nanopub that defines this maintained resource.
     *
     * @return the nanopub ID
     */
    public String getNanopubId() {
        return nanopubId;
    }

    @Override
    public Nanopub getNanopub() {
        return nanopub;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

}
