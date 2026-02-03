package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
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

    /**
     * Resource with profile (Space or MaintainedResource) object with the data shown on this page.
     */
    private ResourceWithProfile resourceWithProfile;

    public ResourcePartPage(final PageParameters parameters) {
        super(parameters);

        final String id = parameters.get("id").toString();
        final String contextId = parameters.get("context").toString();
        final String nanopubId;
        String label = id.replaceFirst("^.*[#/]([^#/]+)$", "$1");
        String description = null;
        Set<IRI> classes = new HashSet<>();

        resourceWithProfile = MaintainedResource.get(contextId);
        if (resourceWithProfile == null) {
            if (Space.get(contextId) != null) {
                resourceWithProfile = Space.get(contextId);
            } else if (User.isUser(contextId)) {
                resourceWithProfile = IndividualAgent.get(contextId);
            } else {
                throw new IllegalArgumentException("Not a resource, space, or user: " + contextId);
            }
        }

        QueryRef getDefQuery = new QueryRef(QueryApiAccess.GET_TERM_DEFINITIONS, "term", id);
        if (resourceWithProfile.getSpace() != null) {
            for (IRI userIri : resourceWithProfile.getSpace().getUsers()) {
                for (String pubkey : User.getUserData().getPubkeyhashes(userIri, true)) {
                    getDefQuery.getParams().put("pubkey", pubkey);
                }
            }
        } else {
            for (String pubkey : User.getUserData().getPubkeyhashes(Utils.vf.createIRI(contextId), true)) {
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
                if (st.getPredicate().equals(SKOS.DEFINITION) || st.getPredicate().equals(DCTERMS.DESCRIPTION) || st.getPredicate().equals(RDFS.COMMENT)) {
                    description = st.getObject().stringValue();
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

        if (description != null) {
            add(new Label("description", description));
        } else {
            add(new Label("description").setVisible(false));
        }

        List<NanodashPageRef> breadCrumb;
        if (resourceWithProfile.getSpace() != null) {
            List<ResourceWithProfile> superSpaces = resourceWithProfile.getSpace().getAllSuperSpacesUntilRoot();
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
        add(new TitleBar("titlebar", this, null,
                breadCrumbArray
        ));

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", label)).setBody(Model.of(id)));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", nanopubId == null ? id : nanopubId)));

        // TODO Improve this code, e.g. make Space a subclass of MaintainedResource or otherwise refactor:
        // we now use the ProfileResource abstraction, but the code still has to be imprved
        if (resourceWithProfile != null) {
            final List<AbstractLink> viewButtons = new ArrayList<>();
            AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                    .set("template", "https://w3id.org/np/RAPxKWDTDP4neVtRckQcTqKHqCC_GHWWPrs7DESb2BJjo")
                    .set("template-version", "latest")
                    .set("param_resource", resourceWithProfile.getId())
                    .set("context", resourceWithProfile.getId())
                    .set("part", id)
            );
            addViewButton.setBody(Model.of("+ view"));
            viewButtons.add(addViewButton);

            final String nanopubRef = nanopubId == null ? "x:" : nanopubId;
            if (resourceWithProfile.isDataInitialized()) {
                add(new ViewList("views", resourceWithProfile, id, nanopubRef, classes));
                add(new ButtonList("view-buttons", resourceWithProfile.getSpace() != null ? resourceWithProfile.getSpace() : resourceWithProfile, null, null, viewButtons));
            } else {
                add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, resourceWithProfile, id, nanopubRef, classes);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.isDataInitialized();
                    }

                });
                add(new AjaxLazyLoadPanel<Component>("view-buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, resourceWithProfile.getSpace() != null ? resourceWithProfile.getSpace() : resourceWithProfile, null, null, viewButtons);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.isDataInitialized();
                    }

                    public Component getLoadingComponent(String id) {
                        return new Label(id).setVisible(false);
                    }

                });
            }
        } else {
            // TODO Ugly code duplication (see above):

            final List<AbstractLink> viewButtons = new ArrayList<>();
            AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                    .set("template", "https://w3id.org/np/RAPxKWDTDP4neVtRckQcTqKHqCC_GHWWPrs7DESb2BJjo")
                    .set("template-version", "latest")
                    .set("param_resource", resourceWithProfile.getSpace().getId())
                    .set("context", resourceWithProfile.getSpace().getId())
                    .set("part", id)
            );
            addViewButton.setBody(Model.of("+ view"));
            viewButtons.add(addViewButton);

            if (resourceWithProfile.getSpace().isDataInitialized()) {
                add(new ViewList("views", resourceWithProfile.getSpace(), id, nanopubId, classes));
                add(new ButtonList("view-buttons", resourceWithProfile.getSpace(), null, null, viewButtons));
            } else {
                add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, resourceWithProfile.getSpace(), id, nanopubId, classes);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.getSpace().isDataInitialized();
                    }

                });
                add(new AjaxLazyLoadPanel<Component>("view-buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, resourceWithProfile.getSpace(), null, null, viewButtons);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return resourceWithProfile.getSpace().isDataInitialized();
                    }

                    public Component getLoadingComponent(String id) {
                        return new Label(id).setVisible(false);
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
