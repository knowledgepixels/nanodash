package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The space-/namespace-dependent placeholders a template can use in an
 * {@code nt:hasPrefix} value, so a template can mint resources under whichever space or
 * maintained resource the form was opened from instead of a namespace hard-coded by the
 * template author (see issue #571 and docs/space-namespace-prefixes.md):
 *
 * <pre>
 * nt:hasPrefix "~~SPACE~~/"      → https://w3id.org/space/foo/bar/
 * nt:hasPrefix "~~SPACE~~/r/"    → https://w3id.org/space/foo/bar/r/
 * nt:hasPrefix "~~NAMESPACE~~"   → the current maintained resource's namespace
 * </pre>
 * <p>
 * The base is taken from the navigation context (the {@code context} URL parameter); when
 * that yields nothing, the form offers the user a choice among the known spaces /
 * maintained resources instead.
 */
public class DynamicPrefix {

    private DynamicPrefix() {
    }

    /**
     * Placeholder standing for the IRI of the currently applicable space (no trailing
     * separator; templates write {@code "~~SPACE~~/"}).
     */
    public static final String SPACE_TOKEN = "~~SPACE~~";

    /**
     * Placeholder standing for the namespace of the current maintained resource
     * (including its trailing separator).
     */
    public static final String NAMESPACE_TOKEN = "~~NAMESPACE~~";

    /**
     * Returns the dynamic placeholder used in the given raw {@code nt:hasPrefix} value.
     *
     * @param rawPrefix the prefix as declared by the template, may be null
     * @return {@link #SPACE_TOKEN}, {@link #NAMESPACE_TOKEN}, or null if the prefix is static
     */
    public static String getToken(String rawPrefix) {
        if (rawPrefix == null) return null;
        if (rawPrefix.contains(SPACE_TOKEN)) return SPACE_TOKEN;
        if (rawPrefix.contains(NAMESPACE_TOKEN)) return NAMESPACE_TOKEN;
        return null;
    }

    /**
     * Resolves the base a token stands for from the navigation context.
     *
     * @param token     the token, as returned by {@link #getToken(String)}
     * @param contextId the navigation context resource id (the {@code context} URL parameter)
     * @return the base to substitute for the token, or null if the context doesn't determine one
     */
    public static String resolveFromContext(String token, String contextId) {
        AbstractResourceWithProfile resource = NavigationContext.resolve(contextId);
        if (resource == null) return null;
        if (SPACE_TOKEN.equals(token)) {
            // A maintained resource resolves to the space maintaining it; a space to itself.
            Space space = resource.getSpace();
            return space == null ? null : space.getId();
        }
        if (NAMESPACE_TOKEN.equals(token)) {
            if (resource instanceof MaintainedResource maintainedResource) {
                return getNamespace(maintainedResource);
            }
            return null;
        }
        return null;
    }

    /**
     * The namespace to mint sub-resources of the given maintained resource under: its
     * declared namespace, or its own IRI plus a separator if it declares none.
     *
     * @param resource the maintained resource
     * @return the namespace, ending in a separator
     */
    public static String getNamespace(MaintainedResource resource) {
        String namespace = resource.getNamespace();
        if (namespace != null && !namespace.isBlank()) return namespace;
        return resource.getId() + "/";
    }

    /**
     * The bases the user can pick from when the navigation context doesn't determine one,
     * mapped to their display labels and ordered by label.
     *
     * @param token the token, as returned by {@link #getToken(String)}
     * @return the selectable bases, mapped to their labels; empty if the token is unknown
     */
    public static Map<String, String> getOptions(String token) {
        Map<String, String> options = new LinkedHashMap<>();
        if (SPACE_TOKEN.equals(token)) {
            List<Space> spaces = new ArrayList<>(SpaceRepository.get().findAll());
            spaces.sort(Comparator.comparing(DynamicPrefix::getLabel, String.CASE_INSENSITIVE_ORDER));
            for (Space space : spaces) {
                options.putIfAbsent(space.getId(), getLabel(space));
            }
        } else if (NAMESPACE_TOKEN.equals(token)) {
            List<MaintainedResource> resources = new ArrayList<>(MaintainedResourceRepository.get().findAll());
            resources.sort(Comparator.comparing(DynamicPrefix::getLabel, String.CASE_INSENSITIVE_ORDER));
            for (MaintainedResource resource : resources) {
                options.putIfAbsent(getNamespace(resource), getLabel(resource));
            }
        }
        return options;
    }

    /**
     * The name of what the user is picking, used as the selector's placeholder text and
     * as its label in validation messages.
     *
     * @param token the token, as returned by {@link #getToken(String)}
     * @return the human-readable name of the selection
     */
    public static String getSelectionLabel(String token) {
        if (NAMESPACE_TOKEN.equals(token)) return "resource";
        return "space";
    }

    private static String getLabel(AbstractResourceWithProfile resource) {
        String label = resource.getLabel();
        if (label == null || label.isBlank()) return resource.getId();
        return label;
    }

}
