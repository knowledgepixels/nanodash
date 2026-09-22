package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NavigationContext;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.ViewDataFetcher;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * This class represents a page for a resource part in the context of a maintained resource, space, or user.
 */
public class ResourcePartPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/part";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    // getContextId() is inherited: the context param holds the maintaining resource.
    @Override
    public boolean isContextPage() {
        return true;
    }

    /**
     * This page's own resource is the part links out of it should point back to
     * (issue #697), not the maintaining resource the {@code context} param names.
     */
    @Override
    public String getPartId() {
        return getPageParameters().get("id").toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPartLabel() {
        return partLabel;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The nanopublication defining the part declares it; a part without one has nothing
     * to embed, but its download still lists what the views show about it.
     */
    @Override
    protected RdfSource getRdfSource() {
        Nanopub definition = Utils.getAsNanopub(definitionNanopubId);
        List<Nanopub> declarations = definition == null ? List.of() : List.of(definition);
        return new RdfSource("part", getPartId(), getPageParameters().get("context").toString(), declarations);
    }

    /**
     * Resource with profile (Space or MaintainedResource) object with the data shown on this page.
     */
    private AbstractResourceWithProfile resourceWithProfile;

    /**
     * The nanopublication defining this part, or null when none is known.
     */
    private String definitionNanopubId;

    /**
     * The part's label as resolved for the title, handed to links out of this page so
     * their back-link can name the part.
     */
    private String partLabel;

    /**
     * If the {@code id} in the given parameters falls under a namespace declared by a
     * maintained resource, forward to this page with that resource set as the
     * {@code context}. Does nothing if no maintained resource declares the namespace.
     *
     * @param parameters page parameters containing the {@code id} to resolve
     * @throws RestartResponseException if a containing maintained resource is found
     */
    public static void forwardToContainingResource(PageParameters parameters) {
        String id = parameters.get("id").toString();
        MaintainedResource containingResource = MaintainedResourceRepository.get().findByNamespace(MaintainedResource.getNamespace(id));
        if (containingResource != null) {
            PageParameters partParameters = new PageParameters(parameters);
            partParameters.set("context", containingResource.getId());
            throw new RestartResponseException(ResourcePartPage.class, partParameters);
        }
    }

    /**
     * Whether the given predicate is schema.org's {@code title}, in either of its
     * spellings.
     */
    static boolean isSchemaTitle(IRI predicate) {
        String p = predicate.stringValue();
        return p.equals("http://schema.org/title") || p.equals("https://schema.org/title");
    }

    /**
     * Whether the given predicate names what a part belongs to: {@code dct:isPartOf}, or
     * schema.org's {@code about} in either of its spellings, which parts published before
     * the hierarchy was stated with {@code dct:isPartOf} use instead.
     */
    static boolean isParentPredicate(IRI predicate) {
        return predicate.equals(DCTERMS.IS_PART_OF) || isSchemaAbout(predicate);
    }

    /**
     * Whether the given predicate is schema.org's {@code about}, in either of its spellings.
     */
    private static boolean isSchemaAbout(IRI predicate) {
        String p = predicate.stringValue();
        return p.equals("http://schema.org/about") || p.equals("https://schema.org/about");
    }

    /**
     * The label the given nanopublication declares for a resource: its {@code rdfs:label},
     * else its {@code schema:title} (issue #701).
     *
     * @param nanopub    the nanopublication defining the resource
     * @param resourceId the resource whose label to look up
     * @return the declared label, or null if the nanopublication declares none
     */
    static String getDeclaredLabel(Nanopub nanopub, String resourceId) {
        String schemaTitle = null;
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().stringValue().equals(resourceId)) {
                continue;
            }
            if (st.getPredicate().equals(RDFS.LABEL)) {
                return st.getObject().stringValue();
            }
            if (schemaTitle == null && isSchemaTitle(st.getPredicate()) && !st.getObject().stringValue().isBlank()) {
                schemaTitle = st.getObject().stringValue();
            }
        }
        return schemaTitle;
    }

    /**
     * The resources a part's defining nanopublication declares it to be about or part of,
     * in the order they are stated, leaving out the part itself and the context it is
     * shown under. These are only candidates: whether one is itself a part of the context
     * has to be checked separately.
     *
     * @param nanopub   the nanopublication defining the part
     * @param partId    the part resource id
     * @param contextId the context resource id the part is shown under
     * @return the candidate parent ids, possibly empty
     */
    static List<String> getDeclaredParentCandidates(Nanopub nanopub, String partId, String contextId) {
        Set<String> declared = new LinkedHashSet<>();
        Set<String> aboutOnly = new LinkedHashSet<>();
        for (Statement st : nanopub.getAssertion()) {
            if (!st.getSubject().stringValue().equals(partId) || !isParentPredicate(st.getPredicate())
                    || !(st.getObject() instanceof IRI parent)) {
                continue;
            }
            String parentId = parent.stringValue();
            if (parentId.equals(partId) || parentId.equals(contextId)) {
                continue;
            }
            (isSchemaAbout(st.getPredicate()) ? aboutOnly : declared).add(parentId);
        }
        declared.addAll(aboutOnly);
        return new ArrayList<>(declared);
    }

    /**
     * The most ancestors a part page's breadcrumb shows. The hierarchy is followed as deep
     * as it goes, but parts of parts need not form a tree, so a bound keeps a long or
     * malformed chain from making the page resolve parts without end.
     */
    static final int MAX_ANCESTORS = 20;

    /**
     * A part resolved while walking up its ancestors: its id, the label to show for it,
     * and the nanopublication defining it, if one was found.
     *
     * @param id         the part resource id
     * @param label      the label to show, or null to fall back to the part's short name
     * @param definition the nanopublication defining the part, or null if none was found
     */
    record AncestorPart(String id, String label, Nanopub definition) {
    }

    /**
     * The breadcrumbs between the context and this page's part, top-down (issue #718): the
     * hierarchy the parts themselves declare, not the path the reader took to get here, so
     * a part reads the same wherever it was opened from. It starts at the part this one
     * declares to belong to and follows each part's own declared parent upwards, as deep as
     * the hierarchy goes, stopping when a part declares none, when a part would repeat
     * (parts of parts need not form a tree), or at {@link #MAX_ANCESTORS}.
     *
     * @param definition      the nanopublication defining this page's part, or null if none is known
     * @param partId          this page's part resource id
     * @param contextId       the context resource id this page is shown under
     * @param partDefinitions returns the nanopublication defining a resource as a part of the context, or null if it is none
     * @return the ancestors' page references, top-down, possibly empty
     */
    static List<NanodashPageRef> getAncestorRefs(Nanopub definition, String partId, String contextId,
                                                 Function<String, Nanopub> partDefinitions) {
        Set<String> visited = new HashSet<>(List.of(partId, contextId));
        List<NanodashPageRef> ancestors = new ArrayList<>();
        AncestorPart ancestor = getDeclaredParent(definition, partId, contextId, visited, partDefinitions);
        while (ancestor != null && ancestors.size() < MAX_ANCESTORS) {
            visited.add(ancestor.id());
            ancestors.add(0, NavigationContext.getPartPageRef(ancestor.id(), ancestor.label(), contextId));
            ancestor = getDeclaredParent(ancestor.definition(), ancestor.id(), contextId, visited, partDefinitions);
        }
        return ancestors;
    }

    /**
     * The first part the given part declares to belong to (see
     * {@link #getDeclaredParentCandidates}) that is itself a part of the context and not
     * already in the chain.
     *
     * @param definition      the nanopublication defining the given part, or null if none is known
     * @param childId         the part whose parent to find
     * @param contextId       the context resource id the chain is shown under
     * @param visited         the ids that may not appear again in the chain
     * @param partDefinitions returns the nanopublication defining a resource as a part of the context, or null if it is none
     * @return the declared parent, or null if there is none
     */
    private static AncestorPart getDeclaredParent(Nanopub definition, String childId, String contextId, Set<String> visited,
                                                  Function<String, Nanopub> partDefinitions) {
        if (definition == null) {
            return null;
        }
        for (String parentId : getDeclaredParentCandidates(definition, childId, contextId)) {
            if (visited.contains(parentId)) {
                continue;
            }
            Nanopub parentDefinition = partDefinitions.apply(parentId);
            if (parentDefinition != null) {
                return new AncestorPart(parentId, getDeclaredLabel(parentDefinition, parentId), parentDefinition);
            }
        }
        return null;
    }

    /**
     * The nanopublication defining the given resource as a part of the given context, as
     * resolved for the part page itself.
     *
     * @param resourceId the resource to resolve
     * @param contextId  the context resource id
     * @param resource   the resolved context resource
     * @return the defining nanopublication, or null if the resource is not a part of the context
     */
    private static Nanopub getPartDefinition(String resourceId, String contextId, AbstractResourceWithProfile resource) {
        ApiResponse response = ApiCache.retrieveResponseSync(ViewDataFetcher.partDefinitionQueryRef(resourceId, contextId, resource), false);
        if (response == null || response.getData().isEmpty()) {
            return null;
        }
        return Utils.getAsNanopub(response.getData().iterator().next().get("np"));
    }

    public ResourcePartPage(final PageParameters parameters) {
        super(parameters);

        final String id = parameters.get("id").toString();
        final String contextId = parameters.get("context").toString();
        final String nanopubId;
        String label = parameters.get("label").isEmpty() ? Utils.getShortNameFromURI(id) : parameters.get("label").toString();
        Set<IRI> classes = new HashSet<>();
        Nanopub definition = null;

        resourceWithProfile = MaintainedResourceRepository.get().findById(contextId);
        if (resourceWithProfile == null) {
            if (SpaceRepository.get().findById(contextId) != null) {
                resourceWithProfile = SpaceRepository.get().findById(contextId);
            } else if (IndividualAgent.isUser(contextId)) {
                resourceWithProfile = IndividualAgent.get(contextId);
            } else {
                throw new IllegalArgumentException("Not a resource, space, or user: " + contextId);
            }
        }
        redirectIfRdfRequested(new RdfSource("part", id, contextId, List.of()));

        QueryRef getDefQuery = ViewDataFetcher.partDefinitionQueryRef(id, contextId, resourceWithProfile);
        ApiResponse getDefResp = ApiCache.retrieveResponseSync(getDefQuery, false);
        if (getDefResp != null && !getDefResp.getData().isEmpty()) {
            nanopubId = getDefResp.getData().iterator().next().get("np");

            Nanopub nanopub = Utils.getAsNanopub(nanopubId);
            definition = nanopub;
            String declaredLabel = getDeclaredLabel(nanopub, id);
            if (declaredLabel != null) {
                label = declaredLabel;
            }
            for (Statement st : nanopub.getAssertion()) {
                if (st.getSubject().stringValue().equals(id) && st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                    classes.add(objIri);
                }
            }
        } else {
            nanopubId = null;
        }
        definitionNanopubId = nanopubId;
//        if (getDefResp == null || getDefResp.getData().isEmpty()) {
//            throw new RestartResponseException(ExplorePage.class, parameters);
//        }

        partLabel = label;

        List<NanodashPageRef> breadCrumb;
        if (resourceWithProfile.getSpace() != null) {
            List<AbstractResourceWithProfile> superSpaces = resourceWithProfile.getSpace().getAllSuperSpacesUntilRoot();
            if (resourceWithProfile instanceof MaintainedResource) {
                superSpaces.add(resourceWithProfile.getSpace());
            }
            superSpaces.add(resourceWithProfile);
            breadCrumb = new ArrayList<>(superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toList());
        } else {
            breadCrumb = new ArrayList<>();
            breadCrumb.add(new NanodashPageRef(UserPage.class, new PageParameters().add("id", contextId), resourceWithProfile.getLabel()));
        }
        breadCrumb.addAll(getAncestorRefs(definition, id, contextId,
                resourceId -> getPartDefinition(resourceId, contextId, resourceWithProfile)));
        breadCrumb.add(new NanodashPageRef(ResourcePartPage.class, new PageParameters().add("id", id).add("context", contextId).add("label", label), label));
        NanodashPageRef[] breadCrumbArray = breadCrumb.toArray(new NanodashPageRef[0]);
        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);
        add(new TitleBar("titlebar", this,
                breadCrumbArray
        ).setTabs(new ResourceTabs("tabs", "part", id, contextId, activeTab)));

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
        add(PageTitleMenu.forResource("titlemenu", resourceWithProfile));
        add(new ExternalLinkWithActionsPanel("id", Model.of(id), Model.of(label), nanopubId == null ? Values.iri(id) : Values.iri(nanopubId)));

        final String nanopubRef = nanopubId == null ? "x:" : nanopubId;
        WebMarkupContainer contentContainer = new WebMarkupContainer("contentContainer");
        add(contentContainer);
        if (activeTab == ResourceTabs.Tab.ABOUT) {
            contentContainer.setVisible(false);
            // The panel constructor resolves view nanopubs over the network when they
            // aren't freshly cached, which would block the initial page render; the
            // view-id list must mirror the panel's View.get calls.
            add(LazyContentPanel.of("otherTab", markupId -> new AboutPartPanel(markupId, resourceWithProfile, id, classes),
                    AboutPartPanel.REQUIRED_VIEWS));
        } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
            contentContainer.setVisible(false);
            // The panel constructor resolves a view nanopub over the network when
            // it isn't freshly cached, which would block the initial page render.
            add(LazyContentPanel.of("otherTab", markupId -> new ExplorePanel(markupId, id),
                    ReferencesPage.REFERENCES_VIEW));
        } else if (activeTab == ResourceTabs.Tab.DOWNLOAD) {
            contentContainer.setVisible(false);
            add(new DownloadLinks("otherTab", "part", id, resourceWithProfile.getId()));
        } else {
            add(new EmptyPanel("otherTab").setVisible(false));
            if (resourceWithProfile.isDataInitialized()) {
                contentContainer.add(RefreshingStructurePanel.of("views", resourceWithProfile,
                        markupId -> new ViewList(markupId, resourceWithProfile, id, nanopubRef, classes)));
            } else {
                contentContainer.add(new LazyContentPanel("views", markupId -> new ViewList(markupId, resourceWithProfile, id, nanopubRef, classes)) {

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.isDataInitialized();
                    }

                    @Override
                    public Component getLoadingComponent(String id) {
                        return new Label(id, ResultComponent.getSectionWaitHtml()).setEscapeModelStrings(false);
                    }

                });
            }
        }
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
