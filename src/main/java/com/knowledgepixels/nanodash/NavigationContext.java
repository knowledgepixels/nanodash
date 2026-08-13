package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.HomePage;
import com.knowledgepixels.nanodash.page.MaintainedResourcePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.ResourcePartPage;
import com.knowledgepixels.nanodash.page.SpacePage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The navigation context: the space, user, or maintained resource a page was reached
 * under, carried across pages as the {@code context} URL parameter. It determines where
 * the user is forwarded to after publishing a nanopub and where the title bar's
 * back-link points on pages that are not themselves a context resource's page.
 */
public class NavigationContext {

    private NavigationContext() {
    }

    /**
     * Name of the page parameter holding the navigation context resource id.
     */
    public static final String CONTEXT_PARAM = "context";

    /**
     * Reads the navigation context id from the given page parameters.
     *
     * @param params the page parameters
     * @return the context resource id, or null if not set
     */
    public static String getContextId(PageParameters params) {
        if (params == null) return null;
        String contextId = params.get(CONTEXT_PARAM).toString("");
        return contextId.isEmpty() ? null : contextId;
    }

    /**
     * Resolves a context id to its space, maintained resource, or user. Agents are
     * created lazily, so a plain {@link AbstractResourceWithProfile#get(String)} lookup
     * alone would miss users not touched yet this session; hence the explicit user check.
     *
     * @param contextId the context resource id
     * @return the resolved resource, or null if the id is not a known space, maintained resource, or user
     */
    public static AbstractResourceWithProfile resolve(String contextId) {
        if (contextId == null || contextId.isEmpty()) return null;
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(contextId);
        if (resource == null && IndividualAgent.isUser(contextId)) {
            resource = IndividualAgent.get(contextId);
        }
        if (resource instanceof IndividualAgent && !IndividualAgent.isUser(contextId)) return null;
        return resource;
    }

    /**
     * The page class showing the given resource.
     *
     * @param resource the context resource
     * @return the page class, or null if the resource is null
     */
    public static Class<? extends NanodashPage> getPageClass(AbstractResourceWithProfile resource) {
        if (resource instanceof Space) return SpacePage.class;
        if (resource instanceof MaintainedResource) return MaintainedResourcePage.class;
        if (resource instanceof IndividualAgent) return UserPage.class;
        return null;
    }

    /**
     * Whether the given context id is the configured home resource, whose page is the
     * home page itself rather than its maintained-resource page.
     *
     * @param contextId the context resource id
     * @return true if it is the home resource
     */
    public static boolean isHomeResource(String contextId) {
        return contextId != null && contextId.equals(NanodashPreferences.get().getHomeResource());
    }

    /**
     * A page reference (link target + label) for the given context id.
     *
     * @param contextId the context resource id
     * @return the page reference, or null if the id cannot be resolved
     */
    public static NanodashPageRef getPageRef(String contextId) {
        if (isHomeResource(contextId)) {
            return new NanodashPageRef(HomePage.class, "Home");
        }
        AbstractResourceWithProfile resource = resolve(contextId);
        if (resource == null) return null;
        return new NanodashPageRef(getPageClass(resource), new PageParameters().set("id", contextId), resource.getLabel());
    }

    /**
     * Sets the given context id on the parameters, unless it is null or a context is
     * already set (call sites that know the context resource remain authoritative).
     *
     * @param params    the page parameters to extend
     * @param contextId the context resource id, or null for a no-op
     * @return the same page parameters, for chaining
     */
    public static PageParameters withContext(PageParameters params, String contextId) {
        if (contextId != null && !contextId.isEmpty() && params.get(CONTEXT_PARAM).isEmpty()) {
            params.set(CONTEXT_PARAM, contextId);
        }
        return params;
    }

    /**
     * A behavior that fills in the page's navigation context on a
     * {@link BookmarkablePageLink} that doesn't carry one yet. Useful where the context
     * id isn't at hand when the link is built (e.g. nanopub cards); runs at configure
     * time, when the component is attached to its page.
     *
     * @return the context-fallback behavior
     */
    public static Behavior pageContextFallback() {
        return new Behavior() {
            @Override
            public void onConfigure(Component component) {
                if (component instanceof BookmarkablePageLink<?> link && component.getPage() instanceof NanodashPage page
                        && link.getPageParameters() != null) {
                    withContext(link.getPageParameters(), page.getContextId());
                }
            }
        };
    }

    /**
     * A behavior that appends the page's navigation context to a link whose target is a
     * URL string (e.g. from {@link com.knowledgepixels.nanodash.component.NanodashLink#getPageUrl(String)})
     * rather than page parameters, where {@link #pageContextFallback()} cannot apply.
     * Only internal page URLs (starting with "/") that don't carry a context yet are touched.
     *
     * @return the context-fallback behavior
     */
    public static Behavior hrefContextFallback() {
        return new Behavior() {
            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                if (!(component.getPage() instanceof NanodashPage page)) return;
                String contextId = page.getContextId();
                if (contextId == null) return;
                String href = tag.getAttribute("href");
                if (href == null || !href.startsWith("/") || href.contains(CONTEXT_PARAM + "=")) return;
                tag.put("href", href + (href.contains("?") ? "&" : "?") + CONTEXT_PARAM + "=" + Utils.urlEncode(contextId));
            }
        };
    }

    /**
     * Delay before the repository caches may re-fetch after a publication, giving the
     * network time to ingest the just-published nanopub; matches the delay used for the
     * other post-publish refresh notifications.
     */
    private static final long INGEST_WAIT_MS = 5 * 1000;

    /**
     * Additional delay for the second resolution attempt when the introduced space or
     * maintained resource has not appeared after the first one.
     */
    private static final long INGEST_RETRY_WAIT_MS = 3 * 1000;

    /**
     * Forwards to the page of the context resource (or its part) after a successful
     * publication, with the just-published nanopub shown only in the title bar message;
     * forwards to the home page if no (resolvable) context is set. A nanopub that
     * declares a space or maintained resource forwards to that resource's own page
     * instead (issue #594). Always throws.
     *
     * @param signedNp   the just-published nanopub
     * @param pageParams the parameters of the publish form's page
     */
    public static void redirectAfterPublish(Nanopub signedNp, PageParameters pageParams) {
        redirectToDeclaredResource(signedNp, pageParams);
        String npUri = signedNp.getUri().stringValue();
        String contextId = getContextId(pageParams);
        if (contextId != null) {
            PageParameters redirectParams = new PageParameters().set("just-published", npUri);
            String partId = pageParams.get("part").toString("");
            if (!partId.isEmpty()) {
                // User was on a part page (e.g. paper collection); redirect back to the part page
                redirectParams.set("id", partId).set(CONTEXT_PARAM, contextId);
                throw new RestartResponseException(ResourcePartPage.class, redirectParams);
            }
            if (isHomeResource(contextId)) {
                throw new RestartResponseException(HomePage.class, redirectParams);
            }
            redirectParams.set("id", contextId);
            // Return to the tab the action asked for (e.g. "about" for a
            // space's About-tab role actions); default is the Content tab.
            String postpubTab = pageParams.get("postpub-tab").toString("");
            if (!postpubTab.isEmpty()) redirectParams.set("tab", postpubTab);
            AbstractResourceWithProfile resource = resolve(contextId);
            if (resource != null) {
                throw new RestartResponseException(getPageClass(resource), redirectParams);
            }
        }
        throw new RestartResponseException(HomePage.class, new PageParameters().set("just-published", npUri));
    }

    /**
     * Forwards to the page of the space or maintained resource the just-published
     * nanopub declares (newly introduced or updated), if there is exactly one and it is
     * not the context page we would forward to anyway (issue #594). The repository
     * caches are invalidated with a short delay so the network has time to ingest the
     * nanopub; the {@code findById} lookups below then block until that delay has
     * passed and fresh data is fetched, so the target page shows the new state. Returns
     * without effect — falling back to the normal context/home forward — when the
     * nanopub declares no such resource or the resource does not appear in the
     * repositories even after a second refresh round.
     * <p>
     * Detection mirrors nanopub-query's SpacesExtractor: the nanopub type set must
     * contain {@code gen:Space} (space declarations, keyed by the subjects of
     * {@code rdf:type gen:Space} and {@code gen:hasRootDefinition} assertion triples)
     * or {@code gen:MaintainedResource}/{@code gen:isMaintainedBy} (resource
     * declarations, keyed by the subjects of {@code gen:isMaintainedBy} triples) —
     * anything else would not be ingested as a space/resource, so forwarding to its
     * page would lead nowhere.
     *
     * @param signedNp   the just-published nanopub
     * @param pageParams the parameters of the publish form's page
     */
    private static void redirectToDeclaredResource(Nanopub signedNp, PageParameters pageParams) {
        // An explicitly requested post-publish redirect stays authoritative.
        if (!pageParams.get("postpub-redirect-url").isEmpty()) return;
        String spaceId = getDeclaredSpaceId(signedNp);
        boolean isSpaceNanopub = spaceId != null;
        String targetId = isSpaceNanopub ? spaceId : getDeclaredMaintainedResourceId(signedNp);
        if (targetId == null) return;
        // The normal forward already goes to this page (and the publish flow has already
        // set up its refresh), including the postpub-tab handling the declared-resource
        // forward doesn't have; the home resource's page is the home page itself.
        if (targetId.equals(getContextId(pageParams)) || isHomeResource(targetId)) return;
        if (AbstractResourceWithProfile.isResourceWithProfile(targetId)) {
            // Update of an already-known space/resource: also refresh its profile data.
            WicketApplication.get().notifyNanopubPublished(signedNp, targetId, INGEST_WAIT_MS);
        } else {
            WicketApplication.get().notifyNanopubPublished(signedNp,
                    isSpaceNanopub ? "spaces" : "maintainedResources", INGEST_WAIT_MS);
        }
        boolean found = declaredResourceExists(isSpaceNanopub, targetId);
        if (!found) {
            // Not ingested within the first delay: give it one more, shorter round.
            if (isSpaceNanopub) {
                SpaceRepository.get().forceRootRefresh(INGEST_RETRY_WAIT_MS);
            } else {
                MaintainedResourceRepository.get().forceRootRefresh(INGEST_RETRY_WAIT_MS);
            }
            found = declaredResourceExists(isSpaceNanopub, targetId);
        }
        if (!found) return;
        PageParameters redirectParams = new PageParameters()
                .set("id", targetId)
                .set("just-published", signedNp.getUri().stringValue());
        Class<? extends NanodashPage> pageClass = isSpaceNanopub ? SpacePage.class : MaintainedResourcePage.class;
        throw new RestartResponseException(pageClass, redirectParams);
    }

    private static boolean declaredResourceExists(boolean isSpaceNanopub, String targetId) {
        if (isSpaceNanopub) return SpaceRepository.get().findById(targetId) != null;
        return MaintainedResourceRepository.get().findById(targetId) != null;
    }

    /**
     * The id of the space the given nanopub declares: the subject of its
     * {@code rdf:type gen:Space} and {@code gen:hasRootDefinition} assertion triples,
     * provided the nanopub is typed {@code gen:Space} (otherwise nanopub-query would
     * not ingest it as a space declaration).
     *
     * @param np the nanopub to inspect
     * @return the declared space id, or null if the nanopub declares no space or more than one
     */
    static String getDeclaredSpaceId(Nanopub np) {
        if (!Utils.isNanopubOfClass(np, KPXL_TERMS.SPACE)) return null;
        Set<String> ids = new LinkedHashSet<>();
        for (Statement st : np.getAssertion()) {
            if (!(st.getSubject() instanceof IRI subj)) continue;
            if ((RDF.TYPE.equals(st.getPredicate()) && KPXL_TERMS.SPACE.equals(st.getObject()))
                    || KPXL_TERMS.HAS_ROOT_DEFINITION.equals(st.getPredicate())) {
                ids.add(subj.stringValue());
            }
        }
        return ids.size() == 1 ? ids.iterator().next() : null;
    }

    /**
     * The id of the maintained resource the given nanopub declares: the subject of its
     * {@code gen:isMaintainedBy} assertion triple, provided the nanopub is typed
     * {@code gen:MaintainedResource} or {@code gen:isMaintainedBy} (the two shapes
     * nanopub-query ingests as maintained-resource declarations).
     *
     * @param np the nanopub to inspect
     * @return the declared resource id, or null if the nanopub declares no maintained resource or more than one
     */
    static String getDeclaredMaintainedResourceId(Nanopub np) {
        if (!Utils.isNanopubOfClass(np, KPXL_TERMS.MAINTAINED_RESOURCE)
                && !Utils.isNanopubOfClass(np, KPXL_TERMS.IS_MAINTAINED_BY)) return null;
        Set<String> ids = new LinkedHashSet<>();
        for (Statement st : np.getAssertion()) {
            if (st.getSubject() instanceof IRI subj && KPXL_TERMS.IS_MAINTAINED_BY.equals(st.getPredicate())) {
                ids.add(subj.stringValue());
            }
        }
        return ids.size() == 1 ? ids.iterator().next() : null;
    }

}
