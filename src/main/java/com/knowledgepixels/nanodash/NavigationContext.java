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
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

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
     * Forwards to the page of the context resource (or its part) after a successful
     * publication, with the just-published nanopub shown only in the title bar message;
     * forwards to the home page if no (resolvable) context is set. Always throws.
     *
     * @param signedNp   the just-published nanopub
     * @param pageParams the parameters of the publish form's page
     */
    public static void redirectAfterPublish(Nanopub signedNp, PageParameters pageParams) {
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

}
