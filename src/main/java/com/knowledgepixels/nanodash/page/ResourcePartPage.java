package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.User;
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
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Resource with profile (Space or MaintainedResource) object with the data shown on this page.
     */
    private AbstractResourceWithProfile resourceWithProfile;

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

    public ResourcePartPage(final PageParameters parameters) {
        super(parameters);

        final String id = parameters.get("id").toString();
        final String contextId = parameters.get("context").toString();
        final String nanopubId;
        String label = parameters.get("label").isEmpty() ? id.replaceFirst("^.*[#/]([^#/]+)$", "$1") : parameters.get("label").toString();
        Set<IRI> classes = new HashSet<>();

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

        QueryRef getDefQuery = new QueryRef(QueryApiAccess.GET_TERM_DEFINITIONS, "term", id);
        if (resourceWithProfile.getSpace() != null) {
            for (IRI userIri : resourceWithProfile.getSpace().getUsers()) {
                for (String pubkey : User.getUserData().getPubkeyHashes(userIri, true)) {
                    getDefQuery.getParams().put("pubkey", pubkey);
                }
            }
        } else {
            for (String pubkey : User.getUserData().getPubkeyHashes(Utils.vf.createIRI(contextId), true)) {
                getDefQuery.getParams().put("pubkey", pubkey);
            }
        }

        ApiResponse getDefResp = ApiCache.retrieveResponseSync(getDefQuery, false);
        if (getDefResp != null && !getDefResp.getData().isEmpty()) {
            nanopubId = getDefResp.getData().iterator().next().get("np");

            Nanopub nanopub = Utils.getAsNanopub(nanopubId);
            for (Statement st : nanopub.getAssertion()) {
                if (!st.getSubject().stringValue().equals(id)) {
                    continue;
                }
                if (st.getPredicate().equals(RDFS.LABEL)) {
                    label = st.getObject().stringValue();
                }
                if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                    classes.add(objIri);
                }
            }
        } else {
            nanopubId = null;
        }
//        if (getDefResp == null || getDefResp.getData().isEmpty()) {
//            throw new RestartResponseException(ExplorePage.class, parameters);
//        }

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
        breadCrumb.add(new NanodashPageRef(ResourcePartPage.class, new PageParameters().add("id", id).add("context", contextId).add("label", label), label));
        NanodashPageRef[] breadCrumbArray = breadCrumb.toArray(new NanodashPageRef[0]);
        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);
        add(new TitleBar("titlebar", this, null,
                breadCrumbArray
        ).setTabs(new ResourceTabs("tabs", "part", id, contextId, activeTab)));

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new Label("titlesuffix", ResourceTabs.titleSuffix(activeTab)));
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
                    AboutPartPanel.PART_INFO_VIEW, AboutResourcePanel.MAINTAINED_RESOURCE_PRESET_ASSIGNMENTS_VIEW, AboutPartPanel.PART_VIEW_DISPLAYS_VIEW));
        } else if (activeTab == ResourceTabs.Tab.EXPLORE) {
            contentContainer.setVisible(false);
            // The panel constructor resolves a view nanopub over the network when
            // it isn't freshly cached, which would block the initial page render.
            add(LazyContentPanel.of("otherTab", markupId -> new ExplorePanel(markupId, id),
                    ReferencesPage.REFERENCES_VIEW));
        } else if (activeTab == ResourceTabs.Tab.RAW) {
            contentContainer.setVisible(false);
            add(new DownloadRdfLinks("otherTab", "part", id, resourceWithProfile.getId()));
        } else {
            add(new EmptyPanel("otherTab").setVisible(false));
            if (resourceWithProfile.isDataInitialized()) {
                contentContainer.add(new ViewList("views", resourceWithProfile, id, nanopubRef, classes));
            } else {
                contentContainer.add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, resourceWithProfile, id, nanopubRef, classes);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.isDataInitialized();
                    }

                    @Override
                    public Component getLoadingComponent(String id) {
                        return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
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
