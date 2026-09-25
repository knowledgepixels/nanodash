package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.vocabulary.NPX;

import java.util.Set;

/**
 * Decides what a publication has to invalidate (issue #622).
 * <p>
 * A publication made from a view's action button names the view's query in
 * {@code refresh-upon-publish}, and that query is what has to be re-run — the view then
 * updates where it stands. The publish flow used to <em>also</em> refresh the whole
 * context resource on top of that, on every publication, which re-runs the resource's
 * view-display query and rebuilds the entire page structure around the view. That is the
 * right thing only for the publications that actually change the structure: which views a
 * resource shows, and who may see them.
 * <p>
 * A part page has one more thing to invalidate: the lookup that decides which nanopub the
 * page shows in the first place — see {@link #partDefinitionRefreshTarget}.
 */
public class PostPublishRefresh {

    private PostPublishRefresh() {
    }  // no instances allowed

    /**
     * Nanopub types (npx:hasNanopubType) that make a publication part of a resource's page
     * structure: its view displays, the presets supplying them, the resource or space
     * declaration itself, and the views being displayed.
     */
    private static final Set<IRI> STRUCTURAL_TYPES = Set.of(
            KPXL_TERMS.VIEW_DISPLAY,
            KPXL_TERMS.ACTIVATED_VIEW_DISPLAY,
            KPXL_TERMS.DEACTIVATED_VIEW_DISPLAY,
            KPXL_TERMS.TOP_LEVEL_VIEW_DISPLAY,
            KPXL_TERMS.PART_LEVEL_VIEW_DISPLAY,
            KPXL_TERMS.PRESET,
            KPXL_TERMS.PRESET_ASSIGNMENT,
            KPXL_TERMS.ACTIVATED_PRESET_ASSIGNMENT,
            KPXL_TERMS.DEACTIVATED_PRESET_ASSIGNMENT,
            KPXL_TERMS.VIEW_ENTITY,
            KPXL_TERMS.ROLE_INSTANTIATION,
            KPXL_TERMS.REVOKED_ROLE_INSTANTIATION,
            KPXL_TERMS.SPACE,
            KPXL_TERMS.MAINTAINED_RESOURCE
    );

    /**
     * Assertion predicates that make a publication part of a resource's page structure,
     * for the publications whose type alone does not say so.
     */
    private static final Set<IRI> STRUCTURAL_PREDICATES = Set.of(
            KPXL_TERMS.IS_DISPLAY_OF_VIEW,
            KPXL_TERMS.IS_DISPLAY_FOR,
            KPXL_TERMS.IS_ASSIGNMENT_OF_PRESET,
            KPXL_TERMS.IS_ASSIGNMENT_FOR,
            KPXL_TERMS.HAS_TOP_LEVEL_VIEW,
            KPXL_TERMS.HAS_VIEW,
            KPXL_TERMS.HAS_ADMIN_PREDICATE,
            KPXL_TERMS.HAS_ROLE,
            KPXL_TERMS.DETACHED_ROLE,
            KPXL_TERMS.IS_VISIBLE_TO,
            KPXL_TERMS.IS_MAINTAINED_BY,
            KPXL_TERMS.HAS_ROOT_DEFINITION,
            KPXL_TERMS.GOVERNED_BY,
            // A retraction says nothing about what it retracts, so it is taken to possibly
            // be the removal of a view display, a preset assignment or a role.
            NPX.RETRACTS
    );

    /**
     * Whether the publication can change the page structure of the given context resource:
     * the set of views the resource shows, or the roles that decide who sees them. When it
     * cannot, the publication only affects the contents of one or more views, and refreshing
     * the view queries it named is enough.
     *
     * @param np        the just-published nanopub
     * @param contextId the id of the context resource whose page is being returned to
     * @return true if the context resource's own data has to be refreshed as well
     */
    public static boolean changesPageStructure(Nanopub np, String contextId) {
        if (np == null) return false;
        for (IRI type : NanopubUtils.getTypes(np)) {
            if (STRUCTURAL_TYPES.contains(type)) return true;
        }
        Set<IRI> rolePredicates = rolePredicatesOf(contextId);
        for (Statement st : np.getAssertion()) {
            IRI predicate = st.getPredicate();
            if (STRUCTURAL_PREDICATES.contains(predicate)) return true;
            // Roles are assigned with the space's own role predicates, which are declared
            // per space rather than drawn from a fixed vocabulary, so they are matched
            // against the space being returned to.
            if (rolePredicates.contains(predicate)) return true;
        }
        return false;
    }

    /**
     * The part-definition lookup a publication invalidates, or null when it invalidates none.
     * <p>
     * A part page does not read the nanopub it shows from any of the view queries: it
     * resolves it first, through {@link ViewDataFetcher#partDefinitionQueryRef} (the newest
     * definition of the term among the members' keys), and then hands that nanopub to every
     * view as their {@code …Np} parameter. A publication that introduces the part being
     * viewed — an override of its definition, say (docs/fill-modes.md) — is a new, newer
     * definition, so it changes the answer to that lookup. Re-running the view queries
     * cannot show it: they are all keyed on the nanopub the lookup returned before. The
     * lookup itself is what has to be cleared.
     *
     * @param np        the just-published nanopub
     * @param partId    the {@code part} page parameter, i.e. the part being viewed, or empty
     * @param contextId the id of the context resource whose page is being returned to
     * @return the query ref to invalidate, as a refresh target, or null
     */
    public static String partDefinitionRefreshTarget(Nanopub np, String partId, String contextId) {
        if (np == null || partId == null || partId.isEmpty()) return null;
        if (contextId == null || contextId.isEmpty() || partId.equals(contextId)) return null;
        if (!NanopubUtils.getIntroducedIriIds(np).contains(partId)) return null;
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(contextId);
        if (resource == null) return null;
        return ViewDataFetcher.partDefinitionQueryRef(partId, contextId, resource).getAsUrlString();
    }

    /**
     * The role-assigning predicates declared by the context resource's space, in both
     * directions, or an empty set when the context is not a space (or its roles are not
     * loaded).
     *
     * @param contextId the id of the context resource
     * @return the role predicates to watch for
     */
    private static Set<IRI> rolePredicatesOf(String contextId) {
        if (contextId == null || contextId.isEmpty()) return Set.of();
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(contextId);
        if (!(resource instanceof Space space)) return Set.of();
        Set<IRI> predicates = new java.util.HashSet<>();
        for (SpaceMemberRoleRef roleRef : space.getRoles()) {
            SpaceMemberRole role = roleRef.getRole();
            if (role == null) continue;
            for (IRI p : role.getRegularProperties()) predicates.add(p);
            for (IRI p : role.getInverseProperties()) predicates.add(p);
        }
        return predicates;
    }

}
