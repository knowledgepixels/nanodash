package com.knowledgepixels.nanodash;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.extra.services.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Synchronous access to a resource's view displays and their query results,
 * bypassing the pages' async resource state. Used by the download pages to
 * reconstruct a page's content server-side.
 */
public final class ViewDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ViewDataFetcher.class);

    private ViewDataFetcher() {
    }

    /**
     * Retrieves a query response, retrying while another thread is fetching the same query.
     * Returns null only if the query genuinely has no cached result and no fetch is in progress.
     *
     * @param queryRef the query reference
     * @return the response, or null
     */
    public static ApiResponse retrieveResponseWithWait(QueryRef queryRef) {
        int waited = 0;
        while (waited < 30_000) {
            ApiResponse response = ApiCache.retrieveResponseSync(queryRef, false);
            if (response != null) return response;
            if (!ApiCache.isRunning(queryRef)) return null;
            try {
                Thread.sleep(200);
                waited += 200;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /**
     * Builds a QueryRef for a view display, mirroring the parameter logic from ViewList.
     *
     * @param vd         the view display
     * @param resource   the resource the display is shown on
     * @param targetId   the target resource (or part) IRI
     * @param targetNpId the target's nanopub ID, or null
     * @return the query reference, or null if the view has no fillable query
     */
    public static QueryRef buildQueryRef(ViewDisplay vd, AbstractResourceWithProfile resource, String targetId, String targetNpId) {
        View view = vd.getView();
        if (view == null || view.getQuery() == null) return null;

        Multimap<String, String> queryRefParams = ArrayListMultimap.create();
        for (String p : view.getQuery().getPlaceholdersList()) {
            String paramName = QueryTemplate.getParamName(p);
            if (paramName.equals(view.getQueryField())) {
                queryRefParams.put(view.getQueryField(), targetId);
                if (QueryTemplate.isMultiPlaceholder(p) && resource instanceof Space space) {
                    for (String altId : space.getAltIDs()) {
                        queryRefParams.put(view.getQueryField(), altId);
                    }
                }
            } else if (paramName.equals(view.getQueryField() + "Namespace") && resource.getNamespace() != null) {
                queryRefParams.put(view.getQueryField() + "Namespace", resource.getNamespace());
            } else if (paramName.equals(view.getQueryField() + "Np")) {
                if (!QueryTemplate.isOptionalPlaceholder(p) && targetNpId == null) {
                    queryRefParams.put(view.getQueryField() + "Np", "x:");
                } else {
                    queryRefParams.put(view.getQueryField() + "Np", targetNpId);
                }
            } else if (paramName.equals("context")) {
                // Auto-fill the page's context, as ViewList does for content-tab views.
                queryRefParams.put("context", resource.getId());
            } else if (paramName.equals("root_np")) {
                // No pinned ref in download context; left empty as ViewList does when no ref is known.
            } else if (!QueryTemplate.isOptionalPlaceholder(p)) {
                logger.error("Query has non-optional parameter that cannot be filled: {} {}", view.getQuery().getQueryId(), p);
                return null;
            }
        }
        return new QueryRef(view.getQuery().getQueryId(), queryRefParams);
    }

    /**
     * Resolves the context resource for a part page (same logic as ResourcePartPage).
     *
     * @param contextId the context resource IRI
     * @return the resource, space, or user
     */
    public static AbstractResourceWithProfile resolveContextResource(String contextId) {
        AbstractResourceWithProfile resource = MaintainedResourceRepository.get().findById(contextId);
        if (resource != null) return resource;

        if (SpaceRepository.get().findById(contextId) != null) {
            return SpaceRepository.get().findById(contextId);
        }
        if (IndividualAgent.isUser(contextId)) {
            return IndividualAgent.get(contextId);
        }
        throw new IllegalArgumentException("Not a resource, space, or user: " + contextId);
    }

    /**
     * Resolves the classes of a part (mirrors ResourcePartPage logic).
     *
     * @param partId    the part IRI
     * @param contextId the context resource IRI
     * @param resource  the resolved context resource
     * @return the part's classes (possibly empty)
     */
    public static Set<IRI> resolvePartClasses(String partId, String contextId, AbstractResourceWithProfile resource) {
        Set<IRI> classes = new HashSet<>();
        String nanopubId = resolvePartNanopubId(partId, contextId, resource);
        if (nanopubId != null) {
            Nanopub nanopub = Utils.getAsNanopub(nanopubId);
            if (nanopub != null) {
                for (Statement st : nanopub.getAssertion()) {
                    if (st.getSubject().stringValue().equals(partId) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                        classes.add(objIri);
                    }
                }
            }
        }
        return classes;
    }

    /**
     * Resolves the nanopub ref for a part (used as query param), returning "x:" if not found.
     *
     * @param partId    the part IRI
     * @param contextId the context resource IRI
     * @param resource  the resolved context resource
     * @return the nanopub ID, or "x:"
     */
    public static String resolvePartNanopubRef(String partId, String contextId, AbstractResourceWithProfile resource) {
        String npId = resolvePartNanopubId(partId, contextId, resource);
        return npId != null ? npId : "x:";
    }

    /**
     * Looks up the nanopub ID for a part's term definition (mirrors ResourcePartPage logic).
     *
     * @param partId    the part IRI
     * @param contextId the context resource IRI
     * @param resource  the resolved context resource
     * @return the nanopub ID, or null
     */
    public static String resolvePartNanopubId(String partId, String contextId, AbstractResourceWithProfile resource) {
        QueryRef getDefQuery = new QueryRef(QueryApiAccess.GET_TERM_DEFINITIONS, "term", partId);
        if (resource.getSpace() != null) {
            for (IRI userIri : resource.getSpace().getUsers()) {
                for (String pubkey : User.getUserData().getPubkeyHashes(userIri, true)) {
                    getDefQuery.getParams().put("pubkey", pubkey);
                }
            }
        } else {
            for (String pubkey : User.getUserData().getPubkeyHashes(Utils.vf.createIRI(contextId), true)) {
                getDefQuery.getParams().put("pubkey", pubkey);
            }
        }
        ApiResponse resp = ApiCache.retrieveResponseSync(getDefQuery, false);
        if (resp != null && !resp.getData().isEmpty()) {
            return resp.getData().iterator().next().get("np");
        }
        return null;
    }

}
