package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a Preset: a named bundle of default views and roles that
 * can be assigned to a resource (a user, a space, or a maintained resource).
 *
 * <p>This mirrors {@link View}: a preset carries a stable <em>kind</em> (via
 * {@code dct:isVersionOf}) so its identity survives across superseding versions,
 * and {@link #get(String)} automatically resolves to the latest version.</p>
 *
 * <p>See {@code doc/presets.md} and nanodash issue #302.</p>
 */
public class Preset implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Preset.class);

    private static final Cache<String, Preset> presets = CacheBuilder.newBuilder()
        .maximumSize(5_000)
        .expireAfterAccess(24, TimeUnit.HOURS)
        .build();

    /**
     * Get a Preset by its ID, resolving to the latest version of its kind. A version that
     * declares {@code gen:governedBy} resolves within its {@code (kind, space)} pair via
     * {@link GovernedVersions}; one that doesn't follows its supersedes chain, and on into a
     * space's versions if the chain ends at a version handing the preset over to that space.
     *
     * @param id the ID of the Preset
     * @return the Preset object, or null if it could not be loaded
     */
    public static Preset get(String id) {
        String npId = toNanopubId(id);
        Preset pinned = getExactVersion(id, npId);
        Preset latest = resolveLatestVersion(id, npId, pinned);
        return latest != null ? latest : pinned;
    }

    /**
     * Whether a preset declares space-based governance, i.e. both a version-independent
     * IRI ({@code dct:isVersionOf}) and a governing space ({@code gen:governedBy}).
     *
     * @param preset the preset to check, may be null
     * @return true if the preset declares both
     */
    public static boolean isSpaceGoverned(Preset preset) {
        return preset != null && preset.getGoverningSpace() != null && preset.getPresetKindIri() != null;
    }

    /**
     * The id of the nanopub a preset id belongs to: the preset id up to and including its
     * artifact code. An id that is already a nanopub id is returned unchanged.
     *
     * @param presetId the preset id
     * @return the nanopub id
     */
    private static String toNanopubId(String presetId) {
        return presetId.replaceFirst("^(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
    }

    /**
     * Resolves a preset to its current version: within its {@code (kind, space)} pair if it
     * declares {@code gen:governedBy} and the space maintains the kind, along its own
     * supersedes chain otherwise, and on into a space's versions where that chain ends at a
     * version handing the preset over to it (see {@link GovernedVersions#resolve(Nanopub)}).
     * The pin is the floor.
     *
     * @param id     the preset id
     * @param npId   the nanopub id of the preset
     * @param pinned the pinned preset, may be null if it could not be loaded
     * @return the resolved preset, or null if resolution stays at the pin, yields nothing that
     * can be loaded, or fails
     */
    private static Preset resolveLatestVersion(String id, String npId, Preset pinned) {
        try {
            GovernedVersions.Resolution resolution = (pinned != null && pinned.getNanopub() != null)
                    ? GovernedVersions.resolve(pinned.getNanopub())
                    : new GovernedVersions.Resolution(QueryApiAccess.getLatestVersionId(npId), null);
            if (resolution.staysAt(npId)) {
                return null;
            }
            if (resolution.versionIri() != null) {
                return getExactVersion(resolution.versionIri(), toNanopubId(resolution.versionIri()));
            }
            Nanopub np = Utils.getAsNanopub(resolution.nanopubId());
            if (np == null) {
                return null;
            }
            Set<String> embeddedIris = NanopubUtils.getEmbeddedIriIds(np);
            if (embeddedIris.size() != 1) {
                return null;
            }
            String latestId = embeddedIris.iterator().next();
            Preset cached = presets.getIfPresent(latestId);
            if (cached == null) {
                cached = new Preset(latestId, np);
                presets.put(latestId, cached);
            }
            return cached;
        } catch (Exception ex) {
            logger.error("Error resolving latest version for preset: {}", id, ex);
            return null;
        }
    }

    /**
     * Loads the preset exactly as given, without a latest-version lookup.
     *
     * @param id   the preset id
     * @param npId the nanopub id of the preset
     * @return the preset, or null if it could not be loaded
     */
    private static Preset getExactVersion(String id, String npId) {
        Preset cached = presets.getIfPresent(id);
        if (cached == null) {
            try {
                cached = new Preset(id, Utils.getAsNanopub(npId));
                presets.put(id, cached);
            } catch (Exception ex) {
                logger.error("Couldn't load nanopub for preset: {}", id, ex);
            }
        }
        return cached;
    }

    private String id;
    private Nanopub nanopub;
    private IRI presetKind;
    private IRI governingSpace;
    private String label;
    private String description;
    private final List<IRI> topLevelViews = new ArrayList<>();
    private final List<IRI> views = new ArrayList<>();
    private final List<IRI> roles = new ArrayList<>();
    private final Set<IRI> appliesToClasses = new HashSet<>();
    private final Set<IRI> appliesToNamespaces = new HashSet<>();

    private Preset(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;
        boolean presetTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().stringValue().equals(id)) continue;
            if (st.getPredicate().equals(RDF.TYPE)) {
                if (st.getObject().equals(KPXL_TERMS.PRESET)) {
                    presetTypeFound = true;
                }
            } else if (st.getPredicate().equals(DCTERMS.IS_VERSION_OF) && st.getObject() instanceof IRI objIri) {
                presetKind = objIri;
            } else if (st.getPredicate().equals(KPXL_TERMS.GOVERNED_BY) && st.getObject() instanceof IRI objIri) {
                governingSpace = objIri;
            } else if (st.getPredicate().equals(RDFS.LABEL)) {
                label = st.getObject().stringValue();
            } else if (st.getPredicate().equals(DCTERMS.DESCRIPTION)) {
                description = st.getObject().stringValue();
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_TOP_LEVEL_VIEW) && st.getObject() instanceof IRI objIri) {
                topLevelViews.add(objIri);
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_VIEW) && st.getObject() instanceof IRI objIri) {
                views.add(objIri);
            } else if (st.getPredicate().equals(KPXL_TERMS.HAS_ROLE) && st.getObject() instanceof IRI objIri) {
                roles.add(objIri);
            } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_INSTANCES_OF) && st.getObject() instanceof IRI objIri) {
                appliesToClasses.add(objIri);
            } else if (st.getPredicate().equals(KPXL_TERMS.APPLIES_TO_NAMESPACE) && st.getObject() instanceof IRI objIri) {
                appliesToNamespaces.add(objIri);
            }
        }
        if (!presetTypeFound) throw new IllegalArgumentException("Not a proper preset nanopub: " + id);
    }

    public String getId() {
        return id;
    }

    public Nanopub getNanopub() {
        return nanopub;
    }

    public IRI getNanopubId() {
        return nanopub == null ? null : nanopub.getUri();
    }

    /**
     * Gets the stable preset kind (the {@code dct:isVersionOf} target), which is
     * version-independent and what assignments and lookups should reference.
     *
     * @return the preset kind IRI, or null if not set
     */
    public IRI getPresetKindIri() {
        return presetKind;
    }

    /**
     * Gets the space governing this preset's versions ({@code gen:governedBy}).
     *
     * @return the governing space IRI, or null if the preset is not space-governed
     */
    public IRI getGoverningSpace() {
        return governingSpace;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the views to be shown at the top level of the resource page
     * ({@code gen:hasTopLevelView}).
     *
     * @return the list of top-level view IRIs
     */
    public List<IRI> getTopLevelViews() {
        return topLevelViews;
    }

    /**
     * Gets the views to be shown by default ({@code gen:hasView}).
     *
     * @return the list of default view IRIs
     */
    public List<IRI> getViews() {
        return views;
    }

    /**
     * Gets the role definitions bundled by this preset ({@code gen:hasRole}).
     *
     * @return the list of role IRIs
     */
    public List<IRI> getRoles() {
        return roles;
    }

    /**
     * Checks whether this preset applies to the given resource, by namespace or
     * by class. Mirrors {@link View#appliesTo(String, Set)}.
     *
     * @param resourceId the resource ID
     * @param classes    the classes the resource is an instance of
     * @return true if the preset applies
     */
    public boolean appliesTo(String resourceId, Set<IRI> classes) {
        for (IRI namespace : appliesToNamespaces) {
            if (resourceId.startsWith(namespace.stringValue())) return true;
        }
        if (classes != null) {
            for (IRI c : classes) {
                if (appliesToClasses.contains(c)) return true;
            }
        }
        return false;
    }

    public boolean appliesToClass(IRI targetClass) {
        return appliesToClasses.contains(targetClass);
    }

    @Override
    public String toString() {
        return id;
    }

}
