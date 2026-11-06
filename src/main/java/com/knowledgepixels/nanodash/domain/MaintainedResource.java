package com.knowledgepixels.nanodash.domain;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
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

    private String label, namespace;
    private volatile String nanopubId;

    // The declaring nanopub, fetched on first use rather than when the resource is built
    // (issue #892). Building runs under the repository's lock for every resource at once,
    // and one registry fetch per resource there kept every page waiting for ~10 minutes
    // after a restart. Kept together with the id it was fetched for, so a nanopub fetched
    // for an earlier version of the resource is never handed out for a newer one.
    private transient volatile LoadedNanopub loadedNanopub;

    private record LoadedNanopub(String id, Nanopub nanopub) {
    }

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
        if (label == null || label.isBlank()) {
            // No label in the data at all: show the short form of the IRI rather than "null".
            label = Utils.getShortNameFromURI(getId());
        }
        this.nanopubId = resp.get("np");
        this.namespace = resp.get("namespace");
        if (namespace != null && namespace.isBlank()) {
            namespace = null;
        }
    }

    void updateFromApi(ApiResponseEntry resp, Space space) {
        String newNpId = resp.get("np");
        if (!newNpId.equals(this.nanopubId)) {
            initialize(resp, space);
            setDataNeedsUpdate();
        }
    }

    /**
     * Get the ID of the nanopub that defines this maintained resource.
     *
     * @return the nanopub ID
     */
    public String getNanopubId() {
        return nanopubId;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Fetched from a registry on the first call (and again after a failed fetch), then kept.
     * The repository loads it in the background once a resource is known, so a page
     * normally finds it here already.
     */
    @Override
    public Nanopub getNanopub() {
        String id = nanopubId;
        LoadedNanopub loaded = loadedNanopub;
        if (loaded != null && loaded.id().equals(id)) return loaded.nanopub();
        Nanopub np = Utils.getAsNanopub(id);
        if (np != null) loadedNanopub = new LoadedNanopub(id, np);
        return np;
    }

    /**
     * Whether {@link #getNanopub()} would answer without going to a registry.
     *
     * @return true if the declaring nanopub of the current version is loaded
     */
    public boolean isNanopubLoaded() {
        LoadedNanopub loaded = loadedNanopub;
        return loaded != null && loaded.id().equals(nanopubId);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    protected Set<IRI> getOwnClasses() {
        return Set.of(KPXL_TERMS.MAINTAINED_RESOURCE);
    }

}
