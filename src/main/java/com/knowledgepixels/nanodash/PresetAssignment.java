package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A class representing the assignment of a {@link Preset} to a resource.
 *
 * <p>Mirrors {@link ViewDisplay}: an assignment is identified by the
 * {@code (preset, resource)} pair rather than by the nanopub URI, and its
 * effective activation state is resolved by aggregating all assignments for that
 * pair (latest-wins among authorized agents). A {@code gen:DeactivatedPresetAssignment}
 * lets a different authorized agent deactivate an assignment they did not create.</p>
 *
 * <p>See {@code doc/presets.md} and nanodash issue #302.</p>
 */
public class PresetAssignment implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(PresetAssignment.class);

    private String id;
    private Nanopub nanopub;
    private IRI presetIri;
    private IRI resource;
    private final Set<IRI> types = new HashSet<>();

    /**
     * Get a PresetAssignment by its ID, resolving to the latest version.
     *
     * @param id the ID of the PresetAssignment
     * @return the PresetAssignment object
     * @throws IllegalArgumentException if the nanopub is not a proper preset assignment
     */
    public static PresetAssignment get(String id) throws IllegalArgumentException {
        // Try to resolve to the latest version (same pattern as ViewDisplay.get()):
        try {
            String npId = id.replaceFirst("^(.*[^A-Za-z0-9-_]RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$1");
            String latestNpId = QueryApiAccess.getLatestVersionId(npId);
            if (!latestNpId.equals(npId)) {
                Nanopub np = Utils.getAsNanopub(latestNpId);
                if (np != null) {
                    Set<String> embeddedIris = NanopubUtils.getEmbeddedIriIds(np);
                    if (embeddedIris.size() == 1) {
                        return new PresetAssignment(embeddedIris.iterator().next(), np);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error resolving latest version for preset assignment: {}", id, ex);
        }
        // Fall back to loading the nanopub as given:
        try {
            Nanopub np = Utils.getAsNanopub(id.replaceFirst("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$2"));
            return new PresetAssignment(id, np);
        } catch (Exception ex) {
            logger.error("Couldn't load nanopub for preset assignment: {}", id, ex);
            throw new IllegalArgumentException("invalid preset assignment value " + id);
        }
    }

    private PresetAssignment(String id, Nanopub nanopub) {
        this.id = id;
        this.nanopub = nanopub;
        boolean assignmentTypeFound = false;
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().stringValue().equals(id)) continue;
            if (st.getPredicate().equals(RDF.TYPE)) {
                if (st.getObject().equals(KPXL_TERMS.PRESET_ASSIGNMENT)) {
                    assignmentTypeFound = true;
                }
                if (st.getObject() instanceof IRI objIri && !st.getObject().equals(KPXL_TERMS.PRESET_ASSIGNMENT)) {
                    types.add(objIri);
                }
            } else if (st.getPredicate().equals(KPXL_TERMS.IS_ASSIGNMENT_OF_PRESET) && st.getObject() instanceof IRI objIri) {
                if (presetIri != null) {
                    throw new IllegalArgumentException("Preset already set: " + objIri);
                }
                presetIri = objIri;
            } else if (st.getPredicate().equals(KPXL_TERMS.IS_ASSIGNMENT_FOR) && st.getObject() instanceof IRI objIri) {
                if (resource != null) {
                    throw new IllegalArgumentException("Resource already set: " + objIri);
                }
                resource = objIri;
            }
        }
        if (!assignmentTypeFound) throw new IllegalArgumentException("Not a proper preset assignment nanopub: " + id);
        if (presetIri == null) throw new IllegalArgumentException("Preset not found: " + id);
        if (resource == null) throw new IllegalArgumentException("Resource not found: " + id);
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

    public boolean hasType(IRI type) {
        return types.contains(type);
    }

    /**
     * Whether this assignment is active. An assignment is active unless it is
     * explicitly typed as {@code gen:DeactivatedPresetAssignment}.
     *
     * @return true if the assignment is active
     */
    public boolean isActive() {
        return !types.contains(KPXL_TERMS.DEACTIVATED_PRESET_ASSIGNMENT);
    }

    /**
     * Gets the IRI of the assigned preset ({@code gen:isAssignmentOfPreset}).
     *
     * @return the preset IRI
     */
    public IRI getPresetIri() {
        return presetIri;
    }

    /**
     * Resolves and returns the assigned {@link Preset}, following the supersedes
     * chain to its latest version.
     *
     * @return the resolved Preset, or null if it could not be loaded
     */
    public Preset getPreset() {
        return Preset.get(presetIri.stringValue());
    }

    /**
     * Gets the target resource of this assignment ({@code gen:isAssignmentFor}).
     *
     * @return the resource IRI
     */
    public IRI getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return id;
    }

}
